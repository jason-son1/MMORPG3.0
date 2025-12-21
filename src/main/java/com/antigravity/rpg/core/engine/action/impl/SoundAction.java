package com.antigravity.rpg.core.engine.action.impl;

import com.antigravity.rpg.core.engine.action.Action;
import com.antigravity.rpg.core.engine.trigger.TriggerContext;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.Map;

public class SoundAction implements Action {

    private Sound sound;
    private float volume = 1.0f;
    private float pitch = 1.0f;

    @Override
    public void execute(TriggerContext context) {
        if (context.getPlayer() != null && sound != null) {
            context.getPlayer().playSound(context.getPlayer().getLocation(), sound, volume, pitch);
        }
    }

    @Override
    public void load(Map<String, Object> config) {
        String soundName = (String) config.get("sound");
        try {
            this.sound = Sound.valueOf(soundName.toUpperCase());
        } catch (Exception e) {
            System.err.println("[SoundAction] 잘못된 사운드 이름: " + soundName);
        }

        if (config.containsKey("volume")) {
            this.volume = ((Number) config.get("volume")).floatValue();
        }
        if (config.containsKey("pitch")) {
            this.pitch = ((Number) config.get("pitch")).floatValue();
        }
    }
}
