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
        loadStats();
    }

    private void loadStats() {
        File statsDir = new File(plugin.getDataFolder(), "stats");
        if (!statsDir.exists()) {
            statsDir.mkdirs();
            // 기본 파일 생성 로직은 별도로 분리하거나 초기화 시점에 처리
            plugin.saveResource("stats/base_stats.yml", false);
        }

        Map<String, YamlConfiguration> configs = configLoader.loadAll(statsDir);

        for (Map.Entry<String, YamlConfiguration> entry : configs.entrySet()) {
            YamlConfiguration config = entry.getValue();
            // 파일 내의 최상위 키들을 스탯 ID로 간주하거나, stats 섹션 하위를 볼 수도 있음.
            // 요구사항: "모든 하위 폴더를 재귀적으로 스캔하여 .yml 파일을 자동 로드"
            // 구조가 기존 stats.yml 하나에서 쪼개지므로, 파일 내용 구조에 따라 다름.
            // 보통 stats: 섹션 아래에 두는 관습을 유지하거나, 파일 자체가 스탯 정의일 수 있음.
            // 여기서는 유연하게 설정 파일 내의 모든 루트 키를 검사하되, 'type' 속성이 있는 경우 스탯으로 간주.

            for (String key : config.getKeys(false)) {
                if (config.isConfigurationSection(key)) {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    if (section.contains("type")) {
                        registerStatFromSection(key, section);
                    }
                }
            }
        }

        plugin.getLogger().info("Loaded " + stats.size() + " stats.");
    }

    private void registerStatFromSection(String key, ConfigurationSection s) {
        String name = s.getString("displayName", key);
        String typeStr = s.getString("type", "SIMPLE");
        StatType type;
        try {
            type = StatType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid stat type for " + key + ": " + typeStr);
            return;
        }

        double min = s.getDouble("min", 0);
        double max = s.getDouble("max", 1000000); // large default
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

    public double clamp(String id, double value) {
        StatDefinition def = stats.get(id);
        if (def == null)
            return value;
        return Math.max(def.getMinValue(), Math.min(def.getMaxValue(), value));
    }
}
