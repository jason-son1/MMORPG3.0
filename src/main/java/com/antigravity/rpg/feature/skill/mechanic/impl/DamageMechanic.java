package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.core.engine.action.impl.DamageAction;
import com.antigravity.rpg.core.engine.DamageProcessor;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.google.inject.Inject;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 데미지를 입히는 메카닉 구현체입니다.
 * DamageAction을 래핑하여 전투 로직을 통일합니다.
 */
public class DamageMechanic implements Mechanic {

    private final DamageProcessor damageProcessor;

    @Inject
    public DamageMechanic(DamageProcessor damageProcessor) {
        this.damageProcessor = damageProcessor;
    }

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        // DamageAction 인스턴스 생성 (Prototype 형태)
        DamageAction action = new DamageAction(damageProcessor);

        // 설정값에서 데미지 공식 추출
        Object val = config.get("amount");
        if (val == null)
            val = config.get("damage");
        if (val == null)
            val = config.get("formula"); // 스킬쪽에서는 formula 키도 자주 사용됨

        String formula = val != null ? val.toString() : "0";

        for (Entity target : ctx.getTargets()) {
            if (!(target instanceof LivingEntity))
                continue;

            LivingEntity livingTarget = (LivingEntity) target;

            // Action의 로직 재사용
            action.processDamage(ctx.getCasterEntity(), livingTarget, formula);
        }
    }
}
