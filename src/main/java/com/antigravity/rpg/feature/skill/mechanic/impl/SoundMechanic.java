package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;

/**
 * 소리를 재생하는 메카닉 구현체입니다.
 */
public class SoundMechanic implements Mechanic {

    @Override
    public void cast(SkillMetadata meta) {
        String soundName = (String) meta.getConfig().getOrDefault("sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        float volume = ((Number) meta.getConfig().getOrDefault("volume", 1.0f)).floatValue();
        float pitch = ((Number) meta.getConfig().getOrDefault("pitch", 1.0f)).floatValue();

        Entity target = meta.getTarget() != null ? meta.getTarget()
                : org.bukkit.Bukkit.getPlayer(meta.getCaster().getUuid());
        if (target != null) {
            try {
                target.getWorld().playSound(target.getLocation(), Sound.valueOf(soundName.toUpperCase()), volume,
                        pitch);
            } catch (Exception ignored) {
            }
        }
    }
}
