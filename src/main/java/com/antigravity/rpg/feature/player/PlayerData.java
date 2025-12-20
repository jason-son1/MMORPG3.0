package com.antigravity.rpg.feature.player;

import lombok.Getter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PlayerData {
    @Getter
    private final UUID uuid;

    // Core data storage
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    // Helper objects (wrappers around the data map or transient)
    private final ResourcePool resources;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        // Initialize default values
        this.data.put("level", 1);
        this.data.put("experience", 0.0);
        this.data.put("skillLevels", new ConcurrentHashMap<String, Integer>());
        this.data.put("professions", new ConcurrentHashMap<String, Integer>());
        this.data.put("skillCooldowns", new ConcurrentHashMap<String, Number>()); // changed to Number to handle
                                                                                  // Long/Integer issues

        this.data.put("savedStats", new ConcurrentHashMap<String, Double>());

        this.resources = new ResourcePool();
    }

    // Generic Accessors
    public void set(String key, Object value) {
        data.put(key, value);
    }

    public <T> T get(String key, Class<T> type) {
        Object val = data.get(key);
        if (val != null && type.isAssignableFrom(val.getClass())) {
            return type.cast(val);
        }
        return null; // Or Optional
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object val = data.get(key);
        if (val == null)
            return defaultValue;
        try {
            return (T) val;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    // Type-Safe Wrappers (Backward Compatibility)
    public String getClassId() {
        return (String) data.get("classId");
    }

    public void setClassId(String classId) {
        data.put("classId", classId != null ? classId : "");
    }

    public int getLevel() {
        Number n = (Number) data.getOrDefault("level", 1);
        return n.intValue();
    }

    public void setLevel(int level) {
        data.put("level", level);
    }

    public double getExperience() {
        Number n = (Number) data.getOrDefault("experience", 0.0);
        return n.doubleValue();
    }

    public void setExperience(double experience) {
        data.put("experience", experience);
    }

    public ResourcePool getResources() {
        return resources;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getSkillLevels() {
        return (Map<String, Integer>) data.get("skillLevels");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getProfessions() {
        return (Map<String, Integer>) data.get("professions");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Long> getSkillCooldowns() {
        // This is a bit tricky because JSON might deserialize as Double or Integer
        // We really should return a view or ensure types on load.
        // For now, let's trust the load process or cast safely.
        Object obj = data.get("skillCooldowns");
        if (obj instanceof Map) {
            return (Map<String, Long>) obj;
        }
        return new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Double> getSavedStats() {
        Object obj = data.get("savedStats");
        if (obj instanceof Map) {
            return (Map<String, Double>) obj;
        }
        return new ConcurrentHashMap<>();
    }

    public boolean isLoaded() {
        return (boolean) data.getOrDefault("isLoaded", false);
    }

    public void setLoaded(boolean loaded) {
        data.put("isLoaded", loaded);
    }

    /**
     * Get the raw map for serialization.
     * Ensure we sync separate objects like Resources back to the map before saving.
     */
    public Map<String, Object> toMap() {
        // Sync Resources
        data.put("currentMana", resources.getCurrentMana());
        data.put("currentStamina", resources.getCurrentStamina());

        return data;
    }

    /**
     * Reconstruct from Map (Deserialization)
     */
    @SuppressWarnings("unchecked")
    public static PlayerData fromMap(UUID uuid, Map<String, Object> map) {
        PlayerData pd = new PlayerData(uuid);
        if (map != null) {
            pd.data.putAll(map);

            // Sync Resources from map
            if (map.containsKey("currentMana")) {
                Number m = (Number) map.get("currentMana");
                pd.resources.setCurrentMana(m.doubleValue());
            }
            if (map.containsKey("currentStamina")) {
                Number s = (Number) map.get("currentStamina");
                pd.resources.setCurrentStamina(s.doubleValue());
            }

            // Ensure collections are mutable maps if they came from JSON types
            ensureConcurrent("skillLevels", pd);
            ensureConcurrent("professions", pd);
            ensureConcurrent("skillCooldowns", pd);

            // Fix types for Cooldowns (JSON double -> long)
            Map<String, Object> cds = (Map<String, Object>) pd.data.get("skillCooldowns");
            // We need to make sure they are Longs because getSkillCooldowns returns
            // Map<String, Long>
            for (Map.Entry<String, Object> entry : cds.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    entry.setValue(((Number) entry.getValue()).longValue());
                }
            }

            // Ensure "savedStats" is present and correct type
            ensureConcurrent("savedStats", pd);
            Map<String, Object> savedStats = (Map<String, Object>) pd.data.get("savedStats");
            for (Map.Entry<String, Object> entry : savedStats.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    entry.setValue(((Number) entry.getValue()).doubleValue());
                }
            }
        }
        pd.setLoaded(true);
        return pd;

    }

    private static void ensureConcurrent(String key, PlayerData pd) {
        Object obj = pd.data.get(key);
        if (obj instanceof Map) {
            // Check if it's already ConcurrentHashMap, if not, wrap/copy
            if (!(obj instanceof ConcurrentHashMap)) {
                pd.data.put(key, new ConcurrentHashMap<>((Map<?, ?>) obj));
            }
        } else {
            pd.data.put(key, new ConcurrentHashMap<>());
        }
    }
}
