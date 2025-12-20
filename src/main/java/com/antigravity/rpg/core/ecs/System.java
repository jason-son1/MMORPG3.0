package com.antigravity.rpg.core.ecs;

public interface System {
    /**
     * Updates the system logic.
     * 
     * @param deltaTime Time in seconds since the last tick.
     */
    void tick(double deltaTime);

    /**
     * @return True if this system receives updates asynchronously.
     */
    boolean isAsync();
}
