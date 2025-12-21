package com.antigravity.rpg.core.engine.action.impl;

import com.antigravity.rpg.core.engine.action.Action;
import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import org.bukkit.entity.LivingEntity;
import java.util.Map;

public class DamageAction implements Action {

    private String formula; // 예: "10 + (STR * 0.5)"

    @Override
    public void execute(TriggerContext context) {
        // 실제 구현에서는 LuaScriptService 등을 통해 formula를 계산해야 함
        // 현재는 고정값 10으로 처리하거나, 간단한 파싱만 수행

        double damage = 10.0;
        try {
            damage = Double.parseDouble(formula);
        } catch (NumberFormatException e) {
            // 수식인 경우 나중에 처리 (Todo)
        }

        if (context.getTarget() instanceof LivingEntity) {
            ((LivingEntity) context.getTarget()).damage(damage, context.getPlayer());
        }
    }

    @Override
    public void load(Map<String, Object> config) {
        Object val = config.get("amount");
        this.formula = val != null ? val.toString() : "0";
    }
}
