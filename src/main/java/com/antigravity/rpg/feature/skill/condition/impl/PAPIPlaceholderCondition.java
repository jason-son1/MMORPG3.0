package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * PlaceholderAPI를 이용하여 특정 값이 일치하는지 확인하는 조건부입니다.
 */
public class PAPIPlaceholderCondition implements Condition {

    private String placeholder;
    private String value;
    private String operator = "==";

    @Override
    public void setup(Map<String, Object> config) {
        this.placeholder = (String) config.get("placeholder");
        this.value = String.valueOf(config.get("value"));
        this.operator = (String) config.getOrDefault("operator", "==");
    }

    @Override
    public boolean evaluate(SkillCastContext ctx, Entity target) {
        if (!(ctx.getCasterEntity() instanceof Player))
            return false;

        Player player = (Player) ctx.getCasterEntity();
        String result = PlaceholderAPI.setPlaceholders(player, placeholder);

        if ("==".equals(operator)) {
            return result.equalsIgnoreCase(value);
        } else if ("!=".equals(operator)) {
            return !result.equalsIgnoreCase(value);
        }

        // 숫자 비교 지원
        try {
            double dResult = Double.parseDouble(result);
            double dValue = Double.parseDouble(value);

            switch (operator) {
                case ">=":
                    return dResult >= dValue;
                case ">":
                    return dResult > dValue;
                case "<=":
                    return dResult <= dValue;
                case "<":
                    return dResult < dValue;
            }
        } catch (NumberFormatException ignored) {
        }

        return false;
    }
}
