package com.antigravity.rpg.feature.skill.condition.impl;

import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 시전자의 클래스가 특정 클래스인지 확인하는 조건부입니다.
 */
public class ClassCondition implements Condition {

    private String classId;

    @Override
    public void setup(Map<String, Object> config) {
        this.classId = (String) config.get("class");
    }

    @Override
    public boolean evaluate(SkillCastContext ctx, Entity target) {
        String currentClass = ctx.getCasterData().getClassId();
        return classId != null && classId.equalsIgnoreCase(currentClass);
    }
}
