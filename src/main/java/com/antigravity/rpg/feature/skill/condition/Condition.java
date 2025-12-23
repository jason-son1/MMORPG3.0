package com.antigravity.rpg.feature.skill.condition;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;

import java.util.Map;

/**
 * 메카닉 실행 전 검사할 조건을 정의하는 인터페이스입니다.
 */
public interface Condition {

    /**
     * 조건을 평가합니다.
     * 
     * @param meta   스킬 실행 컨텍스트
     * @param config 해당 조건의 YAML 설정값
     * @return 조건 충족 여부
     */
    boolean evaluate(SkillMetadata meta, Map<String, Object> config);
}
