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
                        try {
                            // 1. Pre-Conditions 검사 (Caster/Global)
                            // 타겟팅 전이므로 타겟 관련 조건은 무시되거나 캐스터 기준으로 체크됨
                            if (step.getConditionConfigs() != null
                                    && !evaluateConditions(ctx, step.getConditionConfigs(), false)) {
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

                            // 3. Post-Conditions 검사 (Target Filter)
                            if (step.getConditionConfigs() != null && !ctx.getTargets().isEmpty()) {
                                filterTargets(ctx, step.getConditionConfigs());
                                if (ctx.getTargets().isEmpty()) {
                                    // 타겟 필터링 후 타겟이 없으면 스킵? 혹은 타겟 없음으로 진행?
                                    // 보통 타겟이 없으면 이펙트/메카닉이 의미가 없을 수 있음.
                                    // 하지만 이펙트는 시전자 위치에서 나갈 수도 있으므로 진행.
                                }
                            }

                            // 4. Effects 재생
                            playEffects(step.getEffectConfigs(), ctx);

                            // 5. Mechanics 실행
                            if (step.getMechanicConfigs() != null) {
                                for (SkillDefinition.MechanicConfig mCfg : step.getMechanicConfigs()) {
                                    Mechanic mechanic = mechanicFactory.create(mCfg.getType());
                                    if (mechanic != null) {
                                        mechanic.cast(ctx, mCfg.getConfig());
                                    }
                                }
                            }

                            // 6. Delay 등록
                            if (step.getDelay() > 0) {
                                script.setDelayTicks(step.getDelay());
                                script.next();
                                break;
                            }
                        } catch (Exception e) {
                            log.error("Error executing Script FlowStep: {}", e.getMessage(), e);
                        }

                        script.next();
                        continue;
                    }

                    // [Legacy/Direct] MechanicConfig 처리
                    // MechanicConfig 자체가 하나의 Step처럼 동작
                    SkillDefinition.MechanicConfig config = script.getCurrentMechanic();
                    if (config != null) {
                        Map<String, Object> cfg = config.getConfig();

                        // 1. Pre-Conditions (Global)
                        if (cfg.containsKey("conditions")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> conditions = (List<Map<String, Object>>) cfg.get("conditions");
                            if (!evaluateConditions(ctx, conditions, false)) {
                                script.next();
                                continue;
                            }
                        }

                        // (DELAY 메카닉 특수 처리)
                        if ("DELAY".equalsIgnoreCase(config.getType())) {
                            int ticks = ((Number) cfg.getOrDefault("ticks", 0)).intValue();
                            script.setDelayTicks(ticks);
                            script.next();
                            break;
                        }

                        // 2. Targeting
                        if (cfg.containsKey("target")) {
                            Object tObj = cfg.get("target");
                            if (tObj instanceof List && !((List<?>) tObj).isEmpty()) {
                                tObj = ((List<?>) tObj).get(0);
                            }

                            if (tObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> targetCfg = (Map<String, Object>) tObj;
                                Targeter targeter = targeterFactory.create(targetCfg);
                                if (targeter != null) {
                                    ctx.setTargets(targeter.getTargetEntities(ctx));
                                }
                            }
                        }

                        // 3. Post-Condition (Filter)
                        if (cfg.containsKey("conditions")) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> conditions = (List<Map<String, Object>>) cfg.get("conditions");
                            filterTargets(ctx, conditions);
                        }

                        // 4. Mechanic 실행
                        Mechanic mechanic = mechanicFactory.create(config.getType());
                        if (mechanic != null) {
                            mechanic.cast(ctx, cfg);
                        }

                        // (On-Hit 등은 Mechanic 내부에서 처리 or 리턴값으로 처리?)
                        // 현재 구조에서는 Mechanic 내부에서 해결하거나 ScriptRunner.runSubScript 호출

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

    private boolean evaluateConditions(SkillCastContext ctx, List<Map<String, Object>> conditions, boolean perTarget) {
        if (perTarget)
            return true; // 이 메서드는 Global/Caster 체크용

        for (Map<String, Object> condCfg : conditions) {
            String type = (String) condCfg.get("type");
            Condition condition = conditionFactory.create(type, condCfg);
            if (condition != null) {
                // 타겟 없이 시전자 기준으로 조건 평가
                if (!condition.evaluate(ctx, null)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void filterTargets(SkillCastContext ctx, List<Map<String, Object>> conditions) {
        if (ctx.getTargets().isEmpty())
            return;

        ctx.getTargets().removeIf(target -> {
            for (Map<String, Object> condCfg : conditions) {
                String type = (String) condCfg.get("type");
                Condition condition = conditionFactory.create(type, condCfg);
                if (condition != null) {
                    if (!condition.evaluate(ctx, target)) {
                        return true; // 조건 불만족 시 제거
                    }
                }
            }
            return false;
        });
    }

    private void playEffects(List<Map<String, Object>> effectConfigs, SkillCastContext ctx) {
        if (effectConfigs == null)
            return;
        for (Map<String, Object> eCfg : effectConfigs) {
            Effect effect = effectFactory.create(eCfg);
            if (effect != null) {
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

    @Override
    public boolean isAsync() {
        return false;
    }
}
