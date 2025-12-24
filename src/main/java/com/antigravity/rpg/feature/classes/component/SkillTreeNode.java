package com.antigravity.rpg.feature.classes.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 스킬 트리의 개별 노드 설정을 정의하는 클래스입니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillTreeNode {
    private String skillId; // 스킬 고유 ID
    private String type = "ACTIVE"; // ACTIVE 또는 PASSIVE
    private int maxLevel = 5; // 최대 레벨
    private int pointsPerLevel = 1; // 레벨당 필요 포인트

    private List<String> parentSkills; // 선행 스킬 ID 목록 (모두 습득해야 함)
    private List<String> requirements; // 추가 습득 조건 (예: level >= 20)

    // GUI 표시용 좌표
    private int x;
    private int y;
}
