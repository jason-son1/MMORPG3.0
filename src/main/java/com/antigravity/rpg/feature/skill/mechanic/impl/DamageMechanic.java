package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.core.engine.DamageContext;
import com.antigravity.rpg.core.engine.DamageProcessor;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.google.inject.Inject;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 데미지를 입히는 메카닉 구현체입니다.
 */
public class DamageMechanic implements Mechanic {

    private final DamageProcessor damageProcessor;

    @Inject
    public DamageMechanic(DamageProcessor damageProcessor) {
        this.damageProcessor = damageProcessor;
    }

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        // 설정값에서 데미지를 가져옴
        double damage = config.containsKey("amount") ? ((Number) config.get("amount")).doubleValue()
                : (config.containsKey("damage") ? ((Number) config.get("damage")).doubleValue() : 5.0);

        for (Entity target : ctx.getTargets()) {
            if (!(target instanceof LivingEntity))
                continue;

            LivingEntity livingTarget = (LivingEntity) target;

            // RPG 데미지 계산 및 적용
            DamageContext damageCtx = new DamageContext(
                    ctx.getCasterEntity(),
                    livingTarget,
                    damage);

            damageProcessor.process(damageCtx);
            livingTarget.damage(damageCtx.getFinalDamage(), ctx.getCasterEntity());
        }
    }
}
