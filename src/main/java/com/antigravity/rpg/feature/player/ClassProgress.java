package com.antigravity.rpg.feature.player;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 특정 직업의 레벨 및 경험치 진행 상황을 저장하는 데이터 클래스입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassProgress {
    private String classId;
    private int level = 1;
    private double experience = 0.0;

    // 필요 시 전직 단계, 스킬 포인트 등을 여기에 추가 가능

    public void addExperience(double amount) {
        this.experience += amount;
    }

    public void levelUp() {
        this.level++;
        this.experience = 0.0; // 레벨업 시 경험치 초기화 또는 이월 로직
    }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("classId", classId);
        map.put("level", level);
        map.put("experience", experience);
        return map;
    }

    public static ClassProgress fromMap(java.util.Map<String, Object> map) {
        if (map == null)
            return null;
        String id = (String) map.get("classId");
        int lvl = ((Number) map.getOrDefault("level", 1)).intValue();
        double exp = ((Number) map.getOrDefault("experience", 0.0)).doubleValue();
        return new ClassProgress(id, lvl, exp);
    }
}
