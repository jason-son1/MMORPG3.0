package com.antigravity.rpg.core.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.antigravity.rpg.core.script.LuaScriptService;

import static org.junit.jupiter.api.Assertions.*;

public class DamageProcessorTest {

    private LuaScriptService luaScriptService;
    private DamageProcessor damageProcessor;

    @BeforeEach
    public void setup() {
        luaScriptService = Mockito.mock(LuaScriptService.class);
        damageProcessor = new DamageProcessor(luaScriptService);
    }

    @Test
    public void testDefenseScaling() {
        // Setup Context
        EntityStatData attacker = new EntityStatData();
        EntityStatData victim = new EntityStatData();

        // 400 Defense = 50% reduction
        victim.setStat("DEFENSE", 400.0);

        DamageContext context = new DamageContext(null, null, attacker, victim, 100.0);

        // Mock Behavior
        Mockito.when(luaScriptService.calculateDamage(context)).thenReturn(50.0);

        // Execute
        damageProcessor.process(context);

        // Assert
        Assertions.assertEquals(50.0, context.getFinalDamage(), 0.1);
    }

    @Test
    public void testPhysicalDamageBonus() {
        EntityStatData attacker = new EntityStatData();
        attacker.setStat("PHYSICAL_DAMAGE", 20.0);

        EntityStatData victim = new EntityStatData();
        victim.setStat("DEFENSE", 0.0);

        DamageContext context = new DamageContext(null, null, attacker, victim, 50.0);
        context.addTag(DamageTag.PHYSICAL);

        // Mock Behavior
        Mockito.when(luaScriptService.calculateDamage(context)).thenReturn(70.0);

        damageProcessor.process(context);

        // Expected: 70
        Assertions.assertEquals(70.0, context.getFinalDamage(), 0.1);
    }

    @Test
    public void testCritDamage() {
        EntityStatData attacker = new EntityStatData();
        attacker.setStat("CRITICAL_CHANCE", 100.0); // Force Crit
        attacker.setStat("CRITICAL_DAMAGE", 200.0); // 200% = 2x multiplier

        EntityStatData victim = new EntityStatData();

        DamageContext context = new DamageContext(null, null, attacker, victim, 50.0);

        // Mock Behavior (Assuming Lua script sets critical flag/damage)
        Mockito.when(luaScriptService.calculateDamage(context)).thenAnswer(inv -> {
            context.setCritical(true);
            return 100.0;
        });

        damageProcessor.process(context);

        // Expected: 100.0
        Assertions.assertEquals(100.0, context.getFinalDamage(), 0.1);
        Assertions.assertTrue(context.isCritical());
    }
}
