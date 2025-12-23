package com.antigravity.rpg.data.repository;

import java.util.concurrent.CompletableFuture;

/**
 * 제네릭 비동기 저장소 인터페이스입니다.
 * 
 * @param <K> 키 타입 (예: UUID)
 * @param <V> 값 타입 (예: PlayerData)
 */
public interface Repository<K, V> {

    /**
     * 키에 해당하는 데이터를 조회합니다.
     */
    CompletableFuture<V> find(K key);

    /**
     * 데이터를 저장합니다.
     */
    CompletableFuture<Void> save(K key, V value);

    /**
     * 데이터를 삭제합니다.
     */
    CompletableFuture<Void> delete(K key);
}
