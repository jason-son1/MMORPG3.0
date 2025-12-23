package com.antigravity.rpg.feature.skill.target.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.target.Targeter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.RayTraceResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 선형 레이트레이싱을 통해 첫 번째로 부딪힌 엔티티를 타겟으로 지정하는 타겟터입니다.
 */
public class RayTraceTargeter implements Targeter {

    private final double distance;

    public RayTraceTargeter(Map<String, Object> config) {
        this.distance = ((Number) config.getOrDefault("distance", 10.0)).doubleValue();
    }

    @Override
    public List<Entity> getTargetEntities(SkillCastContext ctx) {
        Entity source = ctx.getCasterEntity();
        RayTraceResult result = source.getWorld().rayTraceEntities(
                source.getLocation().add(0, 1.5, 0), // 눈 높이 근처
                source.getLocation().getDirection(),
                distance,
                e -> !e.equals(source));

        if (result != null && result.getHitEntity() != null) {
            return Collections.singletonList(result.getHitEntity());
        }
        return Collections.emptyList();
    }

    @Override
    public List<Location> getTargetLocations(SkillCastContext ctx) {
        Entity source = ctx.getCasterEntity();
        RayTraceResult result = source.getWorld().rayTrace(
                source.getLocation().add(0, 1.5, 0),
                source.getLocation().getDirection(),
                distance,
                org.bukkit.FluidCollisionMode.NEVER,
                true,
                0.5,
                e -> !e.equals(source));

        if (result != null) {
            return Collections.singletonList(result.getHitPosition().toLocation(source.getWorld()));
        }
        return Collections.emptyList();
    }
}
