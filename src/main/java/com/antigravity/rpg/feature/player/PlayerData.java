package com.antigravity.rpg.feature.player;

import lombok.Data;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Data
public class PlayerData {
    private final UUID uuid;
    private final ResourcePool resources = new ResourcePool();
    private final Map<String, Integer> skillLevels = new ConcurrentHashMap<>();
    private final Map<String, Double> savedStats = new ConcurrentHashMap<>();

    // Metadata / Temp Flags
    private transient boolean isLoaded = false;

    private String classId;
    private int level = 1;
    private double experience;

    private final Map<String, Long> skillCooldowns = new ConcurrentHashMap<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * PlayerData를 YAML 저장을 위한 Map으로 변환합니다.
     * 
     * @return 데이터가 담긴 Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("classId", classId != null ? classId : "");
        map.put("level", level);
        map.put("experience", experience);

        // 리소스 (ResourcePool)
        if (resources != null) {
            map.put("currentMana", resources.getCurrentMana());
            map.put("currentStamina", resources.getCurrentStamina());
        }

        // 스킬 레벨
        if (!skillLevels.isEmpty()) {
            map.put("skillLevels", new ConcurrentHashMap<>(skillLevels));
        }

        // 스킬 쿨타임 (필요 시 저장)
        if (!skillCooldowns.isEmpty()) {
            map.put("skillCooldowns", new ConcurrentHashMap<>(skillCooldowns));
        }

        return map;
    }

    /**
     * Map 데이터로부터 PlayerData 객체를 복원합니다.
     * 
     * @param uuid 플레이어 UUID
     * @param map  YAML에서 로드된 Map
     * @return 복원된 PlayerData
     */
    @SuppressWarnings("unchecked")
    public static PlayerData fromMap(UUID uuid, Map<String, Object> map) {
        PlayerData data = new PlayerData(uuid);

        if (map.containsKey("classId"))
            data.setClassId((String) map.get("classId"));
        if (map.containsKey("level"))
            data.setLevel((Integer) map.get("level"));
        if (map.containsKey("experience")) {
            Object exp = map.get("experience");
            if (exp instanceof Number)
                data.setExperience(((Number) exp).doubleValue());
        }

        // 리소스 복원
        if (map.containsKey("currentMana")) {
            Object mana = map.get("currentMana");
            if (mana instanceof Number)
                data.getResources().setCurrentMana(((Number) mana).doubleValue());
        }
        if (map.containsKey("currentStamina")) {
            Object stamina = map.get("currentStamina");
            if (stamina instanceof Number)
                data.getResources().setCurrentStamina(((Number) stamina).doubleValue());
        }

        // 스킬 레벨 복원
        if (map.containsKey("skillLevels")) {
            Object skillsObj = map.get("skillLevels");
            if (skillsObj instanceof Map) {
                Map<String, Integer> skills = (Map<String, Integer>) skillsObj;
                data.getSkillLevels().putAll(skills);
            }
        }

        // 스킬 쿨타임 복원
        if (map.containsKey("skillCooldowns")) {
            Object cdObj = map.get("skillCooldowns");
            if (cdObj instanceof Map) {
                // YAML 로드 시 Long이 Integer로 오기도 하므로 Number로 처리
                Map<String, Object> cooldowns = (Map<String, Object>) cdObj;
                cooldowns.forEach((skill, val) -> {
                    if (val instanceof Number) {
                        data.getSkillCooldowns().put(skill, ((Number) val).longValue());
                    }
                });
            }
        }

        data.setLoaded(true);
        return data;
    }
}
