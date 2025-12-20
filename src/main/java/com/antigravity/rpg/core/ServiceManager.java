package com.antigravity.rpg.core;

import com.antigravity.rpg.api.service.Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Singleton
public class ServiceManager {

    private final Logger logger;
    private final List<Service> runningServices = new ArrayList<>();

    @Inject
    public ServiceManager(Logger logger) {
        this.logger = logger;
    }

    /**
     * Starts a service and tracks it for shutdown.
     */
    public void startService(Service service) {
        logger.info("[ServiceManager] Starting " + service.getName() + "...");
        try {
            service.onEnable();
            runningServices.add(service);
        } catch (Exception e) {
            logger.severe("[ServiceManager] Failed to start " + service.getName());
            e.printStackTrace();
        }
    }

    /**
     * Shuts down all registered services in reverse order.
     */
    public void shutdownAll() {
        logger.info("[ServiceManager] Shutting down services...");
        // Shutdown in reverse order of startup (LIFO)
        for (int i = runningServices.size() - 1; i >= 0; i--) {
            Service service = runningServices.get(i);
            try {
                logger.info("[ServiceManager] Stopping " + service.getName() + "...");
                service.onDisable();
            } catch (Exception e) {
                logger.severe("[ServiceManager] Error stopping " + service.getName());
                e.printStackTrace();
            }
        }
        runningServices.clear();
    }
}
