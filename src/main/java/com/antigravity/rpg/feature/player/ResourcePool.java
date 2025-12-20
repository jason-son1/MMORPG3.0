package com.antigravity.rpg.feature.player;

import lombok.Data;

@Data
public class ResourcePool {
    private double currentMana;
    private double maxMana;
    private double currentStamina;
    private double maxStamina;

    // Combat State
    private long lastCombatTick;
    private boolean inCombat;

    public void updateCombatState(long currentTick) {
        // e.g., 10 seconds combat timeout (200 ticks)
        this.inCombat = (currentTick - lastCombatTick) < 200;
    }
}
