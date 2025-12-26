package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.api.skill.Condition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 시전자 또는 대상의 체력 조건을 검사합니다.
 */
public class HealthCondition implements Condition {

    private String targetType;
    private String operation;
    private double value;
    private boolean usePercentage;

    @Override
    public void setup(Map<String, Object> config) {
        this.targetType = (String) config.getOrDefault("target", "CASTER"); // Default to CASTER if not specified? Or
                                                                            // should it be explicitly checked? Original
                                                                            // code defaulted to CASTER in evaluate.
        this.operation = (String) config.getOrDefault("operation", "GT");
        this.value = ((Number) config.getOrDefault("value", 0.0)).doubleValue();
        this.usePercentage = (boolean) config.getOrDefault("percentage", false);
    }

    @Override
    public boolean evaluate(SkillCastContext ctx, Entity target) {
        LivingEntity entity = null;
        if ("CASTER".equalsIgnoreCase(targetType)) {
            if (ctx.getCasterEntity() instanceof LivingEntity)
                entity = (LivingEntity) ctx.getCasterEntity();
        } else {
            if (target instanceof LivingEntity)
                entity = (LivingEntity) target;
        }

        if (entity == null)
            return false;

        double current = entity.getHealth();
        if (usePercentage) {
            double max = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            current = (current / max) * 100.0;
        }

        switch (operation.toUpperCase()) {
            case "GT":
                return current > value;
            case "LT":
                return current < value;
            case "GTE":
                return current >= value;
            case "LTE":
                return current <= value;
            case "EQ":
                return Math.abs(current - value) < 0.01;
            default:
                return true;
        }
    }
}
