package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.Map;

/**
 * 전방으로 슥 긋는(Slash) 파티클 효과를 생성하는 메카닉입니다.
 */
public class SlashMechanic implements Mechanic {

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        double width = ((Number) config.getOrDefault("width", 2.0)).doubleValue();
        String particleName = (String) config.getOrDefault("particle", "SWEEP_ATTACK");

        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (Exception e) {
            particle = Particle.SWEEP_ATTACK;
        }

        // 타겟이 있으면 모든 타겟 위치에서 Slash, 없으면 시전자 위치 기준 전방 Slash
        if (ctx.getTargets().isEmpty()) {
            renderSlash(ctx.getCasterEntity().getLocation().add(0, 1.0, 0), width, particle);
        } else {
            for (Entity target : ctx.getTargets()) {
                renderSlash(target.getLocation().add(0, 1.0, 0), width, particle);
            }
        }
    }

    private void renderSlash(Location origin, double width, Particle particle) {
        Vector dir = origin.getDirection();
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        for (double d = -width / 2; d <= width / 2; d += 0.2) {
            Location pLoc = origin.clone().add(dir.clone().multiply(1.5)).add(side.clone().multiply(d));
            origin.getWorld().spawnParticle(particle, pLoc, 1, 0, 0, 0, 0);
        }
    }
}
