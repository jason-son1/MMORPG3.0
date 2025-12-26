package com.antigravity.rpg.api.skill;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 스킬 실행 전 또는 타겟팅 필터링 시 검사할 조건을 정의하는 인터페이스입니다.
 */
public interface Condition {

    /**
     * 설정을 초기화합니다.
     * 
     * @param config YAML 설정 데이터
     */
    void setup(Map<String, Object> config);

    /**
     * 조건을 평가합니다.
     * 
     * @param ctx    스킬 시전 컨텍스트
     * @param target 평가 대상 엔티티 (null일 수 있음)
     * @return 조건 충족 여부
     */
    boolean evaluate(SkillCastContext ctx, Entity target);
}
