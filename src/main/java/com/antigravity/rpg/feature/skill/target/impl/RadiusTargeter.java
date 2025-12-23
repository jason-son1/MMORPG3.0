package com.antigravity.rpg.feature.skill.target.impl;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.target.Targeter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 특정 반경 내의 엔티티를 타겟으로 지정하는 타겟터입니다.
 */
@RequiredArgsConstructor
public class RadiusTargeter implements Targeter {

    private final double radius;
    private final boolean includeCaster;

    @Override
    public List<Entity> getTargetEntities(SkillMetadata meta) {
        Location origin = meta.getSourceEntity().getLocation();
        return origin.getWorld().getNearbyEntities(origin, radius, radius, radius).stream()
                .filter(e -> includeCaster || !e.equals(meta.getSourceEntity()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Location> getTargetLocations(SkillMetadata meta) {
        // 엔티티들의 현재 위치들을 반환
        return getTargetEntities(meta).stream()
                .map(Entity::getLocation)
                .collect(Collectors.toList());
    }
}
