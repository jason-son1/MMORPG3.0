package com.antigravity.rpg.feature.skill.effect.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.effect.Effect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * XSeries 및 Bukkit Particle API를 이용한 파티클 효과 구현체입니다.
 */
public class XParticleEffect implements Effect {

    private Particle particle;
    private int amount;
    private double speed;
    private double offsetX, offsetY, offsetZ;
    private Color color;

    @Override
    public void setup(Map<String, Object> config) {
        String pName = (String) config.getOrDefault("particle", "REDSTONE");
        try {
            this.particle = Particle.valueOf(pName.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.particle = Particle.HAPPY_VILLAGER;
        }

        this.amount = ((Number) config.getOrDefault("amount", 1)).intValue();
        this.speed = ((Number) config.getOrDefault("speed", 0.1)).doubleValue();
        this.offsetX = ((Number) config.getOrDefault("offset-x", 0.0)).doubleValue();
        this.offsetY = ((Number) config.getOrDefault("offset-y", 0.0)).doubleValue();
        this.offsetZ = ((Number) config.getOrDefault("offset-z", 0.0)).doubleValue();

        if (config.containsKey("color")) {
            String hex = (String) config.get("color");
            if (hex.startsWith("#")) {
                this.color = Color.fromRGB(Integer.parseInt(hex.substring(1), 16));
            }
        }
    }

    @Override
    public void play(Location origin, Entity target, SkillCastContext ctx) {
        Location playLoc = origin;
        if (playLoc == null && target != null) {
            playLoc = target.getLocation().add(0, 1.0, 0);
        }
        if (playLoc == null) {
            playLoc = ctx.getOriginLocation();
        }

        if (particle == Particle.DUST && color != null) {
            Particle.DustOptions options = new Particle.DustOptions(color, 1.0f);
            playLoc.getWorld().spawnParticle(particle, playLoc, amount, offsetX, offsetY, offsetZ, speed, options);
        } else {
            playLoc.getWorld().spawnParticle(particle, playLoc, amount, offsetX, offsetY, offsetZ, speed);
        }
    }
}
