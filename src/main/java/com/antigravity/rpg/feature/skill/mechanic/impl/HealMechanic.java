package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 체력을 회복시키는 메카닉 구현체입니다.
 */
public class HealMechanic implements Mechanic {

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        double amount = ((Number) config.getOrDefault("amount", 5.0)).doubleValue();

        for (Entity target : ctx.getTargets()) {
            if (!(target instanceof LivingEntity))
                continue;

            LivingEntity livingTarget = (LivingEntity) target;
            double maxHealth = livingTarget.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double newHealth = Math.min(maxHealth, livingTarget.getHealth() + amount);

            livingTarget.setHealth(newHealth);
        }
    }
}
