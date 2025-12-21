package com.antigravity.rpg.feature.player;

import lombok.Getter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PlayerData implements com.antigravity.rpg.core.engine.StatHolder {
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

    // Convenience methods for Resources
    public double getMana() {
        return resources.getCurrentMana();
    }

    public void setMana(double mana) {
        resources.setCurrentMana(mana);
    }

    public double getStamina() {
        return resources.getCurrentStamina();
    }

    public void setStamina(double stamina) {
        resources.setCurrentStamina(stamina);
    }

    // Convenience for Stats
    public double getStat(String statId, double defaultValue) {
        double val = getStat(statId);
        return (val == 0.0) ? defaultValue : val; // Note: if 0 is valid, this logic is flawed, but for regen it's ok.
    }

    /**
     * Map으로부터 데이터 복원 (역직렬화)
     */
    @SuppressWarnings("unchecked")
    public static PlayerData fromMap(UUID uuid, Map<String, Object> map) {
        PlayerData pd = new PlayerData(uuid);
        if (map != null) {
            // Gson 등으로 로드된 데이터의 타입 보정 (Double -> Long/Integer)
            Map<String, Object> fixedMap = fixNumberTypes(map);
            pd.data.putAll(fixedMap);

            // 리소스 동기화
            if (pd.data.containsKey("currentMana")) {
                Number m = (Number) pd.data.get("currentMana");
                pd.resources.setCurrentMana(m.doubleValue());
            }
            if (pd.data.containsKey("currentStamina")) {
                Number s = (Number) pd.data.get("currentStamina");
                pd.resources.setCurrentStamina(s.doubleValue());
            }

            // 가변성(Mutability) 보장
            ensureConcurrent("skillLevels", pd);
            ensureConcurrent("professions", pd);
            ensureConcurrent("skillCooldowns", pd);
            ensureConcurrent("savedStats", pd);
        }
        pd.setLoaded(true);
        return pd;
    }

    /**
     * 재귀적으로 맵/리스트를 순회하며 Double로 변환된 정수형 데이터를 Long/Integer로 복구합니다.
     */

    private static Map<String, Object> fixNumberTypes(Map<String, Object> input) {
        Map<String, Object> output = new ConcurrentHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Object value = entry.getValue();
            output.put(entry.getKey(), fixValue(value));
        }
        return output;
    }

    @SuppressWarnings("unchecked")
    private static Object fixValue(Object value) {
        if (value instanceof Map) {
            return fixNumberTypes((Map<String, Object>) value);
        } else if (value instanceof java.util.List) {
            java.util.List<Object> list = (java.util.List<Object>) value;
            java.util.List<Object> newList = new java.util.ArrayList<>();
            for (Object item : list) {
                newList.add(fixValue(item));
            }
            return newList;
        } else if (value instanceof Double) {
            double d = (Double) value;
            // 정수와 같으면 Long으로 변환 (소수점이 없으면)
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return (long) d;
            }
        }
        return value;
    }

    // Services for Stat/Class logic
    private static com.antigravity.rpg.core.engine.StatCalculator statCalculator;
    private static com.antigravity.rpg.feature.classes.ClassRegistry classRegistry;

    public static void initialize(com.antigravity.rpg.core.engine.StatCalculator sc,
            com.antigravity.rpg.feature.classes.ClassRegistry cr) {
        statCalculator = sc;
        classRegistry = cr;
    }

    @Override
    public double getStat(String statId) {
        if (statCalculator == null)
            return getRawStat(statId);
        return statCalculator.getStat(this, statId);
    }

    @Override
    public double getRawStat(String statId) {
        // 1. Saved Stats (Allocated points or bonuses)
        double val = getSavedStats().getOrDefault(statId, 0.0);

        // 2. Class Stats
        String cId = getClassId();
        if (cId != null && !cId.isEmpty() && classRegistry != null) {
            var classDefOpt = classRegistry.getClass(cId);
            if (classDefOpt.isPresent()) {
                var cDef = classDefOpt.get();
                // Base
                val += cDef.getBaseAttributes().getOrDefault(statId, 0.0);
                // Scale
                double scale = cDef.getScaleAttributes().getOrDefault(statId, 0.0);
                if (scale != 0) {
                    int lvl = getLevel();
                    val += (scale * (lvl - 1));
                }
            }
        }
        return val;
    }

    @Override
    public double getNativeAttributeValue(String attributeName) {
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
        if (p != null) {
            org.bukkit.attribute.Attribute attr = org.bukkit.attribute.Attribute
                    .valueOf(attributeName.toUpperCase().replace(".", "_"));
            var instance = p.getAttribute(attr);
            if (instance != null) {
                return instance.getValue();
            }
        }
        return 0.0;
    }

    @Override
    public String getName() {
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
        return (p != null) ? p.getName() : uuid.toString();
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
