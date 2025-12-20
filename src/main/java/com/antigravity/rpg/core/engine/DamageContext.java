package com.antigravity.rpg.core.engine;

import lombok.Data;
import org.bukkit.entity.LivingEntity;
import java.util.HashSet;
import java.util.Set;

@Data
public class DamageContext {
    private final LivingEntity attacker;
    private final LivingEntity victim;
    private final EntityStatData attackerStats;
    private final EntityStatData victimStats;
    private final Set<String> tags = new HashSet<>();

    // Mutable Calculation Variables
    private double baseDamage;
    private double finalDamage;
    private boolean isCritical;

    public DamageContext(LivingEntity attacker, LivingEntity victim, EntityStatData attackerStats,
            EntityStatData victimStats, double baseDamage) {
        this.attacker = attacker;
        this.victim = victim;
        this.attackerStats = attackerStats;
        this.victimStats = victimStats;
        this.baseDamage = baseDamage;
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void addTag(Enum<?> tag) {
        tags.add(tag.name());
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public boolean hasTag(Enum<?> tag) {
        return tags.contains(tag.name());
    }
}
