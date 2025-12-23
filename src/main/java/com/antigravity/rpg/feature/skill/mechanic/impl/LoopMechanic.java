package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.antigravity.rpg.feature.skill.mechanic.MechanicFactory;
import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.feature.skill.ecs.ScriptComponent;
import com.antigravity.rpg.feature.skill.SkillDefinition;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 하위 메카닉을 여러 번 반복 실행하는 메카닉입니다.
 * (딜레이가 포함된 경우 ECS 엔티티를 별도로 생성하여 처리합니다)
 */
public class LoopMechanic implements Mechanic {

    private final EntityRegistry entityRegistry;
    private final MechanicFactory mechanicFactory;

    @Inject
    public LoopMechanic(EntityRegistry entityRegistry, MechanicFactory mechanicFactory) {
        this.entityRegistry = entityRegistry;
        this.mechanicFactory = mechanicFactory;
    }

    @Override
    public void cast(SkillMetadata meta, Map<String, Object> config) {
        int amount = ((Number) config.getOrDefault("amount", 1)).intValue();
        int interval = ((Number) config.getOrDefault("interval", 0)).intValue();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mechanics = (List<Map<String, Object>>) config.get("mechanics");
        if (mechanics == null)
            return;

        if (interval <= 0) {
            // 즉시 반복
            for (int i = 0; i < amount; i++) {
                executeInner(meta, mechanics);
            }
        } else {
            // 지연 반복 (ECS 활용)
            for (int i = 0; i < amount; i++) {
                UUID loopEntity = entityRegistry.createEntity();
                List<SkillDefinition.MechanicConfig> loopConfigs = new ArrayList<>();

                // 각 반복 회차마다 딜레이 추가
                if (i > 0) {
                    loopConfigs.add(new SkillDefinition.MechanicConfig("DELAY", Map.of("ticks", i * interval)));
                }

                for (Map<String, Object> m : mechanics) {
                    loopConfigs.add(new SkillDefinition.MechanicConfig((String) m.get("type"), m));
                }

                entityRegistry.addComponent(loopEntity, new ScriptComponent(loopConfigs, meta.copy()));
            }
        }
    }

    private void executeInner(SkillMetadata meta, List<Map<String, Object>> mechanics) {
        for (Map<String, Object> mCfg : mechanics) {
            Mechanic mechanic = mechanicFactory.create((String) mCfg.get("type"));
            if (mechanic != null) {
                mechanic.cast(meta, mCfg);
            }
        }
    }
}
