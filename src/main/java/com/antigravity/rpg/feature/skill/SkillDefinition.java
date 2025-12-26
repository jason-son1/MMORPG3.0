package com.antigravity.rpg.feature.skill;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 스킬 정의 객체 (Skill Definition)
 * YAML 파일(skills.yml)로부터 로드되는 스킬의 정적 데이터입니다.
 */
@Getter
@RequiredArgsConstructor
public class SkillDefinition {
    private final String id;
    private final String name;
    private final long cooldownMs;
    private final double manaCost;
    private final double staminaCost;

    // 스킬 발동 시 실행될 메카닉 및 설정 목록
    private final List<MechanicConfig> mechanics = new ArrayList<>();

    // 통합 스킬 실행 흐름 (Flow)
    private final List<com.antigravity.rpg.feature.skill.flow.FlowStep> flow = new ArrayList<>();

    public void addMechanic(MechanicConfig mechanic) {
        this.mechanics.add(mechanic);
    }

    public void addFlowStep(com.antigravity.rpg.feature.skill.flow.FlowStep step) {
        this.flow.add(step);
    }

    /**
     * 메카닉 설정 정보 클래스입니다.
     */
    @Getter
    @RequiredArgsConstructor
    public static class MechanicConfig {
        private final String type;
        private final Map<String, Object> config;
    }
}
