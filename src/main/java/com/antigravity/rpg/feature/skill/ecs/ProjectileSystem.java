package com.antigravity.rpg.feature.skill.ecs;

import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.runtime.ScriptRunner;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ECS 기반 투사체의 이동 및 충돌을 처리하는 시스템입니다.
 * 충돌 발생 시 ScriptRunner에게 처리를 위임합니다.
 */
@Singleton
public class ProjectileSystem implements System {

    private final EntityRegistry entityRegistry;
    private final ScriptRunner scriptRunner;

    @Inject
    public ProjectileSystem(EntityRegistry entityRegistry, ScriptRunner scriptRunner) {
        this.entityRegistry = entityRegistry;
        this.scriptRunner = scriptRunner;
    }

    @Override
    public void tick(double deltaTime) {
        List<UUID> entities = entityRegistry.getEntitiesWithComponent(ProjectileComponent.class);

        for (UUID entityId : entities) {
            entityRegistry.getComponent(entityId, ProjectileComponent.class).ifPresent(proj -> {
                // 1. 수명 다함 체크
                if (proj.getLifeTicksRemaining() <= 0) {
                    entityRegistry.removeEntity(entityId);
                    return;
                }
                proj.setLifeTicksRemaining(proj.getLifeTicksRemaining() - 1);

                // 2. 이동
                Location oldLoc = proj.getCurrentLocation();
                Location newLoc = oldLoc.clone().add(proj.getVelocity());
                proj.setCurrentLocation(newLoc);

                // 3. On-Tick 메카닉 실행
                if (proj.getOnTickMechanics() != null) {
                    // on-tick은 매 틱 실행되므로 즉시 실행 (Delay 처리 등이 필요 없다면 직접 실행이 나을 수 있으나 통일성을 위해
                    // ScriptRunner 위임 고려)
                    // 성능상 매 틱 엔티티 생성은 부담이 될 수 있으나, 요구사항이 "ScriptRunner" 위임이므로 따름.
                    // 다만, 너무 많은 엔티티 생성을 방지하기 위해 여기서는 Context 위치만 업데이트하고 위임.
                    SkillCastContext tickCtx = proj.getContext().copy();
                    tickCtx.setOriginLocation(newLoc);
                    scriptRunner.runSubScriptMap(proj.getOnTickMechanics(), tickCtx);
                }

                // 4. 충돌 체크 (블록 또는 엔티티)
                if (newLoc.getBlock().getType().isSolid()) {
                    triggerHit(entityId, proj, null, newLoc);
                    return;
                }

                Collection<Entity> nearby = newLoc.getWorld().getNearbyEntities(newLoc, proj.getHitboxSize(),
                        proj.getHitboxSize(), proj.getHitboxSize());
                for (Entity hit : nearby) {
                    if (hit instanceof LivingEntity
                            && (!proj.isIgnoreCaster() || !hit.equals(proj.getContext().getCasterEntity()))) {
                        triggerHit(entityId, proj, hit, newLoc);
                        return;
                    }
                }
            });
        }
    }

    private void triggerHit(UUID entityId, ProjectileComponent proj, Entity hitEntity, Location hitLoc) {
        if (proj.getOnHitMechanics() != null) {
            SkillCastContext hitCtx = proj.getContext().copy();
            if (hitEntity != null) {
                hitCtx.setTargets(Collections.singletonList(hitEntity));
            } else {
                hitCtx.getTargets().clear();
            }
            hitCtx.setOriginLocation(hitLoc);

            // ScriptRunner에게 위임
            scriptRunner.runSubScriptMap(proj.getOnHitMechanics(), hitCtx);
        }
        entityRegistry.removeEntity(entityId);
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
