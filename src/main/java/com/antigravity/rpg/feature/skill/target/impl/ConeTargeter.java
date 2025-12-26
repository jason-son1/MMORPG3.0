package com.antigravity.rpg.feature.skill.target.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.api.skill.Targeter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 시전자의 전방 부채꼴(Cone) 영역 내 엔티티를 타겟으로 지정하는 타겟터입니다.
 */
public class ConeTargeter implements Targeter {

    private double radius;
    private double angle; // 시야각 (degrees)

    public ConeTargeter() {
        // Default values
        this.radius = 5.0;
        this.angle = 90.0;
    }

    @Override
    public void setup(Map<String, Object> config) {
        this.radius = ((Number) config.getOrDefault("radius", 5.0)).doubleValue();
        this.angle = ((Number) config.getOrDefault("angle", 90.0)).doubleValue();
    }

    @Override
    public List<Entity> getTargetEntities(SkillCastContext ctx) {
        Entity source = ctx.getCasterEntity();
        Location origin = source.getLocation();
        Vector direction = origin.getDirection().normalize();

        return origin.getWorld().getNearbyEntities(origin, radius, radius, radius).stream()
                .filter(e -> !e.equals(source))
                .filter(e -> {
                    Vector toTarget = e.getLocation().toVector().subtract(origin.toVector()).normalize();
                    double dot = direction.dot(toTarget);
                    double cosAngle = Math.cos(Math.toRadians(angle / 2.0));
                    return dot >= cosAngle;
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
