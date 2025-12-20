package com.antigravity.rpg;

import com.antigravity.rpg.core.di.RpgCoreModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiGravityPlugin extends JavaPlugin {

    private Injector injector;

    @Override
    public void onEnable() {
        getLogger().info("Initializing AntiGravityRPG Core...");

        // Initialize Dependency Injection
        try {
            this.injector = Guice.createInjector(new RpgCoreModule(this));
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Guice Injector!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("AntiGravityRPG Core enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down AntiGravityRPG...");
        // TODO: Shutdown services via Injector or ServiceManager
    }

    public Injector getInjector() {
        return injector;
    }
}
