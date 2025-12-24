package com.antigravity.rpg.feature.classes.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 직업의 장비 착용 규칙 및 마스터리 보너스를 정의하는 컴포넌트입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentRules {
    private List<String> allowWeapons; // 허용 무기 타입 목록
    private List<String> allowArmors; // 허용 방어구 타입 목록
    private List<MasteryBonus> masteryBonus; // 특정 조건 상의 스탯 보너스

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MasteryBonus {
        private String condition; // 활성화 조건 (예: holds_dual_wield)
        private Map<String, Double> stats; // 적용될 스탯 보너스
    }
}
