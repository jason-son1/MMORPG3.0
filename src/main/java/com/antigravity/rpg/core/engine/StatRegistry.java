package com.antigravity.rpg.core.engine;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.core.config.ConfigDirectoryLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class StatRegistry {
    private final Map<String, StatDefinition> stats = new ConcurrentHashMap<>();
    private final java.util.List<StatBonus> bonuses = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final AntiGravityPlugin plugin;
    private final ConfigDirectoryLoader configLoader;

    @Inject
    public StatRegistry(AntiGravityPlugin plugin, ConfigDirectoryLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        loadStats();
    }

    public void reload() {
        stats.clear();
        bonuses.clear();
        loadStats();
    }

    private void loadStats() {
        File statsDir = new File(plugin.getDataFolder(), "stats");
        if (!statsDir.exists()) {
            statsDir.mkdirs();
        }

        Map<String, YamlConfiguration> configs = configLoader.loadAll(statsDir);

        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            YamlConfiguration config = entry.getValue();

            // 1. 스탯 정의 로드 (stats 섹션 또는 루트)
            ConfigurationSection statsSection = config.getConfigurationSection("stats");
            if (statsSection != null) {
                for (String key : statsSection.getKeys(false)) {
                    registerStatFromSection(key, statsSection.getConfigurationSection(key));
                }
            } else {
                // Try parsing root keys if they look like stats (fallback/legacy)
                for (String key : config.getKeys(false)) {
                    if (config.isConfigurationSection(key)) {
                        ConfigurationSection section = config.getConfigurationSection(key);
                        if (section.contains("type") || section.contains("min")) {
                            registerStatFromSection(key, section);
                        }
                    }
                }
            }

            // 2. 보너스 정의 로드
            if (config.contains("bonuses")) {
                java.util.List<Map<?, ?>> bonusList = config.getMapList("bonuses");
                for (Map<?, ?> map : bonusList) {
                    try {
                        String source = (String) map.get("source");
                        String target = (String) map.get("target");
                        String formula = (String) map.get("formula");
                        if (source != null && target != null && formula != null) {
                            bonuses.add(new StatBonus(source, target, formula));
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid bonus definition in " + entry.getKey());
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + stats.size() + " stats and " + bonuses.size() + " bonuses.");
    }

    private void registerStatFromSection(String key, ConfigurationSection s) {
        String name = s.getString("displayName", s.getString("name", key));
        String typeStr = s.getString("type", "SIMPLE");
        StatType type;
        try {
            type = StatType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default to SIMPLE if invalid
            type = StatType.SIMPLE;
        }

        double min = s.getDouble("min", 0);
        double max = s.getDouble("max", 1000000);
        double def = s.getDouble("default", 0);
        String formula = s.getString("formula", "");
        String nativeAttr = s.getString("nativeAttribute", "");

        StatDefinition defObj = new StatDefinition(key, name, type, min, max, def, formula, nativeAttr);
        register(defObj);
    }

    public void register(StatDefinition stat) {
        stats.put(stat.getId(), stat);
    }

    public Optional<StatDefinition> getStat(String id) {
        return Optional.ofNullable(stats.get(id));
    }

    public java.util.List<StatBonus> getBonuses() {
        return bonuses;
    }

    public double clamp(String id, double value) {
        StatDefinition def = stats.get(id);
        if (def == null)
            return value;
        return Math.max(def.getMinValue(), Math.min(def.getMaxValue(), value));
    }

    public java.util.Set<String> getStatIds() {
        return stats.keySet();
    }

    public static class StatBonus {
        private final String source;
        private final String target;
        private final String formula;

        public StatBonus(String source, String target, String formula) {
            this.source = source;
            this.target = target;
            this.formula = formula;
        }

        public String getSource() {
            return source;
        }

        public String getTarget() {
            return target;
        }

        public String getFormula() {
            return formula;
        }
    }
}
