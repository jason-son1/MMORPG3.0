package com.antigravity.rpg.data.repository;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;

public abstract class AbstractCachedRepository<K, V> implements Repository<K, V> {

    private final AsyncLoadingCache<K, V> cache;

    protected AbstractCachedRepository(Executor executor) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .executor(executor)
                .buildAsync(this::loadFromDb);
    }

    protected abstract V loadFromDb(K key) throws Exception;

    protected abstract void saveToDb(K key, V value) throws Exception;

    protected abstract void deleteFromDb(K key) throws Exception;

    @Override
    public CompletableFuture<V> find(K key) {
        return cache.get(key);
    }

    @Override
    public CompletableFuture<Void> save(K key, V value) {
        cache.put(key, CompletableFuture.completedFuture(value));
        return CompletableFuture.runAsync(() -> {
            try {
                saveToDb(key, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompletableFuture<Void> delete(K key) {
        cache.synchronous().invalidate(key);
        return CompletableFuture.runAsync(() -> {
            try {
                deleteFromDb(key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
