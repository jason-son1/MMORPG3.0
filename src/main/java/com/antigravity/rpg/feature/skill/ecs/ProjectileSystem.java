package com.antigravity.rpg.feature.skill.ecs;

import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.antigravity.rpg.feature.skill.mechanic.MechanicFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ECS 기반 투사체의 이동 및 충돌을 처리하는 시스템입니다.
 */
@Singleton
public class ProjectileSystem implements System {

    private final EntityRegistry entityRegistry;
    private final MechanicFactory mechanicFactory;

    @Inject
    public ProjectileSystem(EntityRegistry entityRegistry, MechanicFactory mechanicFactory) {
        this.entityRegistry = entityRegistry;
        this.mechanicFactory = mechanicFactory;
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
                    for (Map<String, Object> cfg : proj.getOnTickMechanics()) {
                        executeMechanic(proj.getMetadata(), cfg, newLoc);
                    }
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
                            && (!proj.isIgnoreCaster() || !hit.equals(proj.getMetadata().getSourceEntity()))) {
                        triggerHit(entityId, proj, hit, newLoc);
                        return;
                    }
                }
            });
        }
    }

    private void triggerHit(UUID entityId, ProjectileComponent proj, Entity hitEntity, Location hitLoc) {
        if (proj.getOnHitMechanics() != null) {
            SkillMetadata hitMeta = proj.getMetadata().copy();
            hitMeta.setTargetEntity(hitEntity);
            hitMeta.setTargetLocation(hitLoc);

            for (Map<String, Object> cfg : proj.getOnHitMechanics()) {
                Mechanic mechanic = mechanicFactory.create((String) cfg.get("type"));
                if (mechanic != null) {
                    mechanic.cast(hitMeta, cfg);
                }
            }
        }
        entityRegistry.removeEntity(entityId);
    }

    private void executeMechanic(SkillMetadata meta, Map<String, Object> cfg, Location loc) {
        Mechanic mechanic = mechanicFactory.create((String) cfg.get("type"));
        if (mechanic != null) {
            SkillMetadata tickMeta = meta.copy();
            tickMeta.setTargetLocation(loc);
            mechanic.cast(tickMeta, cfg);
        }
    }

    @Override
    public boolean isAsync() {
        return false;
    }
}
