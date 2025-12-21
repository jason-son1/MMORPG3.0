package com.antigravity.rpg.core.engine.trigger;

import lombok.Data;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

@Data
public class TriggerContext {
    private final String triggerType;
    private final Player player;
    private final Entity target; // Optional
    private final org.bukkit.event.Event event; // Optional, null if cast by command/script
    private final Map<String, Object> variables = new HashMap<>();

    public TriggerContext(String triggerType, Player player, Entity target, org.bukkit.event.Event event) {
        this.triggerType = triggerType;
        this.player = player;
        this.target = target;
        this.event = event;
    }

    public TriggerContext(Player player) {
        this("SCRIPT", player, null, null);
    }

    public TriggerContext(Player player, Entity target) {
        this("SCRIPT", player, target, null);
    }
}
