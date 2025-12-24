package com.antigravity.rpg.feature.classes.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 직업의 파티 시너지 효과를 정의하는 컴포넌트입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Synergy {
    private double auraRange = 10.0;
    private List<SynergyEffect> effects;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SynergyEffect {
        private String type; // 효과 타입 (BUFF, DEBUFF)
        private String target; // 대상 (PARTY, ALLY, SELF)
        private String stat; // 변경할 스탯 ID
        private double value; // 변경 수치
    }
}
