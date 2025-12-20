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
 * 스킬 관리자 (Skill Manager)
 * skills.yml 파일을 읽어 스킬 정보를 로드하고 관리합니다.
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

    public void reload() {
        loadSkills();
    }

    private void loadSkills() {
        skills.clear();
        File skillsDir = new File(plugin.getDataFolder(), "skills");
        if (!skillsDir.exists()) {
            skillsDir.mkdirs();
        }

        Map<String, YamlConfiguration> configs = configLoader.loadAll(skillsDir);
        int count = 0;

        for (YamlConfiguration config : configs.values()) {
            for (String id : config.getKeys(false)) {
                ConfigurationSection s = config.getConfigurationSection(id);
                if (s == null)
                    continue;

                String name = s.getString("name", id);
                long cooldown = s.getLong("cooldown", 0) * 1000;
                double mana = s.getDouble("mana_cost", 0);
                double stamina = s.getDouble("stamina_cost", 0);

                SkillDefinition skill = new SkillDefinition(id, name, cooldown, mana, stamina);

                List<String> actions = s.getStringList("actions");
                List<String> conditions = s.getStringList("conditions");

                if (!actions.isEmpty() || !conditions.isEmpty()) {
                    Trigger trigger = triggerService.parseTrigger(conditions, actions);
                    skill.addTrigger(trigger);
                }

                skills.put(id, skill);
                count++;
            }
        }
        plugin.getLogger().info(count + " skills loaded from skills/ directory.");
    }

    public SkillDefinition getSkill(String id) {
        return skills.get(id);
    }
}
