package com.antigravity.rpg.core.engine.action.impl;

import com.antigravity.rpg.core.engine.action.Action;
import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 소리를 재생하는 액션입니다.
 */
public class SoundAction implements Action {

    private Sound sound;
    private float volume = 1.0f;
    private float pitch = 1.0f;

    @Override
    public void execute(TriggerContext context) {
        if (sound == null)
            return;

        Player player = context.getPlayer();
        if (player != null) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    @Override
    public void load(Map<String, Object> config) {
        String soundName = (String) config.get("sound");
        try {
            this.sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            this.sound = Sound.ENTITY_PLAYER_LEVELUP; // 기본값
        }

        if (config.containsKey("volume")) {
            this.volume = ((Number) config.get("volume")).floatValue();
        }
        if (config.containsKey("pitch")) {
            this.pitch = ((Number) config.get("pitch")).floatValue();
        }
    }
}
