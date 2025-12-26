package com.antigravity.rpg.core.formula;

import com.antigravity.rpg.core.engine.StatHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class ExpressionEngineTest {

    private ExpressionEngine engine;
    private StatHolder holder;

    @BeforeEach
    void setUp() {
        com.antigravity.rpg.AntiGravityPlugin plugin = Mockito.mock(com.antigravity.rpg.AntiGravityPlugin.class);
        engine = new ExpressionEngine(plugin);
        holder = Mockito.mock(StatHolder.class);
    }

    @Test
    void testBasicEvaluate() {
        String formula = "10 + 5 * 2";
        double result = engine.evaluateFormula(formula, holder);
        assertEquals(20.0, result);
    }

    @Test
    void testPlaceholderEvaluate() {
        String formula = "10 + {str} * 2";
        when(holder.getStat("str")).thenReturn(5.0);

        double result = engine.evaluateFormula(formula, holder);
        assertEquals(20.0, result);
    }

    @Test
    void testMultiplePlaceholders() {
        String formula = "({str} + {dex}) * {mult}";
        when(holder.getStat("str")).thenReturn(10.0);
        when(holder.getStat("dex")).thenReturn(5.0);
        when(holder.getStat("mult")).thenReturn(2.0);

        double result = engine.evaluateFormula(formula, holder);
        assertEquals(30.0, result);
    }

    @Test
    void testCachingPerformance() {
        String formula = "100 + {val} / 2";
        when(holder.getStat("val")).thenReturn(10.0);

        // First run (compile)
        long start1 = System.nanoTime();
        engine.evaluateFormula(formula, holder);
        long end1 = System.nanoTime();

        // Second run (cache)
        long start2 = System.nanoTime();
        engine.evaluateFormula(formula, holder);
        long end2 = System.nanoTime();

        System.out.println("First run: " + (end1 - start1) + "ns");
        System.out.println("Second run: " + (end2 - start2) + "ns");

        // Cache should be faster (not strictly asserted but good for manual verify)
    }
}
