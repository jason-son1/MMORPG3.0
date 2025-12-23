package com.antigravity.rpg.feature.skill;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.trigger.Trigger;
import com.antigravity.rpg.core.engine.trigger.TriggerService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.antigravity.rpg.core.config.ConfigDirectoryLoader;

/**
 * 스킬(Skill) 정보를 로드하고 관리하는 서비스입니다.
 * 'skills' 폴더 내의 모든 설정을 읽어 스킬의 이름, 재사용 대기시간, 소모 자원 등을 정의합니다.
 */
@Singleton
public class SkillManager implements Service {

    private final AntiGravityPlugin plugin;
    private final TriggerService triggerService;
    private final ConfigDirectoryLoader configLoader;
    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();

    @Inject
    public SkillManager(AntiGravityPlugin plugin, TriggerService triggerService, ConfigDirectoryLoader configLoader) {
        this.plugin = plugin;
        this.triggerService = triggerService;
        this.configLoader = configLoader;
    }

    @Override
    public void onEnable() {
        loadSkills();
    }

    @Override
    public void onDisable() {
        skills.clear();
    }

    @Override
    public String getName() {
        return "SkillManager";
    }

    /**
     * 스킬 설정을 다시 로드합니다.
     */
    public void reload() {
        loadSkills();
    }

    /**
     * 'skills' 디렉토리를 탐색하여 모든 스킬 설정을 로드합니다.
     */
    private void loadSkills() {
        skills.clear();
        File skillsDir = new File(plugin.getDataFolder(), "skills");
        if (!skillsDir.exists()) {
            skillsDir.mkdirs();
        }

        // ConfigDirectoryLoader를 통해 디렉토리 내 모든 YAML 파일을 로드합니다.
        Map<String, YamlConfiguration> configs = configLoader.loadAll(skillsDir);
        int count = 0;

        for (YamlConfiguration config : configs.values()) {
            for (String id : config.getKeys(false)) {
                ConfigurationSection s = config.getConfigurationSection(id);
                if (s == null)
                    continue;

                String name = s.getString("name", id);
                long cooldown = s.getLong("cooldown", 0) * 1000; // 초 단위를 밀리초로 변환
                double mana = s.getDouble("mana_cost", 0);
                double stamina = s.getDouble("stamina_cost", 0);

                SkillDefinition skill = new SkillDefinition(id, name, cooldown, mana, stamina);

                // 트리거 및 액션 파싱 (레거시 방식 지원)
                List<String> actions = s.getStringList("actions");
                List<String> conditions = s.getStringList("conditions");

                if (!actions.isEmpty() || !conditions.isEmpty()) {
                    Trigger trigger = triggerService.parseTrigger(conditions, actions);
                    skill.addTrigger(trigger);
                }

                // [NEW] 메카닉(Mechanic) 파싱
                ConfigurationSection mechanicsSection = s.getConfigurationSection("mechanics");
                if (mechanicsSection != null) {
                    for (String key : mechanicsSection.getKeys(false)) {
                        ConfigurationSection m = mechanicsSection.getConfigurationSection(key);
                        if (m != null) {
                            String type = m.getString("type");
                            if (type != null) {
                                Map<String, Object> values = m.getValues(false);
                                skill.addMechanic(new SkillDefinition.MechanicConfig(type, values));
                            }
                        }
                    }
                } else if (s.getList("mechanics") instanceof List) {
                    // 리스트 형태의 메카닉 파싱 지원
                    List<?> list = s.getList("mechanics");
                    if (list != null) {
                        for (Object obj : list) {
                            if (obj instanceof Map) {
                                Map<String, Object> map = (Map<String, Object>) obj;
                                String type = (String) map.get("type");
                                if (type != null) {
                                    skill.addMechanic(new SkillDefinition.MechanicConfig(type, map));
                                }
                            }
                        }
                    }
                }

                skills.put(id, skill);
                count++;
            }
        }
        plugin.getLogger().info("총 " + count + "개의 스킬이 skills/ 디렉토리에서 로드되었습니다.");
    }

    /**
     * 특정 식별자에 해당하는 스킬 정보를 가져옵니다.
     */
    public SkillDefinition getSkill(String id) {
        return skills.get(id);
    }
}
