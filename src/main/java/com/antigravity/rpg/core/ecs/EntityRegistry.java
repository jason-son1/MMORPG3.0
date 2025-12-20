package com.antigravity.rpg.core.ecs;

import java.util.Optional;
import java.util.UUID;

public interface EntityRegistry {

    /**
     * Creates a new unique Entity ID.
     */
    UUID createEntity();

    /**
     * Removes an entity and all its components.
     */
    void removeEntity(UUID entityId);

    /**
     * Attaches a component to an entity.
     */
    <T extends Component> void addComponent(UUID entityId, T component);

    /**
     * Retrieves a component from an entity.
     */
    <T extends Component> Optional<T> getComponent(UUID entityId, Class<T> componentClass);

    /**
     * Checks if an entity has a specific component.
     */
    boolean hasComponent(UUID entityId, Class<? extends Component> componentClass);
}
