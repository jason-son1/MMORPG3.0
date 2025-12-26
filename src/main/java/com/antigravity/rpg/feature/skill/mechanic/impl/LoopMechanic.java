package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.SkillDefinition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.api.skill.Mechanic;
import com.antigravity.rpg.feature.skill.runtime.ScriptRunner;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 지정된 횟수만큼 하위 메카닉들을 반복 실행하는 메카닉입니다.
 * ScriptRunner를 통해 반복적인 스케줄을 생성합니다.
 */
public class LoopMechanic implements Mechanic {

    private final ScriptRunner scriptRunner;

    @Inject
    public LoopMechanic(ScriptRunner scriptRunner) {
        this.scriptRunner = scriptRunner;
    }

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        int iterations = ((Number) config.getOrDefault("iterations", 1)).intValue();
        int interval = ((Number) config.getOrDefault("interval", 20)).intValue();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mechanicsMap = (List<Map<String, Object>>) config.get("mechanics");
        if (mechanicsMap == null || mechanicsMap.isEmpty())
            return;

        // 메카닉 설정 리스트 변환
        List<SkillDefinition.MechanicConfig> mechanics = new ArrayList<>();
        for (Map<String, Object> map : mechanicsMap) {
            String type = (String) map.get("type");
            if (type != null) {
                mechanics.add(new SkillDefinition.MechanicConfig(type, map));
            }
        }

        // 반복 실행을 위해 "Mechanic List + Delay" 패턴을 전개 (Macro Expansion)
        // 횟수가 많으면 별도의 LoopSystem이 필요하겠지만, 여기서는 전개 방식으로 구현.
        List<SkillDefinition.MechanicConfig> expandedList = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            // 이번 턴의 메카닉들 추가
            expandedList.addAll(mechanics);

            // 마지막 턴이 아니면 딜레이 추가
            if (i < iterations - 1 && interval > 0) {
                Map<String, Object> delayCfg = new HashMap<>();
                delayCfg.put("ticks", interval);
                expandedList.add(new SkillDefinition.MechanicConfig("DELAY", delayCfg));
            }
        }

        // 생성된 긴 스크립트를 실행
        scriptRunner.runSubScript(expandedList, ctx);
    }
}
