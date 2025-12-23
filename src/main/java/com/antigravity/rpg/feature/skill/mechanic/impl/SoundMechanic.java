package com.antigravity.rpg.feature.skill.mechanic.impl;

import com.antigravity.rpg.feature.skill.context.SkillMetadata;
import com.antigravity.rpg.feature.skill.mechanic.Mechanic;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;

import java.util.Map;

/**
 * 소리를 재생하는 메카닉 구현체입니다.
 */
public class SoundMechanic implements Mechanic {

    @Override
    public void cast(SkillMetadata meta, Map<String, Object> config) {
        String soundName = (String) config.getOrDefault("sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        float volume = ((Number) config.getOrDefault("volume", 1.0f)).floatValue();
        float pitch = ((Number) config.getOrDefault("pitch", 1.0f)).floatValue();

        Entity target = meta.getTargetEntity() != null ? meta.getTargetEntity() : meta.getSourceEntity();
        if (target != null) {
            try {
                target.getWorld().playSound(target.getLocation(), Sound.valueOf(soundName.toUpperCase()), volume,
                        pitch);
            } catch (Exception ignored) {
            }
        }
    }
}
