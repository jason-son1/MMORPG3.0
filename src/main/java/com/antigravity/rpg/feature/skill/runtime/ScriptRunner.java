package com.antigravity.rpg.feature.skill.runtime;

import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.feature.skill.SkillDefinition;
import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.ecs.ScriptComponent;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.UUID;

/**
 * 스킬 스크립트를 실제 ECS 환경에서 실행 시작하는 진입점입니다.
 */
@Singleton
public class ScriptRunner {

    private final EntityRegistry entityRegistry;

    @Inject
    public ScriptRunner(EntityRegistry entityRegistry) {
        this.entityRegistry = entityRegistry;
    }

    /**
     * 스킬을 실행합니다. (새로운 ECS 엔티티 생성)
     */
    public void run(SkillDefinition skill, SkillMetadata metadata) {
        UUID scriptEntity = entityRegistry.createEntity();

        // ScriptComponent 추가하여 ScriptExecutionSystem이 처리하도록 함
        ScriptComponent component = new ScriptComponent(skill.getMechanics(), metadata);
        entityRegistry.addComponent(scriptEntity, component);
    }
}
