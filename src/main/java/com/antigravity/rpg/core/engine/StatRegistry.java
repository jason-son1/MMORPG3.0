package com.antigravity.rpg.core.engine;

import com.antigravity.rpg.AntiGravityPlugin;
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

    // Standard Stats (Kept for code compatibility, but defined in config)
    public static final String MAX_HEALTH = "MAX_HEALTH";
    public static final String MAX_MANA = "MAX_MANA";
    public static final String HEALTH_REGEN = "HEALTH_REGEN";
    public static final String MANA_REGEN = "MANA_REGEN";
    public static final String PHYSICAL_DAMAGE = "PHYSICAL_DAMAGE";
    public static final String MAGICAL_DAMAGE = "MAGICAL_DAMAGE";
    public static final String DEFENSE = "DEFENSE";
    public static final String CRITICAL_CHANCE = "CRITICAL_CHANCE";
    public static final String CRITICAL_DAMAGE = "CRITICAL_DAMAGE";
    public static final String MOVEMENT_SPEED = "MOVEMENT_SPEED";

    @Inject
    public StatRegistry(AntiGravityPlugin plugin) {
        this.plugin = plugin;
        loadStats();
    }

    public void reload() {
        stats.clear();
        loadStats();
    }

    private void loadStats() {
        File file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) {
            plugin.saveResource("stats.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("stats");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null)
                continue;

            String name = s.getString("name", key);
            String typeStr = s.getString("type", "ATTRIBUTE");
            StatType type = StatType.valueOf(typeStr);
            double min = s.getDouble("min", 0);
            double max = s.getDouble("max", 1000000); // large default
            double def = s.getDouble("default", 0);

            StatDefinition defObj = new StatDefinition(key, name, type, min, max, def);
            register(defObj);

            plugin.getLogger().info("Loaded stat: " + key + " (" + name + ")");
        }
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
