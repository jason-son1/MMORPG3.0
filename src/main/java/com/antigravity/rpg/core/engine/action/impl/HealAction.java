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

    private double amount;

    @Override
    public void execute(TriggerContext context) {
        if (!(context.getTarget() instanceof LivingEntity))
            return;

        LivingEntity target = (LivingEntity) context.getTarget();
        double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double newHealth = Math.min(maxHealth, target.getHealth() + amount);

        target.setHealth(newHealth);
    }

    @Override
    public void load(Map<String, Object> config) {
        if (config.containsKey("amount")) {
            this.amount = ((Number) config.get("amount")).doubleValue();
        }
    }
}
