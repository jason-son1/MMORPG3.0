package com.antigravity.rpg.core.engine;

import com.antigravity.rpg.core.ecs.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityStatData implements Component {
    private final Map<String, Double> statValues = new ConcurrentHashMap<>();

    public void setStat(String id, double value) {
        statValues.put(id, value);
    }

    public double getStat(String id) {
        return statValues.getOrDefault(id, 0.0);
    }

    public double getStat(String id, double fallback) {
        return statValues.getOrDefault(id, fallback);
    }

    public Map<String, Double> getAllStats() {
        return statValues;
    }
}
