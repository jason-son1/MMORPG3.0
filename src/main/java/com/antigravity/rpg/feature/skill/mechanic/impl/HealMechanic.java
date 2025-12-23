package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.core.engine.action.impl.HealAction;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 체력을 회복시키는 메카닉 구현체입니다.
 * HealAction을 래핑하여 로직을 통일합니다.
 */
public class HealMechanic implements Mechanic {

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        HealAction action = new HealAction();
        String amount = config.getOrDefault("amount", 5.0).toString();

        for (Entity target : ctx.getTargets()) {
            if (!(target instanceof LivingEntity))
                continue;

            LivingEntity livingTarget = (LivingEntity) target;

            // Action 로직 호출
            action.processHeal(livingTarget, amount);
        }
    }
}
