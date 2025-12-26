package com.antigravity.rpg.core.registry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.Set;

/**
 * Generic Registry for creating instances by key.
 * Used for Mechanics, Targeters, Conditions, etc.
 */
public class Registry<T> {
    private final Map<String, Supplier<T>> providers = new ConcurrentHashMap<>();

    public void register(String key, Supplier<T> provider) {
        providers.put(key.toUpperCase(), provider);
    }

    public void register(String key, Class<? extends T> clazz) {
        register(key, () -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate " + clazz.getName(), e);
            }
        });
    }

    public Optional<T> create(String key) {
        Supplier<T> supplier = providers.get(key.toUpperCase());
        return supplier != null ? Optional.ofNullable(supplier.get()) : Optional.empty();
    }

    public Set<String> getKeys() {
        return providers.keySet();
    }
}
