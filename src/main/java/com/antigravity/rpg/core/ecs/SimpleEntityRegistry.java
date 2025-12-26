package com.antigravity.rpg.core.ecs;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * optimized EntityRegistry using int arrays and ComponentTypeRegistry.
 */
@Singleton
public class SimpleEntityRegistry implements EntityRegistry {

    private final AtomicInteger nextEntityId = new AtomicInteger(1);
    private final Map<UUID, Integer> uuidToId = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> idToUuid = new ConcurrentHashMap<>();

    // Entity ID -> Component Type ID -> Component
    // Using simple array of arrays. Resized as needed.
    // Index 0 is unused for entity ID 0.
    private Component[][] componentStore = new Component[1024][];

    // Lock for resizing
    private final Object lock = new Object();

    @Override
    public UUID createEntity() {
        UUID uuid = UUID.randomUUID();
        registerEntity(uuid);
        return uuid;
    }

    @Override
    public void registerEntity(UUID uuid) {
        if (uuidToId.containsKey(uuid))
            return;

        int id = nextEntityId.getAndIncrement();
        uuidToId.put(uuid, id);
        idToUuid.put(id, uuid);

        ensureCapacity(id);
        // Initialize component array for this entity
        // Initial size based on current max component ID, but we can grow it lazily
        synchronized (lock) {
            componentStore[id] = new Component[ComponentTypeRegistry.getMaxId() + 16];
        }
    }

    private void ensureCapacity(int entityId) {
        synchronized (lock) {
            if (entityId >= componentStore.length) {
                int newSize = Math.max(componentStore.length * 2, entityId + 1);
                componentStore = Arrays.copyOf(componentStore, newSize);
            }
        }
    }

    @Override
    public void removeEntity(UUID entityId) {
        Integer id = uuidToId.remove(entityId);
        if (id != null) {
            idToUuid.remove(id);
            synchronized (lock) {
                if (id < componentStore.length) {
                    componentStore[id] = null;
                }
            }
        }
    }

    @Override
    public <T extends Component> void addComponent(UUID entityId, T component) {
        Integer id = uuidToId.get(entityId);
        if (id == null)
            return;

        int typeId = ComponentTypeRegistry.getId(component.getClass());

        synchronized (lock) {
            Component[] components = componentStore[id];
            if (components == null)
                return; // Entity removed

            if (typeId >= components.length) {
                // Grow component array
                int newSize = Math.max(components.length * 2, typeId + 1);
                components = Arrays.copyOf(components, newSize);
                componentStore[id] = components;
            }
            components[typeId] = component;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Component> Optional<T> getComponent(UUID entityId, Class<T> componentClass) {
        Integer id = uuidToId.get(entityId);
        if (id == null)
            return Optional.empty();

        int typeId = ComponentTypeRegistry.getId(componentClass);

        // Optimistic read (array read is atomic for reference, but we might race with
        // resize)
        // Since we only replace the array reference, worst case we read from old array
        // and miss a new component add
        // or get null. For thread safety strictness, we might want to sync,
        // but for performance in game loop, we usually assume single-threaded tick or
        // read-heavy.
        // Let's stick to synchronized for correctness first as requested "Optimization"
        // but safety first.
        // Actually, pure array read:
        Component[] components = componentStore[id];
        if (components == null || typeId >= components.length)
            return Optional.empty();

        return Optional.ofNullable((T) components[typeId]);
    }

    @Override
    public boolean hasComponent(UUID entityId, Class<? extends Component> componentClass) {
        Integer id = uuidToId.get(entityId);
        if (id == null)
            return false;

        int typeId = ComponentTypeRegistry.getId(componentClass);
        Component[] components = componentStore[id];
        return components != null && typeId < components.length && components[typeId] != null;
    }

    @Override
    public List<UUID> getEntitiesWithComponent(Class<? extends Component> componentClass) {
        List<UUID> result = new ArrayList<>();
        int typeId = ComponentTypeRegistry.getId(componentClass);

        // Iterate over all active entities
        // This is generic iteration, might be slow if many entities.
        // But fast for array check.

        // Snapshot of map keys or iterate store
        for (Map.Entry<Integer, UUID> entry : idToUuid.entrySet()) {
            int id = entry.getKey();
            Component[] components = componentStore[id];
            if (components != null && typeId < components.length && components[typeId] != null) {
                result.add(entry.getValue());
            }
        }
        return result;
    }
}
