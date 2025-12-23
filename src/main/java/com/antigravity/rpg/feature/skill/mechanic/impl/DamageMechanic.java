package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.core.engine.DamageContext;
import com.antigravity.rpg.core.engine.DamageProcessor;
import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.google.inject.Inject;
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
    public void cast(SkillMetadata meta, Map<String, Object> config) {
        if (!(meta.getTargetEntity() instanceof LivingEntity))
            return;
        LivingEntity target = (LivingEntity) meta.getTargetEntity();

        // 설정값에서 데미지를 가져옴
        double damage = config.containsKey("amount") ? ((Number) config.get("amount")).doubleValue()
                : (config.containsKey("damage") ? ((Number) config.get("damage")).doubleValue() : 5.0);

        // RPG 데미지 계산 및 적용
        DamageContext context = new DamageContext(
                meta.getSourceEntity(),
                target,
                damage);

        damageProcessor.process(context);
        target.damage(context.getFinalDamage());
    }
}
