package com.antigravity.rpg.api.skill;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;

import java.util.Map;

/**
 * 스킬의 개별 효과(메카닉)를 정의하는 인터페이스입니다.
 */
public interface Mechanic {

    /**
     * 메카닉을 실행합니다.
     * 
     * @param ctx    스킬 실행 컨텍스트
     * @param config 해당 메카닉의 YAML 설정값
     */
    void cast(SkillCastContext ctx, Map<String, Object> config);
}
