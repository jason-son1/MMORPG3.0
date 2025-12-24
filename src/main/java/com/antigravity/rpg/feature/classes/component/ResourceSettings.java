package com.antigravity.rpg.feature.classes.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 직업의 특수 자원(Mana, Rage 등) 설정을 정의하는 컴포넌트입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSettings {

    public enum ResourceType {
        MANA, RAGE, ENERGY, STAMINA, NONE
    }

    public enum RegenMode {
        PASSIVE, // 시간 경과에 따른 자동 회복
        ON_HIT, // 공격 적중 시 회복 (분노 등)
        ON_DAMAGED, // 피격 시 회복
        DECAY // 시간 경과에 따라 감소 (비전투 시)
    }

    private ResourceType type = ResourceType.MANA; // 전용 자원 종류
    private double max = 100.0;
    private RegenMode regenMode = RegenMode.PASSIVE;
    private double regenAmount = 1.0;
    private double decayAmount = 0.0;
}
