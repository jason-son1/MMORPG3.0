package com.antigravity.rpg.feature.skill.target.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.api.skill.Targeter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 특정 반경 내의 엔티티를 타겟으로 지정하는 타겟터입니다.
 */
@RequiredArgsConstructor
public class RadiusTargeter implements Targeter {

    private final double radius;
    private final boolean includeCaster;

    public RadiusTargeter(Map<String, Object> config) {
        this.radius = ((Number) config.getOrDefault("radius", 5.0)).doubleValue();
        this.includeCaster = (boolean) config.getOrDefault("include-caster", false);
    }

    @Override
    public List<Entity> getTargetEntities(SkillCastContext ctx) {
        Location origin = ctx.getCasterEntity().getLocation();
        return origin.getWorld().getNearbyEntities(origin, radius, radius, radius).stream()
                .filter(e -> includeCaster || !e.equals(ctx.getCasterEntity()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Location> getTargetLocations(SkillCastContext ctx) {
        // 엔티티들의 현재 위치들을 반환
        return getTargetEntities(ctx).stream()
                .map(Entity::getLocation)
                .collect(Collectors.toList());
    }
}
