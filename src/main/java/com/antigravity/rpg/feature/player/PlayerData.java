package com.antigravity.rpg.feature.player;

import lombok.Data;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Data
public class PlayerData {
    private final UUID uuid;
    private final ResourcePool resources = new ResourcePool();
    private final Map<String, Integer> skillLevels = new ConcurrentHashMap<>();
    private final Map<String, Double> savedStats = new ConcurrentHashMap<>();

    // Metadata / Temp Flags
    private transient boolean isLoaded = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }
}
