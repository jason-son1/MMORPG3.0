package com.antigravity.rpg.feature.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * MMORPG 3.0에서 지원하는 커스텀 장비 슬롯 정의입니다.
 */
@Getter
@RequiredArgsConstructor
public enum EquipmentSlot {
    RING("반지"),
    NECKLACE("목걸이"),
    EARRING("귀걸이"),
    BELT("벨트"),
    CLOAK("망토"),
    ARTEFACT("아티팩트");

    private final String displayName;
}
