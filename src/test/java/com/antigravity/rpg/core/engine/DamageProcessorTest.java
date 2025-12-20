package com.antigravity.rpg.core.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class DamageProcessorTest {

    private StatRegistry statRegistry;
    private DamageProcessor damageProcessor;

    @BeforeEach
    public void setup() {
        statRegistry = new StatRegistry(); // Loads defaults
        damageProcessor = new DamageProcessor(statRegistry);
    }

    @Test
    public void testDefenseScaling() {
        // Setup Context
        EntityStatData attacker = new EntityStatData();
        EntityStatData victim = new EntityStatData();

        // 400 Defense = 50% reduction
        victim.setStat(StatRegistry.DEFENSE, 400.0);

        DamageContext context = new DamageContext(null, null, attacker, victim, 100.0);

        // Execute
        damageProcessor.process(context);

        // Assert
        // Expected: 100 * (1 - (400 / 800)) = 50.0
        Assertions.assertEquals(50.0, context.getFinalDamage(), 0.1);
    }

    @Test
    public void testPhysicalDamageBonus() {
        EntityStatData attacker = new EntityStatData();
        attacker.setStat(StatRegistry.PHYSICAL_DAMAGE, 20.0);

        EntityStatData victim = new EntityStatData();
        victim.setStat(StatRegistry.DEFENSE, 0.0);

        DamageContext context = new DamageContext(null, null, attacker, victim, 50.0);
        context.addTag(DamageTag.PHYSICAL);

        damageProcessor.process(context);

        // Expected: 50 + 20 = 70
        Assertions.assertEquals(70.0, context.getFinalDamage(), 0.1);
    }

    @Test
    public void testCritDamage() {
        EntityStatData attacker = new EntityStatData();
        attacker.setStat(StatRegistry.CRITICAL_CHANCE, 100.0); // Force Crit
        attacker.setStat(StatRegistry.CRITICAL_DAMAGE, 200.0); // 200% = 2x multiplier

        EntityStatData victim = new EntityStatData();

        DamageContext context = new DamageContext(null, null, attacker, victim, 50.0);

        damageProcessor.process(context);

        // Expected: 50 * 2.0 = 100.0
        Assertions.assertEquals(100.0, context.getFinalDamage(), 0.1);
        Assertions.assertTrue(context.isCritical());
    }
}
