package com.antigravity.rpg.core.ecs.system;

import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.core.ecs.component.ProjectileComponent;
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
