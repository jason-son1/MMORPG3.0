package com.antigravity.rpg.feature.skill.flow;

import com.antigravity.rpg.feature.skill.SkillDefinition;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 스킬 실행 흐름(Flow)의 개별 단계를 정의하는 클래스입니다.
 * 타겟팅, 이펙트, 메카닉 실행 및 지연 작업을 포함할 수 있습니다.
 */
@Getter
@Builder
public class FlowStep {

    // 타겟터 설정 (새로운 타겟 목록 생성)
    private final Map<String, Object> targeterConfig;

    // 이펙트 설정 목록
    private final List<Map<String, Object>> effectConfigs;

    // 메카닉 설정 목록
    private final List<SkillDefinition.MechanicConfig> mechanicConfigs;

    // 해당 단계의 조건 목록 (불충족 시 이 단계 건너뜀)
    private final List<Map<String, Object>> conditionConfigs;

    // 지연 시간 (Ticks)
    @Builder.Default
    private final int delay = 0;

    // 비동기 실행 여부
    @Builder.Default
    private final boolean async = false;
}
