package com.antigravity.rpg.core.ecs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Assigns unique integer IDs to Component subclasses.
 * This allows using arrays instead of Maps for component lookup.
 */
public class ComponentTypeRegistry {

    private static final Map<Class<? extends Component>, Integer> typeIds = new ConcurrentHashMap<>();
    private static final AtomicInteger nextId = new AtomicInteger(0);

    public static int getId(Class<? extends Component> componentClass) {
        return typeIds.computeIfAbsent(componentClass, k -> nextId.getAndIncrement());
    }

    public static int getMaxId() {
        return nextId.get();
    }
}
