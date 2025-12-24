package com.antigravity.rpg.feature.player;

import lombok.Data;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 플레이어의 직업 상태(슬롯 및 진행도)를 관리하는 데이터 컨테이너입니다.
 */
@Data
public class PlayerClassData {
    // 현재 장착중인 직업 (슬롯 -> 직업ID)
    private Map<ClassType, String> activeClasses = new ConcurrentHashMap<>();

    // 보유한 모든 직업의 진행도 (직업ID -> 진행정보)
    private Map<String, ClassProgress> classProgressMap = new ConcurrentHashMap<>();

    public PlayerClassData() {
        // 초기화
    }

    public void setClass(ClassType type, String classId) {
        activeClasses.put(type, classId);
        // 처음 얻는 직업이면 진행도 초기화
        classProgressMap.computeIfAbsent(classId, k -> new ClassProgress(k, 1, 0.0));
    }

    public String getClassId(ClassType type) {
        return activeClasses.get(type);
    }

    public ClassProgress getProgress(String classId) {
        return classProgressMap.get(classId);
    }

    public ClassProgress getActiveProgress(ClassType type) {
        String id = getClassId(type);
        if (id == null)
            return null;
        return getProgress(id);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new ConcurrentHashMap<>();

        // Active Classes (Enum -> String)
        Map<String, String> activeMap = new ConcurrentHashMap<>();
        for (Map.Entry<ClassType, String> entry : activeClasses.entrySet()) {
            activeMap.put(entry.getKey().name(), entry.getValue());
        }
        map.put("activeClasses", activeMap);

        // Progress Map (String -> ClassProgress Map)
        Map<String, Object> progressMapSerial = new ConcurrentHashMap<>();
        for (Map.Entry<String, ClassProgress> entry : classProgressMap.entrySet()) {
            progressMapSerial.put(entry.getKey(), entry.getValue().toMap());
        }
        map.put("classProgress", progressMapSerial);

        return map;
    }

    @SuppressWarnings("unchecked")
    public static PlayerClassData fromMap(Map<String, Object> map) {
        PlayerClassData pcd = new PlayerClassData();
        if (map == null)
            return pcd;

        // Active Classes
        if (map.containsKey("activeClasses")) {
            Map<String, String> activeMap = (Map<String, String>) map.get("activeClasses");
            for (Map.Entry<String, String> entry : activeMap.entrySet()) {
                try {
                    ClassType type = ClassType.valueOf(entry.getKey());
                    pcd.activeClasses.put(type, entry.getValue());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        // Progress Map
        if (map.containsKey("classProgress")) {
            Map<String, Object> progressMapSerial = (Map<String, Object>) map.get("classProgress");
            for (Map.Entry<String, Object> entry : progressMapSerial.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    ClassProgress cp = ClassProgress.fromMap((Map<String, Object>) entry.getValue());
                    if (cp != null) {
                        pcd.classProgressMap.put(entry.getKey(), cp);
                    }
                }
            }
        }

        return pcd;
    }
}
