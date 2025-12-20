package com.antigravity.rpg.core.engine.trigger;

import lombok.Data;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

@Data
public class TriggerContext {
    private final Player player;
    private final Entity target; // Optional
    private final Map<String, Object> variables = new HashMap<>();

    public TriggerContext(Player player, Entity target) {
        this.player = player;
        this.target = target;
    }
}
