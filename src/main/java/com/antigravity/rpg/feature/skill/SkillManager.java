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

/**
 * 스킬 관리자 (Skill Manager)
 * skills.yml 파일을 읽어 스킬 정보를 로드하고 관리합니다.
 */
@Singleton
public class SkillManager implements Service {

    private final AntiGravityPlugin plugin;
    private final TriggerService triggerService;
    private final Map<String, SkillDefinition> skills = new ConcurrentHashMap<>();

    @Inject
    public SkillManager(AntiGravityPlugin plugin, TriggerService triggerService) {
        this.plugin = plugin;
        this.triggerService = triggerService;
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

    public void reload() {
        loadSkills();
    }

    private void loadSkills() {
        skills.clear();
        File file = new File(plugin.getDataFolder(), "skills.yml");
        if (!file.exists()) {
            plugin.saveResource("skills.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("skills");

        if (section == null) {
            plugin.getLogger().warning("skills.yml에 'skills' 섹션이 없습니다.");
            return;
        }

        int count = 0;
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null)
                continue;

            String name = s.getString("name", id);
            long cooldown = s.getLong("cooldown", 0) * 1000; // 초 단위 -> 밀리초
            double mana = s.getDouble("mana_cost", 0);
            double stamina = s.getDouble("stamina_cost", 0);

            SkillDefinition skill = new SkillDefinition(id, name, cooldown, mana, stamina);

            // 트리거 로드 (단순화: on_cast 트리거만 있다고 가정하거나 conditions/actions 바로 로드)
            // 여기서는 YAML 구조가 actions: [설명] 형태라고 가정하고 파싱
            List<String> actions = s.getStringList("actions");
            List<String> conditions = s.getStringList("conditions");

            if (!actions.isEmpty() || !conditions.isEmpty()) {
                Trigger trigger = triggerService.parseTrigger(conditions, actions);
                skill.addTrigger(trigger);
            }

            skills.put(id, skill);
            count++;
        }
        plugin.getLogger().info(count + "개의 스킬을 로드했습니다.");
    }

    public SkillDefinition getSkill(String id) {
        return skills.get(id);
    }
}
