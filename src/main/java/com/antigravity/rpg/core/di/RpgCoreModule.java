package com.antigravity.rpg.core.di;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.core.ecs.SimpleEntityRegistry;
import com.antigravity.rpg.data.service.DatabaseService;
import com.antigravity.rpg.data.sql.HikariDatabaseService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class RpgCoreModule extends AbstractModule {

    private final AntiGravityPlugin plugin;

    public RpgCoreModule(AntiGravityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        // Bind the main plugin instance
        bind(AntiGravityPlugin.class).toInstance(plugin);
        bind(JavaPlugin.class).toInstance(plugin);

        // Data Services
        bind(DatabaseService.class).to(HikariDatabaseService.class).in(Singleton.class);

        // ECS Services
        bind(EntityRegistry.class).to(SimpleEntityRegistry.class).in(Singleton.class);

        // Network Services
        bind(com.antigravity.rpg.core.network.NetworkService.class)
                .to(com.antigravity.rpg.core.network.ProtocolLibNetworkService.class).in(Singleton.class);

        // Config & Formula
        bind(com.antigravity.rpg.core.config.ConfigDirectoryLoader.class).in(Singleton.class);
        bind(com.antigravity.rpg.core.formula.PlaceholderService.class).in(Singleton.class);
        bind(com.antigravity.rpg.core.formula.ExpressionEngine.class).in(Singleton.class);

        // Engine
        bind(com.antigravity.rpg.core.engine.StatRegistry.class).in(Singleton.class);
        bind(com.antigravity.rpg.core.engine.StatCalculator.class).in(Singleton.class);
        bind(com.antigravity.rpg.core.engine.DamageProcessor.class).in(Singleton.class);
        bind(com.antigravity.rpg.core.event.PriorityEventBus.class).in(Singleton.class);

        // Features
        bind(com.antigravity.rpg.core.script.LuaScriptService.class).in(Singleton.class);

        // Core Managers
        bind(com.antigravity.rpg.feature.quest.QuestManager.class).in(Singleton.class);
        bind(com.antigravity.rpg.feature.social.PartyManager.class).in(Singleton.class);
        bind(com.antigravity.rpg.feature.loot.LootManager.class).in(Singleton.class);
        bind(com.antigravity.rpg.feature.item.CustomItemFactory.class).in(Singleton.class);
    }

    /*
     * @Provides
     * 
     * @Singleton
     * Logger provideLogger() {
     * return plugin.getLogger();
     * }
     */
}
