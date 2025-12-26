package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.core.engine.StatHolder;
import com.antigravity.rpg.core.formula.ExpressionEngine;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.api.skill.Mechanic;
import com.google.inject.Inject;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.Map;

/**
 * 체력을 회복시키는 메카닉 구현체입니다.
 * HealAction을 래핑하여 로직을 통일합니다.
 */
public class HealMechanic implements Mechanic {

    private final ExpressionEngine expressionEngine;

    @Inject
    public HealMechanic(ExpressionEngine expressionEngine) {
        this.expressionEngine = expressionEngine;
    }

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        // 설정값에서 회복량 공식 추출
        Object amountObj = config.getOrDefault("amount", config.getOrDefault("value", "0"));
        String formula = amountObj.toString();

        // Caster 스탯을 기반으로 공식 계산을 위한 StatHolder 어댑터 생성
        StatHolder casterStats = new StatHolder() {
            @Override
            public double getStat(String statId) {
                return ctx.getCasterStatsSnapshot().getOrDefault(statId, 0.0);
            }

            @Override
            public double getRawStat(String statId) {
                return getStat(statId);
            }

            @Override
            public double getNativeAttributeValue(String attributeName) {
                return 0.0; // 네이티브 속성은 현재 컨텍스트 스냅샷에 없으므로 0 처리 (필요 시 수정)
            }

            @Override
            public String getName() {
                return ctx.getCasterEntity().getName();
            }
        };

        double healAmount = expressionEngine.evaluateFormula(formula, casterStats);

        for (Entity target : ctx.getTargets()) {
            if (!(target instanceof LivingEntity))
                continue;

            LivingEntity livingTarget = (LivingEntity) target;
            double maxHealth = livingTarget.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double currentHealth = livingTarget.getHealth();

            if (currentHealth >= maxHealth)
                continue;

            double newHealth = Math.min(maxHealth, currentHealth + healAmount);

            // 치유 이벤트 발생 (플러그인 호환성)
            EntityRegainHealthEvent event = new EntityRegainHealthEvent(livingTarget, newHealth - currentHealth,
                    EntityRegainHealthEvent.RegainReason.CUSTOM);
            ctx.getCasterEntity().getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                livingTarget.setHealth(Math.min(maxHealth, currentHealth + event.getAmount()));
            }
        }
    }
}
