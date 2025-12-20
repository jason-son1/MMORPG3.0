package com.antigravity.rpg.feature.classes;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ClassDefinition {
    private final String id;
    private final String displayName;
    private final Map<String, Double> baseAttributes; // 기본 스탯 (1레벨 기준)
    private final Map<String, Double> scaleAttributes; // 레벨업 당 증가 스탯
    private final List<String> skills; // 보유 스킬 목록

    // 트리거 등 추가 확장 가능
    // private final String equipTrigger;
}
