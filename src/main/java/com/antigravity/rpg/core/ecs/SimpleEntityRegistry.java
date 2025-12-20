package com.antigravity.rpg.core.ecs;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized EntityRegistry using Integer IDs and IdentityHashMap.
 */
@Singleton
public class SimpleEntityRegistry implements EntityRegistry {

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<UUID, Integer> uuidToId = new ConcurrentHashMap<>();
    // Using Integer ID for faster lookups in the component map
    private final Map<Integer, Map<Class<? extends Component>, Component>> entityComponents = new ConcurrentHashMap<>();

    @Override
    public UUID createEntity() {
        UUID uuid = UUID.randomUUID();
        int id = nextId.getAndIncrement();

        uuidToId.put(uuid, id);
        // IdentityHashMap is faster for Class keys (reference equality).
        // Wrapped in synchronizedMap for thread safety.
        entityComponents.put(id, Collections.synchronizedMap(new IdentityHashMap<>()));
        return uuid;
    }

    @Override
    public void removeEntity(UUID entityId) {
        Integer id = uuidToId.remove(entityId);
        if (id != null) {
            entityComponents.remove(id);
        }
    }

    @Override
    public <T extends Component> void addComponent(UUID entityId, T component) {
        Integer id = uuidToId.get(entityId);
        if (id == null)
            return;

        Map<Class<? extends Component>, Component> components = entityComponents.get(id);
        if (components != null) {
            components.put(component.getClass(), component);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getComponent(UUID entityId, Class<T> componentClass) {
        Integer id = uuidToId.get(entityId);
        if (id == null)
            return Optional.empty();

        Map<Class<? extends Component>, Component> components = entityComponents.get(id);
        if (components == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) components.get(componentClass));
    }

    @Override
    public boolean hasComponent(UUID entityId, Class<? extends Component> componentClass) {
        Integer id = uuidToId.get(entityId);
        if (id == null)
            return false;

        Map<Class<? extends Component>, Component> components = entityComponents.get(id);
        return components != null && components.containsKey(componentClass);
    }
}
