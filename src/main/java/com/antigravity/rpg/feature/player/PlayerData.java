package com.antigravity.rpg.feature.player;

import lombok.Getter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 플레이어의 모든 데이터를 담고 있는 데이터 객체입니다.
 * ECS 컴포넌트 저장소, 리소스 풀, 스탯 계산 로직 등을 포함합니다.
 */
public class PlayerData implements com.antigravity.rpg.core.engine.StatHolder {
    @Getter
    private final UUID uuid;

    // 핵심 데이터 저장소 (레벨, 경험치 등 가변 데이터)
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    // ECS 컴포넌트 저장소
    private final Map<Class<?>, Object> components = new ConcurrentHashMap<>();

    // 리소스 풀 (마나, 스태미나 등)
    private final ResourcePool resources;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        // 기본값 초기화
        this.data.put("level", 1);
        this.data.put("experience", 0.0);
        this.data.put("skillLevels", new ConcurrentHashMap<String, Integer>());
        this.data.put("professions", new ConcurrentHashMap<String, Integer>());
        this.data.put("skillCooldowns", new ConcurrentHashMap<String, Number>());
        this.data.put("savedStats", new ConcurrentHashMap<String, Double>());

        this.resources = new ResourcePool();
    }

    // --- 데이터 접근자 (Accessors) ---
    public void set(String key, Object value) {
        data.put(key, value);
    }

    public <T> T get(String key, Class<T> type) {
        Object val = data.get(key);
        if (val != null && type.isAssignableFrom(val.getClass())) {
            return type.cast(val);
        }
        return null;
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

    // --- 타입 안전 래퍼 메서드 (Type-Safe Wrappers) ---
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
     * 저장을 위해 데이터를 Map 형태로 변환합니다.
     */
    public Map<String, Object> toMap() {
        // 리소스 상태 동기화
        data.put("currentMana", resources.getCurrentMana());
        data.put("currentStamina", resources.getCurrentStamina());

        return data;
    }

    // --- 리소스 편의 메서드 ---
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

    public double getStat(String statId, double defaultValue) {
        double val = getStat(statId);
        return (val == 0.0) ? defaultValue : val;
    }

    /**
     * Map으로부터 데이터를 복원합니다 (역직렬화 지원).
     */
    @SuppressWarnings("unchecked")
    public static PlayerData fromMap(UUID uuid, Map<String, Object> map) {
        PlayerData pd = new PlayerData(uuid);
        if (map != null) {
            // 숫자 타입 보정 (Gson 직렬화 시 소수점이 없어도 Double로 인식되는 문제 해결)
            Map<String, Object> fixedMap = fixNumberTypes(map);
            pd.data.putAll(fixedMap);

            // 리소스 값 복구
            if (pd.data.containsKey("currentMana")) {
                Number m = (Number) pd.data.get("currentMana");
                pd.resources.setCurrentMana(m.doubleValue());
            }
            if (pd.data.containsKey("currentStamina")) {
                Number s = (Number) pd.data.get("currentStamina");
                pd.resources.setCurrentStamina(s.doubleValue());
            }

            // 실시간 수정을 위한 가변성 보장
            ensureConcurrent("skillLevels", pd);
            ensureConcurrent("professions", pd);
            ensureConcurrent("skillCooldowns", pd);
            ensureConcurrent("savedStats", pd);
        }
        pd.setLoaded(true);
        return pd;
    }

    /**
     * 재귀적으로 맵을 순회하며 Double 타입 정수를 Long으로 복구합니다.
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
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return (long) d;
            }
        }
        return value;
    }

    // --- 스탯 및 클래스 로직 연동 ---
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
        // 1. 저장된 추가 스탯 (스탯 포인트 등)
        double val = getSavedStats().getOrDefault(statId, 0.0);

        // 2. 클래스별 기본 스탯 및 성장 가중치 반영
        String cId = getClassId();
        if (cId != null && !cId.isEmpty() && classRegistry != null) {
            var classDefOpt = classRegistry.getClass(cId);
            if (classDefOpt.isPresent()) {
                var cDef = classDefOpt.get();
                // 기본치
                val += cDef.getBaseAttributes().getOrDefault(statId, 0.0);
                // 성장치 (레벨당 가중치)
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
            try {
                org.bukkit.attribute.Attribute attr = org.bukkit.attribute.Attribute
                        .valueOf(attributeName.toUpperCase().replace(".", "_"));
                var instance = p.getAttribute(attr);
                if (instance != null) {
                    return instance.getValue();
                }
            } catch (Exception ignored) {
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
            if (!(obj instanceof ConcurrentHashMap)) {
                pd.data.put(key, new ConcurrentHashMap<>((Map<?, ?>) obj));
            }
        } else {
            pd.data.put(key, new ConcurrentHashMap<>());
        }
    }

    // --- ECS 컴포넌트 메서드 ---
    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> type) {
        return (T) components.get(type);
    }

    public <T> void addComponent(Class<T> type, T component) {
        components.put(type, component);
    }
}
