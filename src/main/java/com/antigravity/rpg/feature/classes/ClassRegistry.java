package com.antigravity.rpg.feature.classes;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.core.config.ConfigDirectoryLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class ClassRegistry {

    private final Map<String, ClassDefinition> classes = new ConcurrentHashMap<>();
    private final AntiGravityPlugin plugin;
    private final ConfigDirectoryLoader configLoader;

    @Inject
    public ClassRegistry(AntiGravityPlugin plugin, ConfigDirectoryLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        loadClasses();
    }

    public void reload() {
        classes.clear();
        loadClasses();
    }

    private void loadClasses() {
        File classDir = new File(plugin.getDataFolder(), "classes");
        if (!classDir.exists()) {
            classDir.mkdirs();
            // 기본 직업 파일 생성 (필요 시)
        }

        Map<String, YamlConfiguration> configs = configLoader.loadAll(classDir);
        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            YamlConfiguration config = entry.getValue();

            // classes 섹션 하위를 찾을 수도 있고, 루트를 직업 정의로 볼 수도 있음.
            // 여기서는 파일 하나가 직업 하나라고 가정하거나, 루트 키를 직업 ID로 가정.
            // 요구사항: classes/warrior/warrior.yml -> warrior
            // 보통 파일 내에 id 필드를 두는 것이 안전함.

            for (String key : config.getKeys(false)) {
                // 루트 키가 직업 ID인 경우
                if (config.isConfigurationSection(key)) {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    // 직업 정의가 맞는지 확인 (예: base_stats, skills 등이 있는지)
                    if (section.contains("base_stats") || section.contains("skills")) {
                        registerClassFromSection(key, section);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + classes.size() + " classes.");
    }

    private void registerClassFromSection(String id, ConfigurationSection section) {
        String displayName = section.getString("display_name", id);

        Map<String, Double> baseStats = new HashMap<>();
        ConfigurationSection baseSec = section.getConfigurationSection("base_stats");
        if (baseSec != null) {
            for (String stat : baseSec.getKeys(false)) {
                baseStats.put(stat, baseSec.getDouble(stat));
            }
        }

        Map<String, Double> scaleStats = new HashMap<>();
        ConfigurationSection scaleSec = section.getConfigurationSection("scale_stats");
        if (scaleSec != null) {
            for (String stat : scaleSec.getKeys(false)) {
                scaleStats.put(stat, scaleSec.getDouble(stat));
            }
        }

        List<String> skills = section.getStringList("skills");
        if (skills == null)
            skills = new ArrayList<>();

        ClassDefinition classDef = new ClassDefinition(id, displayName, baseStats, scaleStats, skills);
        classes.put(id, classDef);
    }

    public Optional<ClassDefinition> getClass(String id) {
        return Optional.ofNullable(classes.get(id));
    }
}
