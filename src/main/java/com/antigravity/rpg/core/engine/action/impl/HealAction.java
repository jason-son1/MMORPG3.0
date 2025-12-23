package com.antigravity.rpg.core.engine.action.impl;

import com.antigravity.rpg.core.engine.action.Action;
import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/**
 * 대상의 체력을 회복시키는 액션입니다.
 */
public class HealAction implements Action {

    private String amountFormula;

    @Override
    public void execute(TriggerContext context) {
        if (!(context.getTarget() instanceof LivingEntity))
            return;

        processHeal((LivingEntity) context.getTarget(), amountFormula);
    }

    /**
     * 실제 힐 처리 로직을 수행합니다.
     * 
     * @param target  대상
     * @param formula 힐량 공식 또는 값
     */
    public void processHeal(LivingEntity target, String formula) {
        double amount = 0;
        try {
            amount = Double.parseDouble(formula);
        } catch (NumberFormatException e) {
            amount = 5.0; // 기본값
        }

        double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double newHealth = Math.min(maxHealth, target.getHealth() + amount);

        target.setHealth(newHealth);
    }

    @Override
    public void load(Map<String, Object> config) {
        Object val = config.get("amount");
        this.amountFormula = val != null ? val.toString() : "5.0";
    }
}
