package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.api.skill.Condition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 대상이 특정 ECS 컴포넌트를 보유하고 있는지 확인하는 조건부입니다.
 */
public class ComponentCondition implements Condition {

    private String componentClass;

    @Override
    public void setup(Map<String, Object> config) {
        this.componentClass = (String) config.get("component");
    }

    @Override
    public boolean evaluate(SkillCastContext ctx, Entity target) {
        if (target == null)
            return false;

        // PlayerData에서 컴포넌트 확인 (현재 PlayerData가 ECS 컴포넌트 저장소 역할을 함)
        if (ctx.getCasterId().equals(target.getUniqueId())) {
            try {
                Class<?> clazz = Class.forName(componentClass);
                return ctx.getCasterData().getComponent(clazz) != null;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        // 일반 엔티티인 경우 EntityRegistry를 통해 확인해야 함 (추가 구현 필요시 확장)
        return false;
    }
}
