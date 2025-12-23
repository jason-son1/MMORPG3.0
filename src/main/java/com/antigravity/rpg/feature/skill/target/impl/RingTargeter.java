package com.antigravity.rpg.feature.skill.target.impl;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.target.Targeter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 특정 거리 영역(고리 형태) 내의 엔티티를 타겟으로 지정하는 타겟터입니다.
 */
@RequiredArgsConstructor
public class RingTargeter implements Targeter {

    private final double innerRadius;
    private final double outerRadius;

    @Override
    public List<Entity> getTargetEntities(SkillMetadata meta) {
        Location origin = meta.getSourceEntity().getLocation();
        return origin.getWorld().getNearbyEntities(origin, outerRadius, outerRadius, outerRadius).stream()
                .filter(e -> {
                    double dist = e.getLocation().distance(origin);
                    return dist >= innerRadius && dist <= outerRadius;
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
