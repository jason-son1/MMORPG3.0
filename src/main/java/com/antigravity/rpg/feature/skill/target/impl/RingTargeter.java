package com.antigravity.rpg.feature.skill.target.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.target.Targeter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 도넛 형태(Ring)의 범위를 타겟팅하는 타겟터입니다.
 */
public class RingTargeter implements Targeter {

    private final double innerRadius;
    private final double outerRadius;

    public RingTargeter(Map<String, Object> config) {
        this.innerRadius = ((Number) config.getOrDefault("inner-radius", 3.0)).doubleValue();
        this.outerRadius = ((Number) config.getOrDefault("outer-radius", 6.0)).doubleValue();
    }

    @Override
    public List<Entity> getTargetEntities(SkillCastContext ctx) {
        Location center = ctx.getOriginLocation();
        double innerSq = innerRadius * innerRadius;
        double outerSq = outerRadius * outerRadius;

        return center.getWorld().getNearbyEntities(center, outerRadius, outerRadius, outerRadius).stream()
                .filter(e -> {
                    double distSq = e.getLocation().distanceSquared(center);
                    return distSq >= innerSq && distSq <= outerSq;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Location> getTargetLocations(SkillCastContext ctx) {
        return getTargetEntities(ctx).stream()
                .map(Entity::getLocation)
                .collect(Collectors.toList());
    }
}
