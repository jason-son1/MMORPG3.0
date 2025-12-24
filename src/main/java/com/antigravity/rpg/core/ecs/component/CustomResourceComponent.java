package com.antigravity.rpg.core.ecs.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lua 스크립트 등에서 정의하는 커스텀 자원(예: 콤보 포인트, 영혼 조각 등)을 관리하는 컴포넌트입니다.
 */
public class CustomResourceComponent {
    private final Map<String, Double> resources = new ConcurrentHashMap<>();
    private final Map<String, Double> maxResources = new ConcurrentHashMap<>();

    public double get(String key) {
        return resources.getOrDefault(key, 0.0);
    }

    public void set(String key, double value) {
        double max = maxResources.getOrDefault(key, Double.MAX_VALUE);
        resources.put(key, Math.min(Math.max(0, value), max));
    }

    public void add(String key, double amount) {
        set(key, get(key) + amount);
    }

    public void setMax(String key, double max) {
        maxResources.put(key, max);
    }

    public double getMax(String key) {
        return maxResources.getOrDefault(key, Double.MAX_VALUE);
    }

    public Map<String, Double> getAll() {
        return new ConcurrentHashMap<>(resources);
    }
}
