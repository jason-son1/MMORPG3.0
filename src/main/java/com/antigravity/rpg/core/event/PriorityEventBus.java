package com.antigravity.rpg.core.event;

import com.antigravity.rpg.AntiGravityPlugin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

@Singleton
public class PriorityEventBus {

    private final AntiGravityPlugin plugin;

    @Inject
    public PriorityEventBus(AntiGravityPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers a listener with a specific priority, bypassing
     * standard @EventHandler if needed
     * for dynamic registration or fine-tuned control.
     */
    public <T extends Event> void register(Class<T> eventClass, EventPriority priority,
            java.util.function.Consumer<T> handler) {
        plugin.getServer().getPluginManager().registerEvent(
                eventClass,
                new Listener() {
                }, // Dummy listener
                priority,
                (listener, event) -> {
                    if (eventClass.isInstance(event)) {
                        handler.accept(eventClass.cast(event));
                    }
                },
                plugin);
    }
}
