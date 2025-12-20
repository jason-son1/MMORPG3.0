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
}
