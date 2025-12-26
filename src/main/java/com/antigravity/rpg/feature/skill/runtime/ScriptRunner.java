package com.antigravity.rpg.feature.skill.runtime;

import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.feature.skill.SkillDefinition;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.core.ecs.component.ScriptComponent;
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
    public void run(SkillDefinition skill, SkillCastContext context) {
        UUID scriptEntity = entityRegistry.createEntity();

        // ScriptComponent 추가하여 ScriptExecutionSystem이 처리하도록 함
        ScriptComponent component = new ScriptComponent(skill.getMechanics(), skill.getFlow(), context);
        entityRegistry.addComponent(scriptEntity, component);
    }

    /**
     * 특정 메카닉 리스트를 즉시 실행하는 하위 스크립트 엔티티를 생성합니다.
     * (예: 투사체 적중 시, 주기적 효과 등)
     * 
     * @param mechanics 실행할 메카닉 설정 목록
     * @param context   실행 컨텍스트 (대상 정보 포함)
     */
    public void runSubScript(java.util.List<SkillDefinition.MechanicConfig> mechanics, SkillCastContext context) {
        if (mechanics == null || mechanics.isEmpty())
            return;

        UUID scriptEntity = entityRegistry.createEntity();

        // 하위 스크립트는 Flow 없이 메카닉 리스트만 실행
        ScriptComponent component = new ScriptComponent(mechanics, null, context);
        entityRegistry.addComponent(scriptEntity, component);
    }

    /**
     * Map 형태의 메카닉 설정을 바로 실행합니다. (Config 리스트 변환 편의 메서드)
     * ProjectileSystem 등에서 호출하여 on-hit 메카닉을 ScriptExecutionSystem으로 위임합니다.
     */
    public void runSubScriptMap(java.util.List<java.util.Map<String, Object>> mechanicsMap, SkillCastContext context) {
        if (mechanicsMap == null || mechanicsMap.isEmpty())
            return;

        java.util.List<SkillDefinition.MechanicConfig> configs = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> map : mechanicsMap) {
            String type = (String) map.get("type");
            if (type != null) {
                configs.add(new SkillDefinition.MechanicConfig(type, map));
            }
        }
        runSubScript(configs, context);
    }
}
