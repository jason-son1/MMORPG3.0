package com.antigravity.rpg.data.repository;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;

/**
 * Caffeine 캐시를 사용하는 추상 비동기 저장소 구현체입니다.
 * 데이터 조회 시 캐시를 우선 확인하며, 캐시에 없을 경우에만 데이터베이스에서 로드합니다.
 * 모든 저장 및 삭제 작업은 비동기로 수행됩니다.
 */
public abstract class AbstractCachedRepository<K, V> implements Repository<K, V> {

    private final Executor executor;
    private final AsyncLoadingCache<K, V> cache;

    protected AbstractCachedRepository(Executor executor) {
        this.executor = executor;
        this.cache = Caffeine.newBuilder()
                .maximumSize(1000) // 최대 1000개의 항목 유지
                .expireAfterAccess(30, TimeUnit.MINUTES) // 30분 동안 접근이 없으면 만료
                .executor(executor)
                .buildAsync(this::loadFromDb);
    }

    /**
     * 데이터베이스에서 데이터를 로드하는 로직을 구현해야 합니다.
     */
    protected abstract V loadFromDb(K key) throws Exception;

    /**
     * 데이터베이스에 데이터를 저장하는 로직을 구현해야 합니다.
     */
    protected abstract void saveToDb(K key, V value) throws Exception;

    /**
     * 데이터베이스에서 데이터를 삭제하는 로직을 구현해야 합니다.
     */
    protected abstract void deleteFromDb(K key) throws Exception;

    @Override
    public CompletableFuture<V> find(K key) {
        return cache.get(key);
    }

    @Override
    public CompletableFuture<Void> save(K key, V value) {
        // 캐시 즉시 갱신
        cache.put(key, CompletableFuture.completedFuture(value));
        // 비동기로 데이터베이스 저장 수행
        return CompletableFuture.runAsync(() -> {
            try {
                saveToDb(key, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(K key) {
        // 캐시 즉시 무효화
        cache.synchronous().invalidate(key);
        // 비동기로 데이터베이스 삭제 수행
        return CompletableFuture.runAsync(() -> {
            try {
                deleteFromDb(key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
    }

    /**
     * 캐시에 값을 직접 등록합니다 (DB 로드 생략).
     * 외부 소스(예: JSON 파일)에서 데이터를 로드했을 때 유용합니다.
     */
    public void register(K key, V value) {
        cache.put(key, CompletableFuture.completedFuture(value));
    }

    /**
     * 캐시에서 값을 직접 제거합니다 (DB 삭제 생략).
     * 데이터를 파일로 저장하고 메모리에서 제거할 때 유용합니다.
     */
    public void unregister(K key) {
        cache.synchronous().invalidate(key);
    }
}
