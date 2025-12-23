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
 * 정수 ID와 IdentityHashMap을 사용하여 최적화된 EntityRegistry 구현체입니다.
 */
@Singleton
public class SimpleEntityRegistry implements EntityRegistry {

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<UUID, Integer> uuidToId = new ConcurrentHashMap<>();

    // 조회를 빠르게 하기 위해 UUID 대신 내부 정수 ID를 키로 사용합니다.
    private final Map<Integer, Map<Class<? extends Component>, Component>> entityComponents = new ConcurrentHashMap<>();

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

        int id = nextId.getAndIncrement();
        uuidToId.put(uuid, id);
        entityComponents.put(id, Collections.synchronizedMap(new IdentityHashMap<>()));
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

    @Override
    public java.util.List<UUID> getEntitiesWithComponent(Class<? extends Component> componentClass) {
        java.util.List<UUID> result = new java.util.ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : uuidToId.entrySet()) {
            Map<Class<? extends Component>, Component> components = entityComponents.get(entry.getValue());
            if (components != null && components.containsKey(componentClass)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
