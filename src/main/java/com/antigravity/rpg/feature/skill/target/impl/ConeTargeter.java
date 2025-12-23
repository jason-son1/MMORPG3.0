package com.antigravity.rpg.feature.skill.target.impl;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.target.Targeter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 시전자의 전방 부채꼴(Cone) 영역 내 엔티티를 타겟으로 지정하는 타겟터입니다.
 */
@RequiredArgsConstructor
public class ConeTargeter implements Targeter {

    private final double radius;
    private final double angle; // 시야각 (degrees)

    @Override
    public List<Entity> getTargetEntities(SkillMetadata meta) {
        Entity source = meta.getSourceEntity();
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
    public List<Location> getTargetLocations(SkillMetadata meta) {
        return getTargetEntities(meta).stream()
                .map(Entity::getLocation)
                .collect(Collectors.toList());
    }
}
