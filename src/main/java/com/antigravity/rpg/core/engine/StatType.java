package com.antigravity.rpg.core.engine;

public enum StatType {
    SIMPLE, // 단순 수치 (예: 힘, 민첩)
    FORMULA, // 공식 기반 계산 (예: 전투력 = 힘 * 2)
    RESOURCE, // 현재/최대값 관리 (예: 체력, 마나)
    NATIVE_ATTRIBUTE, // 마인크래프트 기본 속성 (예: 이동 속도)
    CHANCE // 확률형 스탯 (예: 치명타 확률)
}
