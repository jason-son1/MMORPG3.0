package com.antigravity.rpg.core.engine;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class StatRegistry {
    private final Map<String, StatDefinition> stats = new ConcurrentHashMap<>();

    // Standard Stats
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

    public StatRegistry() {
        registerDefaults();
    }

    private void registerDefaults() {
        register(new StatDefinition(MAX_HEALTH, "Max Health", StatType.ATTRIBUTE, 1, 1000000, 20));
        register(new StatDefinition(MAX_MANA, "Max Mana", StatType.ATTRIBUTE, 0, 1000000, 100));
        register(new StatDefinition(HEALTH_REGEN, "Health Regen", StatType.ATTRIBUTE, 0, 10000, 1));
        register(new StatDefinition(MANA_REGEN, "Mana Regen", StatType.ATTRIBUTE, 0, 10000, 5));

        register(new StatDefinition(PHYSICAL_DAMAGE, "Physical Damage", StatType.ATTRIBUTE, 0, 1000000, 5));
        register(new StatDefinition(MAGICAL_DAMAGE, "Magical Damage", StatType.ATTRIBUTE, 0, 1000000, 0));
        register(new StatDefinition(DEFENSE, "Defense", StatType.ATTRIBUTE, 0, 1000000, 0));

        register(new StatDefinition(CRITICAL_CHANCE, "Crit Chance", StatType.CHANCE, 0, 100, 5));
        register(new StatDefinition(CRITICAL_DAMAGE, "Crit Damage", StatType.MULTIPLIER, 100, 500, 150));

        register(new StatDefinition(MOVEMENT_SPEED, "Speed", StatType.ATTRIBUTE, 0, 0.5, 0.2)); // Clamped 0.5
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
