package com.antigravity.rpg.feature.skill.target.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.target.Targeter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 구체(Sphere) 형태의 범위를 타겟팅하는 타겟터입니다.
 */
public class SphereTargeter implements Targeter {

    private double radius;

    public SphereTargeter(Map<String, Object> config) {
        this.radius = ((Number) config.getOrDefault("radius", 5.0)).doubleValue();
    }

    @Override
    public List<Entity> getTargetEntities(SkillCastContext ctx) {
        Location center = ctx.getOriginLocation();
        return new ArrayList<>(center.getWorld().getNearbyEntities(center, radius, radius, radius));
    }

    @Override
    public List<Location> getTargetLocations(SkillCastContext ctx) {
        List<Location> locs = new ArrayList<>();
        locs.add(ctx.getOriginLocation());
        return locs;
    }
}
