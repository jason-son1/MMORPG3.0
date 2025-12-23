package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.ecs.ProjectileComponent;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import com.google.inject.Inject;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ECS 기반 투사체를 생성하는 메카닉입니다.
 */
public class ProjectileMechanic implements Mechanic {

    private final EntityRegistry entityRegistry;

    @Inject
    public ProjectileMechanic(EntityRegistry entityRegistry) {
        this.entityRegistry = entityRegistry;
    }

    @Override
    public void cast(SkillMetadata meta, Map<String, Object> config) {
        Location startLoc = meta.getSourceEntity().getLocation().add(0, 1.5, 0);
        Vector direction = startLoc.getDirection();

        // [Offset] 시작 위치 보정
        if (config.containsKey("start-offset")) {
            String offsetStr = (String) config.get("start-offset");
            String[] parts = offsetStr.split(",");
            if (parts.length == 3) {
                try {
                    double ox = Double.parseDouble(parts[0].trim());
                    double oy = Double.parseDouble(parts[1].trim());
                    double oz = Double.parseDouble(parts[2].trim());
                    startLoc.add(ox, oy, oz);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        double speed = ((Number) config.getOrDefault("speed", 1.0)).doubleValue();
        Vector velocity = direction.clone().multiply(speed);

        double hitbox = ((Number) config.getOrDefault("hitbox", 0.5)).doubleValue();
        boolean ignoreCaster = (boolean) config.getOrDefault("ignore-caster", true);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> onTick = (List<Map<String, Object>>) config.get("on-tick");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> onHit = (List<Map<String, Object>>) config.get("on-hit");

        // ECS 엔티티 생성 및 컴포넌트 추가
        UUID projId = entityRegistry.createEntity();
        ProjectileComponent component = new ProjectileComponent(
                startLoc, velocity, onTick, onHit, meta.copy(), hitbox, ignoreCaster);
        entityRegistry.addComponent(projId, component);
    }
}
