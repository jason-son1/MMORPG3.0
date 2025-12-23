package com.antigravity.rpg.feature.skill.ecs;

import com.antigravity.rpg.core.ecs.Component;
import com.antigravity.rpg.feature.skill.SkillDefinition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.flow.FlowStep;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 스킬 스크립트의 실행 상태를 저장하는 컴포넌트입니다.
 * 레거시 Mechanic 목록과 새로운 FlowStep 목록을 모두 지원합니다.
 */
@Getter
@Setter
public class ScriptComponent implements Component {

    // 레거시 메카닉 목록
    private final List<SkillDefinition.MechanicConfig> mechanics;
    private int currentIndex = 0;

    // [NEW] 통합 흐름(Flow) 목록
    private final List<FlowStep> flowSteps;
    private int currentFlowIndex = 0;

    // 남은 지연 시간 (Ticks)
    private int delayTicks = 0;

    // 공유 스킬 컨텍스트
    private final SkillCastContext context;

    public ScriptComponent(List<SkillDefinition.MechanicConfig> mechanics, SkillCastContext context) {
        this(mechanics, null, context);
    }

    public ScriptComponent(List<SkillDefinition.MechanicConfig> mechanics, List<FlowStep> flowSteps,
            SkillCastContext context) {
        this.mechanics = mechanics;
        this.flowSteps = flowSteps;
        this.context = context;
    }

    /**
     * 다음 단계로 이동합니다. (Flow가 존재하면 Flow 우선)
     */
    public void next() {
        if (flowSteps != null && !flowSteps.isEmpty()) {
            currentFlowIndex++;
        } else if (mechanics != null) {
            currentIndex++;
        }
    }

    /**
     * 모든 실행이 완료되었는지 확인합니다.
     */
    public boolean isFinished() {
        if (flowSteps != null && !flowSteps.isEmpty()) {
            return currentFlowIndex >= flowSteps.size();
        }
        return mechanics == null || currentIndex >= mechanics.size();
    }

    /**
     * 현재 실행해야 할 레거시 메카닉을 가져옵니다.
     */
    public SkillDefinition.MechanicConfig getCurrentMechanic() {
        if (mechanics == null || currentIndex >= mechanics.size())
            return null;
        return mechanics.get(currentIndex);
    }

    /**
     * 현재 실행해야 할 Flow 단계를 가져옵니다.
     */
    public FlowStep getCurrentFlowStep() {
        if (flowSteps == null || currentFlowIndex >= flowSteps.size())
            return null;
        return flowSteps.get(currentFlowIndex);
    }
}
