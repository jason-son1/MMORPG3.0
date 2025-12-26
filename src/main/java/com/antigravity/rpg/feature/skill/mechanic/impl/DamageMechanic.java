package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.core.engine.DamageTag;
import com.antigravity.rpg.core.formula.ExpressionEngine;
import com.antigravity.rpg.feature.combat.CombatService;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.api.skill.Mechanic;
import com.google.inject.Inject;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 데미지를 입히는 메카닉 구현체입니다.
 * CombatService를 통해 전투 로직을 통일합니다.
 */
public class DamageMechanic implements Mechanic {

    private final CombatService combatService;
    private final ExpressionEngine expressionEngine;

    @Inject
    public DamageMechanic(CombatService combatService, ExpressionEngine expressionEngine) {
        this.combatService = combatService;
        this.expressionEngine = expressionEngine;
    }

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        // 설정값에서 데미지 공식 추출
        Object val = config.get("amount");
        if (val == null)
            val = config.get("damage");
        if (val == null)
            val = config.get("formula"); // 스킬쪽에서는 formula 키도 자주 사용됨

        String formula = val != null ? val.toString() : "0";

        // 수식 계산 (casterData가 StatHolder 지원 시)
        double baseDamage;
        if (ctx.getCasterData() != null) {
            baseDamage = expressionEngine.evaluateFormula(formula, ctx.getCasterData());
        } else {
            // 숫자인 경우 파싱 시도
            try {
                baseDamage = Double.parseDouble(formula);
            } catch (NumberFormatException e) {
                baseDamage = 0;
            }
        }

        // 데미지 타입 결정 (config에서 설정 가능, 기본값: PHYSICAL)
        DamageTag damageType = DamageTag.PHYSICAL;
        Object typeVal = config.get("type");
        if (typeVal != null) {
            try {
                damageType = DamageTag.valueOf(typeVal.toString().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // 기본값 유지
            }
        }

        for (Entity target : ctx.getTargets()) {
            if (!(target instanceof LivingEntity))
                continue;

            LivingEntity livingTarget = (LivingEntity) target;
            LivingEntity caster = (ctx.getCasterEntity() instanceof LivingEntity)
                    ? (LivingEntity) ctx.getCasterEntity()
                    : null;

            if (caster != null) {
                combatService.dealScriptDamage(caster, livingTarget, baseDamage, damageType);
            }
        }
    }
}
