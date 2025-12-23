package com.antigravity.rpg.feature.skill.ecs;

import com.antigravity.rpg.core.ecs.Component;
import com.antigravity.rpg.feature.skill.SkillDefinition;
import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 스킬 스크립트의 실행 상태를 저장하는 컴포넌트입니다.
 * 현재 실행 중인 메카닉 인덱스와 다음 실행까지 남은 틱(Delay)을 관리합니다.
 */
@Getter
@Setter
public class ScriptComponent implements Component {

    // 실행할 메카닉 목록
    private final List<SkillDefinition.MechanicConfig> mechanics;

    // 현재 메카닉 인덱스
    private int currentIndex = 0;

    // 남은 지연 시간 (Ticks)
    private int delayTicks = 0;

    // 공유 스킬 컨텍스트
    private final SkillMetadata metadata;

    public ScriptComponent(List<SkillDefinition.MechanicConfig> mechanics, SkillMetadata metadata) {
        this.mechanics = mechanics;
        this.metadata = metadata;
    }

    /**
     * 다음 메카닉으로 이동합니다.
     */
    public void next() {
        currentIndex++;
    }

    /**
     * 모든 메카닉이 실행되었는지 여부를 반환합니다.
     */
    public boolean isFinished() {
        return currentIndex >= mechanics.size();
    }

    /**
     * 현재 실행해야 할 메카닉 설정을 가져옵니다.
     */
    public SkillDefinition.MechanicConfig getCurrentMechanic() {
        if (isFinished())
            return null;
        return mechanics.get(currentIndex);
    }
}
