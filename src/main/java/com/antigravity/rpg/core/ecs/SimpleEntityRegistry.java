package com.antigravity.rpg.core.ecs;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class SimpleEntityRegistry implements EntityRegistry {

    // Map<EntityID, Map<ComponentClass, ComponentInstance>>
    private final Map<UUID, Map<Class<? extends Component>, Component>> entityComponents = new ConcurrentHashMap<>();

    @Override
    public UUID createEntity() {
        UUID id = UUID.randomUUID();
        entityComponents.put(id, new ConcurrentHashMap<>());
        return id;
    }

    @Override
    public void removeEntity(UUID entityId) {
        entityComponents.remove(entityId);
    }

    @Override
    public <T extends Component> void addComponent(UUID entityId, T component) {
        Map<Class<? extends Component>, Component> components = entityComponents.get(entityId);
        if (components != null) {
            components.put(component.getClass(), component);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getComponent(UUID entityId, Class<T> componentClass) {
        Map<Class<? extends Component>, Component> components = entityComponents.get(entityId);
        if (components == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) components.get(componentClass));
    }

    @Override
    public boolean hasComponent(UUID entityId, Class<? extends Component> componentClass) {
        Map<Class<? extends Component>, Component> components = entityComponents.get(entityId);
        return components != null && components.containsKey(componentClass);
    }
}
