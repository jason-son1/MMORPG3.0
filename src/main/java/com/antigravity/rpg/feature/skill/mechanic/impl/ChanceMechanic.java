package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.antigravity.rpg.feature.skill.mechanic.MechanicFactory;
import com.google.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 확률적으로 하위 메카닉을 실행하는 메카닉입니다.
 */
public class ChanceMechanic implements Mechanic {

    private final MechanicFactory mechanicFactory;
    private final Random random = new Random();

    @Inject
    public ChanceMechanic(MechanicFactory mechanicFactory) {
        this.mechanicFactory = mechanicFactory;
    }

    @Override
    public void cast(SkillMetadata meta, Map<String, Object> config) {
        double chance = ((Number) config.getOrDefault("chance", 1.0)).doubleValue();
        if (random.nextDouble() > chance)
            return;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mechanics = (List<Map<String, Object>>) config.get("mechanics");
        if (mechanics == null)
            return;

        for (Map<String, Object> mCfg : mechanics) {
            String type = (String) mCfg.get("type");
            Mechanic mechanic = mechanicFactory.create(type);
            if (mechanic != null) {
                mechanic.cast(meta, mCfg);
            }
        }
    }
}
