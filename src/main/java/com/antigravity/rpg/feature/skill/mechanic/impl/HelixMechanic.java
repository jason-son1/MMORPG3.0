package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Map;

/**
 * 나선형(Helix) 파티클 효과를 생성하는 메카닉입니다.
 */
public class HelixMechanic implements Mechanic {

    @Override
    public void cast(SkillMetadata meta, Map<String, Object> config) {
        Location origin = meta.getTargetLocation();
        if (origin == null)
            origin = meta.getSourceEntity().getLocation();

        double radius = ((Number) config.getOrDefault("radius", 2.0)).doubleValue();
        double height = ((Number) config.getOrDefault("height", 3.0)).doubleValue();
        int particles = ((Number) config.getOrDefault("particles", 50)).intValue();
        String particleName = (String) config.getOrDefault("particle", "FLAME");

        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (Exception e) {
            particle = Particle.FLAME;
        }

        for (int i = 0; i < particles; i++) {
            double angle = 2 * Math.PI * i / (particles / 3.0); // 3바퀴
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = (double) i / particles * height;

            Location pLoc = origin.clone().add(x, y, z);
            origin.getWorld().spawnParticle(particle, pLoc, 1, 0, 0, 0, 0);
        }
    }
}
