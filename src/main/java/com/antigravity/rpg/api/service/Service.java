package com.antigravity.rpg.api.service;

/**
 * Represents a major system module that needs start/stop handling.
 * This allows the main plugin to manage lifecycles cleanly.
 */
public interface Service {
    /**
     * Called during plugin enablement.
     */
    void onEnable();

    /**
     * Called during plugin disablement.
     */
    void onDisable();

    /**
     * @return Human-readable name of the service.
     */
    String getName();
}
