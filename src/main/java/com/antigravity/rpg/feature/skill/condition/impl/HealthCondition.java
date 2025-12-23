package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 시전자 또는 대상의 체력 조건을 검사합니다.
 */
public class HealthCondition implements Condition {

    @Override
    public boolean evaluate(SkillMetadata meta, Map<String, Object> config) {
        String targetType = (String) config.getOrDefault("target", "CASTER");
        String operation = (String) config.getOrDefault("operation", "GT"); // GT, LT, GTE, LTE, EQ
        double value = ((Number) config.getOrDefault("value", 0.0)).doubleValue();
        boolean usePercentage = (boolean) config.getOrDefault("percentage", false);

        LivingEntity target = null;
        if ("CASTER".equalsIgnoreCase(targetType)) {
            if (meta.getSourceEntity() instanceof LivingEntity)
                target = (LivingEntity) meta.getSourceEntity();
        } else {
            if (meta.getTargetEntity() instanceof LivingEntity)
                target = (LivingEntity) meta.getTargetEntity();
        }

        if (target == null)
            return false;

        double current = target.getHealth();
        if (usePercentage) {
            double max = target.getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_MAX_HEALTH") != null
                    ? org.bukkit.attribute.Attribute.valueOf("GENERIC_MAX_HEALTH")
                    : org.bukkit.attribute.Attribute.valueOf("MAX_HEALTH")).getValue();
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
