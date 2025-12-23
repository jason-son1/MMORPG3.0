package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillCastContext;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 소리를 재생하는 메카닉 구현체입니다.
 */
public class SoundMechanic implements Mechanic {

    @Override
    public void cast(SkillCastContext ctx, Map<String, Object> config) {
        String soundName = (String) config.getOrDefault("sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        float volume = ((Number) config.getOrDefault("volume", 1.0f)).floatValue();
        float pitch = ((Number) config.getOrDefault("pitch", 1.0f)).floatValue();

        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }

        // 타겟이 있으면 모든 타겟 위치에서 재생, 없으면 시전자 위치에서 재생
        if (ctx.getTargets().isEmpty()) {
            Location loc = ctx.getCasterEntity().getLocation();
            loc.getWorld().playSound(loc, sound, volume, pitch);
        } else {
            for (Entity target : ctx.getTargets()) {
                target.getWorld().playSound(target.getLocation(), sound, volume, pitch);
            }
        }
    }
}
