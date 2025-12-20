package com.antigravity.rpg.core.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ThreadLocalRandom;

@Singleton
public class DamageProcessor {

    private final StatRegistry statRegistry;

    @Inject
    public DamageProcessor(StatRegistry statRegistry) {
        this.statRegistry = statRegistry;
    }

    public void process(DamageContext context) {
        double damage = context.getBaseDamage();
        EntityStatData attackerStats = context.getAttackerStats();
        EntityStatData victimStats = context.getVictimStats();

        // 1. Base Damage Additions (Physical/Magic)
        if (context.hasTag(DamageTag.PHYSICAL)) {
            damage += attackerStats.getStat(StatRegistry.PHYSICAL_DAMAGE);
        } else if (context.hasTag(DamageTag.MAGIC)) {
            damage += attackerStats.getStat(StatRegistry.MAGICAL_DAMAGE);
        }

        // 2. Critical Strike
        double critChance = attackerStats.getStat(StatRegistry.CRITICAL_CHANCE);
        if (ThreadLocalRandom.current().nextDouble() * 100 < critChance) {
            double critDmg = attackerStats.getStat(StatRegistry.CRITICAL_DAMAGE); // e.g. 150.0
            damage *= (critDmg / 100.0);
            context.setCritical(true);
        }

        // 3. Defense Calculation (Logarithmic)
        // Formula: Damage * (1 - (Def / (Def + 400)))
        if (!context.hasTag(DamageTag.IGNORE_DEFENSE)) {
            double defense = victimStats.getStat(StatRegistry.DEFENSE);
            if (defense > 0) {
                double reduction = defense / (defense + 400.0);
                damage *= (1.0 - reduction);
            }
        }

        context.setFinalDamage(Math.max(0, damage));
    }
}
