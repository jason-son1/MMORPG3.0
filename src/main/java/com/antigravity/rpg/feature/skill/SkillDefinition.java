package com.antigravity.rpg.feature.skill;

import com.antigravity.rpg.core.engine.trigger.Trigger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    // 이 스킬이 실행될 때 발동하는 트리거 목록
    private final List<Trigger> triggers = new ArrayList<>();

    public void addTrigger(Trigger trigger) {
        this.triggers.add(trigger);
    }
}
