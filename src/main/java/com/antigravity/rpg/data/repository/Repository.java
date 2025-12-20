package com.antigravity.rpg.data.repository;

import java.util.concurrent.CompletableFuture;

/**
 * Generic Async Repository interface.
 * 
 * @param <K> Key type (e.g., UUID)
 * @param <V> Value type (e.g., NexusProfile)
 */
public interface Repository<K, V> {

    CompletableFuture<V> find(K key);

    CompletableFuture<Void> save(K key, V value);

    CompletableFuture<Void> delete(K key);
}
