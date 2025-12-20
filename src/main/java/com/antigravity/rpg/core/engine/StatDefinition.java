package com.antigravity.rpg.core.engine;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatDefinition {
    private final String id;
    private final String displayName;
    private final StatType type;
    private final double minValue;
    private final double maxValue;
    private final double defaultValue;
    private final String formula; // FORMULA 타입일 때 사용
    private final String nativeAttribute; // NATIVE_ATTRIBUTE 타입일 때 사용 (예: generic.movement_speed)
}
