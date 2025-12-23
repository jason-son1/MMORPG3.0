package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.antigravity.rpg.feature.skill.runtime.ScriptRunner;
import com.google.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 확률적으로 하위 메카닉을 실행하는 메카닉입니다.
 */
public class ChanceMechanic implements Mechanic {

    private final ScriptRunner scriptRunner;

    @Inject
    public ChanceMechanic(ScriptRunner scriptRunner) {
        this.scriptRunner = scriptRunner;
    }

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        double chance = ((Number) config.getOrDefault("chance", 0.5)).doubleValue();

        if (ThreadLocalRandom.current().nextDouble() < chance) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mechanics = (List<Map<String, Object>>) config.get("mechanics");

            if (mechanics != null) {
                scriptRunner.runSubScriptMap(mechanics, ctx);
            }
        }
    }
}
