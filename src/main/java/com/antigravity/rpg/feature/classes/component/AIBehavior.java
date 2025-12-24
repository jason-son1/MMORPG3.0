package com.antigravity.rpg.feature.classes.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 직업의 AI 행동 패턴을 정의하는 컴포넌트입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIBehavior {
    private TargetPriority targetPriority = TargetPriority.CLOSEST;
    private double combatDistance = 3.0;
    private List<SkillRotation> skillRotation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillRotation {
        private String skill;
        private String condition; // 시전 조건 (Lua 식 또는 ConditionManager 조건)
    }

    public enum TargetPriority {
        LOWEST_HP, CLOSEST, HIGHEST_THREAT, RANDOM
    }
}
