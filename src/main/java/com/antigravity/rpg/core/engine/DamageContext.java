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
    private final Set<DamageTag> tags = new HashSet<>();

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

    public void addTag(DamageTag tag) {
        tags.add(tag);
    }

    public boolean hasTag(DamageTag tag) {
        return tags.contains(tag);
    }
}
