package com.antigravity.rpg.feature.skill.ecs;

import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.feature.skill.SkillDefinition;
import com.antigravity.rpg.feature.skill.condition.Condition;
import com.antigravity.rpg.feature.skill.condition.ConditionFactory;
import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.antigravity.rpg.feature.skill.mechanic.MechanicFactory;
import com.antigravity.rpg.feature.skill.target.Targeter;
import com.antigravity.rpg.feature.skill.target.TargeterFactory;
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

    @Inject
    public ScriptExecutionSystem(EntityRegistry entityRegistry,
            MechanicFactory mechanicFactory,
            TargeterFactory targeterFactory,
            ConditionFactory conditionFactory) {
        this.entityRegistry = entityRegistry;
        this.mechanicFactory = mechanicFactory;
        this.targeterFactory = targeterFactory;
        this.conditionFactory = conditionFactory;
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

                // 2. 메카닉 단계별 실행
                while (!script.isFinished() && script.getDelayTicks() <= 0) {
                    SkillDefinition.MechanicConfig config = script.getCurrentMechanic();
                    Map<String, Object> cfg = config.getConfig();

                    // [Condition] 조건 검사
                    if (cfg.containsKey("conditions")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> conditions = (List<Map<String, Object>>) cfg.get("conditions");
                        if (!evaluateConditions(script.getMetadata(), conditions)) {
                            script.next();
                            continue;
                        }
                    }

                    // [Timeline] Delay 특수 처리
                    if ("DELAY".equalsIgnoreCase(config.getType())) {
                        int ticks = ((Number) cfg.getOrDefault("ticks", 0)).intValue();
                        script.setDelayTicks(ticks);
                        script.next();
                        break;
                    }

                    // [Targeting] 타겟팅 수행
                    Targeter targeter = null;
                    if (cfg.containsKey("target")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> targetCfg = (Map<String, Object>) cfg.get("target");
                        targeter = targeterFactory.create(targetCfg);
                    }

                    // [Mechanic] 실행
                    Mechanic mechanic = mechanicFactory.create(config.getType());
                    if (mechanic != null) {
                        if (targeter != null) {
                            List<Entity> targets = targeter.getTargetEntities(script.getMetadata());
                            for (Entity target : targets) {
                                // 타겟별로 독립된 컨텍스트(사본) 생성
                                SkillMetadata targetMeta = script.getMetadata().copy();
                                targetMeta.setTargetEntity(target);
                                targetMeta.setTargetLocation(target.getLocation());
                                mechanic.cast(targetMeta, cfg);
                            }

                            // 엔티티가 없는 경우 위치 기반 타겟팅 시도
                            if (targets.isEmpty()) {
                                List<Location> locs = targeter.getTargetLocations(script.getMetadata());
                                for (Location loc : locs) {
                                    SkillMetadata locMeta = script.getMetadata().copy();
                                    locMeta.setTargetLocation(loc);
                                    mechanic.cast(locMeta, cfg);
                                }
                            }
                        } else {
                            // 타겟터가 없으면 현재 컨텍스트 그대로 실행
                            mechanic.cast(script.getMetadata(), cfg);
                        }
                    }

                    script.next();
                }

                // 3. 종료 확인
                if (script.isFinished() && script.getDelayTicks() <= 0) {
                    entityRegistry.removeEntity(entityId);
                }
            });
        }
    }

    private boolean evaluateConditions(SkillMetadata meta, List<Map<String, Object>> conditions) {
        for (Map<String, Object> condCfg : conditions) {
            String type = (String) condCfg.get("type");
            Condition condition = conditionFactory.create(type);
            if (condition != null) {
                if (!condition.evaluate(meta, condCfg)) {
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
