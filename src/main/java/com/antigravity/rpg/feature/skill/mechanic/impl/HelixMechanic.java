package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 타겟 주변에 나선형 파티클 효과를 생성하는 메카닉입니다.
 */
public class HelixMechanic implements Mechanic {

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        double radius = ((Number) config.getOrDefault("radius", 2.0)).doubleValue();
        double height = ((Number) config.getOrDefault("height", 3.0)).doubleValue();
        int particles = ((Number) config.getOrDefault("particles", 50)).intValue();

        for (Entity target : ctx.getTargets()) {
            Location loc = target.getLocation();
            for (int i = 0; i < particles; i++) {
                double ratio = (double) i / particles;
                double angle = ratio * Math.PI * 4; // 2바퀴
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = ratio * height;

                loc.add(x, y, z);
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 1, 0, 0, 0, 0);
                loc.subtract(x, y, z);
            }
        }
    }
}
