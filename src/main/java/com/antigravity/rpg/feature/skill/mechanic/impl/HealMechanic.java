package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 체력을 회복시키는 메카닉 구현체입니다.
 */
public class HealMechanic implements Mechanic {

    @Override
    public void cast(SkillMetadata meta, Map<String, Object> config) {
        if (!(meta.getTargetEntity() instanceof LivingEntity))
            return;
        LivingEntity target = (LivingEntity) meta.getTargetEntity();

        double amount = ((Number) config.getOrDefault("amount", 5.0)).doubleValue();

        double currentHealth = target.getHealth();
        double maxHealth = target.getAttribute(org.bukkit.attribute.Attribute.valueOf("GENERIC_MAX_HEALTH") != null
                ? org.bukkit.attribute.Attribute.valueOf("GENERIC_MAX_HEALTH")
                : org.bukkit.attribute.Attribute.valueOf("MAX_HEALTH")).getValue();

        target.setHealth(Math.min(maxHealth, currentHealth + amount));
    }
}
