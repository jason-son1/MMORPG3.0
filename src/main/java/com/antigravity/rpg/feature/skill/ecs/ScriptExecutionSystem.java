package com.antigravity.rpg.feature.skill.ecs;

import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.feature.skill.SkillDefinition;
import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.condition.ConditionFactory;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.antigravity.rpg.feature.skill.mechanic.MechanicFactory;
import com.antigravity.rpg.feature.skill.target.Targeter;
import com.antigravity.rpg.feature.skill.target.TargeterFactory;
import com.antigravity.rpg.feature.skill.effect.Effect;
import com.antigravity.rpg.feature.skill.effect.EffectFactory;
import com.antigravity.rpg.feature.skill.flow.FlowStep;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 스킬 스크립트 실행을 담당하는 ECS 시스템입니다.
 * Targeting, Condition, Timeline(Delay) 단계를 포함한 전체 파이프라인을 처리합니다.
 */
@Singleton
@Slf4j
public class ScriptExecutionSystem implements System {

    private final EntityRegistry entityRegistry;
    private final MechanicFactory mechanicFactory;
    private final TargeterFactory targeterFactory;
    private final ConditionFactory conditionFactory;
    private final EffectFactory effectFactory;

    @Inject
    public ScriptExecutionSystem(EntityRegistry entityRegistry,
            MechanicFactory mechanicFactory,
            TargeterFactory targeterFactory,
            ConditionFactory conditionFactory,
            EffectFactory effectFactory) {
        this.entityRegistry = entityRegistry;
        this.mechanicFactory = mechanicFactory;
        this.targeterFactory = targeterFactory;
        this.conditionFactory = conditionFactory;
        this.effectFactory = effectFactory;
    }

    @Override
    public void tick(double deltaTime) {
        List<UUID> entities = entityRegistry.getEntitiesWithComponent(ScriptComponent.class);

        for (UUID entityId : entities) {
            entityRegistry.getComponent(entityId, ScriptComponent.class).ifPresent(script -> {
                // 1. 지연 시간 처리
                if (script.getDelayTicks() > 0) {
                    script.setDelayTicks(script.getDelayTicks() - 1);
                    return;
                }

                // 2. 실행 루프
                while (!script.isFinished() && script.getDelayTicks() <= 0) {
                    SkillCastContext ctx = script.getContext();

                    // [NEW] FlowStep 처리
                    FlowStep step = script.getCurrentFlowStep();
                    if (step != null) {
                        // 1. Condition 검사
                        if (step.getConditionConfigs() != null
                                && !evaluateConditions(ctx, step.getConditionConfigs())) {
                            script.next();
                            continue;
                        }

                        // 2. Targeting 업데이트
                        if (step.getTargeterConfig() != null) {
                            Targeter targeter = targeterFactory.create(step.getTargeterConfig());
                            if (targeter != null) {
                                ctx.setTargets(targeter.getTargetEntities(ctx));
                            }
                        }

                        // 3. Effects 재생
                        if (step.getEffectConfigs() != null) {
                            for (Map<String, Object> eCfg : step.getEffectConfigs()) {
                                Effect effect = effectFactory.create(eCfg);
                                if (effect != null) {
                                    // 타겟이 있으면 모든 타겟에 대해 재생, 없으면 시전자 위치
                                    if (ctx.getTargets().isEmpty()) {
                                        effect.play(ctx.getOriginLocation(), null, ctx);
                                    } else {
                                        for (Entity target : ctx.getTargets()) {
                                            effect.play(null, target, ctx);
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Mechanics 실행
                        if (step.getMechanicConfigs() != null) {
                            for (SkillDefinition.MechanicConfig mCfg : step.getMechanicConfigs()) {
                                Mechanic mechanic = mechanicFactory.create(mCfg.getType());
                                if (mechanic != null) {
                                    mechanic.cast(ctx, mCfg.getConfig());
                                }
                            }
                        }

                        // 5. Delay 등록
                        if (step.getDelay() > 0) {
                            script.setDelayTicks(step.getDelay());
                            script.next();
                            break;
                        }

                        script.next();
                        continue;
                    }

                    // [Legacy] MechanicConfig 처리
                    SkillDefinition.MechanicConfig config = script.getCurrentMechanic();
                    if (config != null) {
                        Map<String, Object> cfg = config.getConfig();
                        // ... (기존 레거시 로직 동일하게 유지)
                        if (cfg.containsKey("conditions")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> conditions = (List<Map<String, Object>>) cfg.get("conditions");
                            if (!evaluateConditions(ctx, conditions)) {
                                script.next();
                                continue;
                            }
                        }

                        if ("DELAY".equalsIgnoreCase(config.getType())) {
                            int ticks = ((Number) cfg.getOrDefault("ticks", 0)).intValue();
                            script.setDelayTicks(ticks);
                            script.next();
                            break;
                        }

                        if (cfg.containsKey("target")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> targetCfg = (Map<String, Object>) cfg.get("target");
                            Targeter targeter = targeterFactory.create(targetCfg);
                            if (targeter != null) {
                                ctx.setTargets(targeter.getTargetEntities(ctx));
                            }
                        }

                        Mechanic mechanic = mechanicFactory.create(config.getType());
                        if (mechanic != null) {
                            mechanic.cast(ctx, cfg);
                        }

                        script.next();
                    }
                }

                // 3. 종료 확인
                if (script.isFinished() && script.getDelayTicks() <= 0) {
                    entityRegistry.removeEntity(entityId);
                }
            });
        }
    }

    private boolean evaluateConditions(SkillCastContext ctx, List<Map<String, Object>> conditions) {
        for (Map<String, Object> condCfg : conditions) {
            String type = (String) condCfg.get("type");
            Condition condition = conditionFactory.create(type, condCfg);
            if (condition != null) {
                // 타겟 없이 시전자 기준으로 조건 평가 시 target은 null 전달
                if (!condition.evaluate(ctx, null)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
