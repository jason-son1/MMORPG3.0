package com.antigravity.rpg.feature.classes.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 직업 전직(Promotion)에 필요한 조건을 정의하는 컴포넌트입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequirements {
    private List<String> requirements; // 전직 조건 문자열 목록 (예: "class level 30")
}
