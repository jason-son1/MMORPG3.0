package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 시전자의 스탯 조건을 확인하는 조건부입니다.
 */
public class StatCondition implements Condition {

    private String stat;
    private double value;
    private String operator = ">=";

    @Override
    public void setup(Map<String, Object> config) {
        this.stat = (String) config.get("stat");
        this.value = ((Number) config.getOrDefault("value", 0)).doubleValue();
        this.operator = (String) config.getOrDefault("operator", ">=");
    }

    @Override
    public boolean evaluate(SkillCastContext ctx, Entity target) {
        double current = ctx.getCasterStatsSnapshot().getOrDefault(stat, 0.0);

        switch (operator) {
            case ">=":
                return current >= value;
            case ">":
                return current > value;
            case "<=":
                return current <= value;
            case "<":
                return current < value;
            case "==":
                return current == value;
            default:
                return false;
        }
    }
}
