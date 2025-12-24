package com.antigravity.rpg.feature.player;

import lombok.Getter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

/**
 * 플레이어의 모든 데이터를 담고 있는 데이터 객체입니다.
 * ECS 컴포넌트 저장소, 리소스 풀, 스탯 계산 로직 등을 포함합니다.
 */
public class PlayerData implements com.antigravity.rpg.core.engine.StatHolder,
        com.antigravity.rpg.core.engine.StatCalculator.PlayerDataFunc,
        com.antigravity.rpg.core.engine.StatCalculator.NativeStatHolder {
    @Getter
    private final UUID uuid;

    @Getter
    private boolean dirty = false;

    // 핵심 데이터 저장소 (레벨, 경험치 등 가변 데이터)
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    // ECS 컴포넌트 저장소
    private final Map<Class<?>, Object> components = new ConcurrentHashMap<>();

    // 리소스 풀 (마나, 스태미나 등)
    private final ResourcePool resources;

    // [NEW] 직업 슬롯 및 진행도 데이터
    @Getter
    private final PlayerClassData classData = new PlayerClassData();

    // 마지막 전투 시간 (밀리초, 전투 상태 확인용)
    private long lastCombatTime = 0L;

    // [NEW] 커스텀 장비 슬롯
    @Getter
    private final Map<com.antigravity.rpg.feature.item.EquipmentSlot, org.bukkit.inventory.ItemStack> equipment;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        // 기본값 초기화
        this.data.put("skillLevels", new ConcurrentHashMap<String, Integer>());
        this.data.put("professions", new ConcurrentHashMap<String, Integer>());
        this.data.put("skillCooldowns", new ConcurrentHashMap<String, Number>());
        this.data.put("savedStats", new ConcurrentHashMap<String, Double>());
        this.data.put("skillPoints", 0);

        this.resources = new ResourcePool();
        this.equipment = new ConcurrentHashMap<>();
    }

    // --- 데이터 접근자 (Accessors) ---
    public void set(String key, Object value) {
        data.put(key, value);
        markDirty();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
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
        String main = classData.getClassId(ClassType.MAIN);
        return main != null ? main : "";
    }

    public void setClassId(String classId) {
        classData.setClass(ClassType.MAIN, classId);
        markDirty();
    }

    public int getLevel() {
        ClassProgress cp = classData.getActiveProgress(ClassType.MAIN);
        return cp != null ? cp.getLevel() : 1;
    }

    public void setLevel(int level) {
        // MAIN 클래스 레벨 설정 (없으면 진행도 생성됨)
        String mainId = getClassId();
        if (mainId == null || mainId.isEmpty())
            return;

        ClassProgress cp = classData.getProgress(mainId);
        if (cp != null) {
            cp.setLevel(level);
            markDirty();
        }
    }

    public int getSkillPoints() {
        Number n = (Number) data.getOrDefault("skillPoints", 0);
        return n.intValue();
    }

    public void setSkillPoints(int points) {
        data.put("skillPoints", points);
        markDirty();
    }

    public double getExperience() {
        ClassProgress cp = classData.getActiveProgress(ClassType.MAIN);
        return cp != null ? cp.getExperience() : 0.0;
    }

    public void setExperience(double experience) {
        String mainId = getClassId();
        if (mainId == null || mainId.isEmpty())
            return;

        ClassProgress cp = classData.getProgress(mainId);
        if (cp != null) {
            cp.setExperience(experience);
            markDirty();
        }
    }

    public ResourcePool getResources() {
        return resources;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Integer> getSkillLevels() {
        return (Map<String, Integer>) data.get("skillLevels");
    }

    public int getSkillLevel(String skillId) {
        Map<String, Integer> levels = getSkillLevels();
        return levels != null ? levels.getOrDefault(skillId, 0) : 0;
    }

    public void setSkillLevel(String skillId, int level) {
        Map<String, Integer> levels = getSkillLevels();
        if (levels != null) {
            levels.put(skillId, level);
            markDirty();
        }
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
     * 마지막 전투 시간을 반환합니다 (밀리초).
     * 전투 상태 확인에 사용됩니다.
     */
    public long getLastCombatTime() {
        return lastCombatTime;
    }

    /**
     * 마지막 전투 시간을 갱신합니다.
     * 피격/공격 시 호출해야 합니다.
     */
    public void updateCombatTime() {
        this.lastCombatTime = System.currentTimeMillis();
    }

    /**
     * 저장을 위해 데이터를 Map 형태로 변환합니다.
     */
    public Map<String, Object> toMap() {
        // 리소스 상태 동기화
        data.put("currentMana", resources.getCurrentMana());
        data.put("currentStamina", resources.getCurrentStamina());

        // [NEW] ClassData 저장
        data.put("playerClassData", classData.toMap());

        return data;
    }

    public void setMana(double mana) {
        resources.setCurrentMana(mana);
        markDirty();
    }

    // --- 리소스 편의 메서드 ---
    public double getMana() {
        return resources.getCurrentMana();
    }

    public double getStamina() {
        return resources.getCurrentStamina();
    }

    public void setStamina(double stamina) {
        resources.setCurrentStamina(stamina);
        markDirty();
    }

    public double getStat(String statId, double defaultValue) {
        double val = getStat(statId);
        return (val == 0.0) ? defaultValue : val;
    }

    /**
     * Map으로부터 데이터를 복원합니다 (역직렬화 지원).
     */
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

            // [NEW] ClassData 복구 및 마이그레이션
            if (pd.data.containsKey("playerClassData")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> classDataMap = (Map<String, Object>) pd.data.get("playerClassData");
                PlayerClassData loadedClassData = PlayerClassData.fromMap(classDataMap);

                // loadedClassData 내용을 pd.classData에 복사 (final 필드이므로)
                pd.classData.getActiveClasses().putAll(loadedClassData.getActiveClasses());
                pd.classData.getClassProgressMap().putAll(loadedClassData.getClassProgressMap());
            } else {
                // 기존 데이터 마이그레이션 (level, experience, classId)
                String oldClassId = (String) pd.data.get("classId");
                if (oldClassId != null && !oldClassId.isEmpty()) {
                    int oldLevel = ((Number) pd.data.getOrDefault("level", 1)).intValue();
                    double oldExp = ((Number) pd.data.getOrDefault("experience", 0.0)).doubleValue();

                    pd.classData.setClass(ClassType.MAIN, oldClassId);
                    ClassProgress cp = pd.classData.getProgress(oldClassId);
                    if (cp != null) { // setClass 내부에서 생성됨
                        cp.setLevel(oldLevel);
                        cp.setExperience(oldExp);
                    }
                }
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
    private static com.antigravity.rpg.feature.classes.condition.ConditionManager conditionManager;

    public static void initialize(com.antigravity.rpg.core.engine.StatCalculator sc,
            com.antigravity.rpg.feature.classes.ClassRegistry cr,
            com.antigravity.rpg.feature.classes.condition.ConditionManager cm) {
        statCalculator = sc;
        classRegistry = cr;
        conditionManager = cm;
    }

    public static com.antigravity.rpg.feature.classes.ClassRegistry getClassRegistry() {
        return classRegistry;
    }

    @Override
    public double getStat(String statId) {
        if (statCalculator == null)
            return getRawStat(statId);
        return statCalculator.getStat(this, statId);
    }

    // 외부 스탯 수정자 (시너지, 버프 등)
    private final Map<String, Double> externalModifiers = new ConcurrentHashMap<>();

    public void addModifier(String statId, double value) {
        externalModifiers.put(statId, externalModifiers.getOrDefault(statId, 0.0) + value);
    }

    public void removeModifier(String statId, double value) {
        double current = externalModifiers.getOrDefault(statId, 0.0);
        externalModifiers.put(statId, Math.max(0, current - value));
    }

    public void clearModifiers() {
        externalModifiers.clear();
    }

    @Override
    public double getRawStat(String statId) {
        // 0. 레벨 정보 우선 처리
        if (statId.equalsIgnoreCase("level")) {
            return (double) getLevel();
        }

        // 1. 저장된 추가 스탯 (스탯 포인트 등)
        double val = getSavedStats().getOrDefault(statId, 0.0);

        // 2. 클래스별 기본 스탯 반영
        String cId = getClassId();
        if (cId != null && !cId.isEmpty() && classRegistry != null) {
            var classDefOpt = classRegistry.getClass(cId);
            if (classDefOpt.isPresent()) {
                var cDef = classDefOpt.get();
                if (cDef.getAttributes() != null && cDef.getAttributes().getBase() != null) {
                    val += cDef.getAttributes().getBase().getOrDefault(statId, 0.0);
                }
            }
        }

        // 3. 외부 수정자 합산 (시너지 등)
        val += externalModifiers.getOrDefault(statId, 0.0);

        // 4. 커스텀 장비 스탯 반영
        for (org.bukkit.inventory.ItemStack item : equipment.values()) {
            if (item == null || item.getType().isAir())
                continue;
            val += getItemStat(item, statId);
        }

        return val;
    }

    /**
     * 아이템으로부터 특정 스탯 값을 가져옵니다.
     */
    private double getItemStat(org.bukkit.inventory.ItemStack item, String statId) {
        if (!item.hasItemMeta())
            return 0.0;
        // 임시: PDC에서 직접 읽기 (Phase 6에서 PDCAdapter로 고도화 예정)
        org.bukkit.persistence.PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("mmorpg", "stat_" + statId.toLowerCase());
        return pdc.getOrDefault(key, org.bukkit.persistence.PersistentDataType.DOUBLE, 0.0);
    }

    /**
     * 플레이어의 스탯을 재계산하고 필요 시 UI를 업데이트합니다.
     */
    public void recalculateStats() {
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(uuid);
        if (p == null)
            return;

        // 1. 마스터리 보너스 업데이트
        updateMasteryBonuses(p);

        // 2. 최대 체력 동기화
        double maxHealth = getStat("MAX_HEALTH", 20.0);
        AttributeInstance attrInstance = null;
        try {
            attrInstance = p.getAttribute(Attribute.valueOf("MAX_HEALTH"));
        } catch (Exception e) {
            try {
                attrInstance = p.getAttribute(Attribute.valueOf("GENERIC_MAX_HEALTH"));
            } catch (Exception e2) {
            }
        }

        if (attrInstance != null) {
            attrInstance.setBaseValue(maxHealth);
        }

        // 추가적인 스탯 동기화 로직...
    }

    private void updateMasteryBonuses(org.bukkit.entity.Player player) {
        // 기존 마스터리 보너스 초기화 (마스터리 전용 Modifier를 구분해서 관리하면 좋지만, 여기서는 단순화)
        // 실제 운영 시에는 'mastery_' 접두사 등을 붙여 관리하는 것이 안전함.

        String cId = getClassId();
        if (cId == null || cId.isEmpty() || classRegistry == null || conditionManager == null)
            return;

        classRegistry.getClass(cId).ifPresent(def -> {
            if (def.getEquipment() != null && def.getEquipment().getMasteryBonus() != null) {
                for (com.antigravity.rpg.feature.classes.component.EquipmentRules.MasteryBonus bonus : def
                        .getEquipment()
                        .getMasteryBonus()) {
                    if (conditionManager.check(this, bonus.getCondition(), player)) {
                        // 조건 충족 시 보너스 적용
                        if (bonus.getStats() != null) {
                            bonus.getStats().forEach((statId, value) -> {
                                if (value != null)
                                    addModifier(statId, value);
                            });
                        }
                    } else {
                        // 조건 미충족 시 보너스 제거
                        if (bonus.getStats() != null) {
                            bonus.getStats().forEach((statId, value) -> {
                                if (value != null)
                                    removeModifier(statId, value);
                            });
                        }
                    }
                }
            }
        });
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
