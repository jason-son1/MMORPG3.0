package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.core.engine.DamageContext;
import com.antigravity.rpg.core.engine.DamageProcessor;
import com.antigravity.rpg.core.engine.EntityStatData;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.google.inject.Inject;
import org.bukkit.entity.LivingEntity;

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
    public void cast(SkillMetadata meta) {
        if (!(meta.getTarget() instanceof LivingEntity))
            return;
        LivingEntity target = (LivingEntity) meta.getTarget();

        // 설정값에서 데미지(또는 공식)를 가져옴
        double damage = meta.getConfig().containsKey("damage") ? ((Number) meta.getConfig().get("damage")).doubleValue()
                : 5.0;

        // RPG 데미지 계산 및 적용
        DamageContext context = new DamageContext(
                meta.getCaster().getName().equals(meta.getTarget().getName()) ? null
                        : meta.getCaster().getUuid() != null ? org.bukkit.Bukkit.getPlayer(meta.getCaster().getUuid())
                                : null,
                target,
                damage);

        damageProcessor.process(context);
        target.damage(context.getFinalDamage());
    }
}
