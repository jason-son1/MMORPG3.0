package com.antigravity.rpg.feature.classes.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 직업별 경험치 획득 규칙을 정의하는 컴포넌트입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceSources {
    private Map<String, SourceSettings> sources;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceSettings {
        private String amount; // 수식 (예: "10 + (mob_level * 2)")
        private List<String> conditions; // 획득 조건 목록
        private List<String> blocks; // MINE_BLOCK 시 대상 블록 목록
    }
}
