package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.entity.LivingEntity;

/**
 * 체력을 회복시키는 메카닉 구현체입니다.
 */
public class HealMechanic implements Mechanic {

    @Override
    public void cast(SkillMetadata meta) {
        if (!(meta.getTarget() instanceof LivingEntity))
            return;
        LivingEntity target = (LivingEntity) meta.getTarget();

        double amount = ((Number) meta.getConfig().getOrDefault("amount", 5.0)).doubleValue();

        double currentHealth = target.getHealth();
        double maxHealth = target.getAttribute(org.bukkit.attribute.Attribute.valueOf("MAX_HEALTH")).getValue();

        target.setHealth(Math.min(maxHealth, currentHealth + amount));
    }
}
