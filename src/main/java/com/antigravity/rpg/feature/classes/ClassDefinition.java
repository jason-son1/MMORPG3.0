package com.antigravity.rpg.feature.classes;

import com.antigravity.rpg.feature.classes.component.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 직업 정의를 담는 클래스입니다.
 * 컴포넌트 기반 구조를 통해 스탯, 성장, 스킬, 장비, AI, 시너지 정보를 관리합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDefinition {
    private String key; // 직업 고유 키 (예: warrior)
    private String parent; // 부모 직업 키 (상속 및 기본값 적용용)
    private String displayName; // 표시 이름 (파일 내 이름)
    private String lore; // 직업 설명
    private String role; // 직업 역할 (TANK, DPS 등 - 파티 매칭 및 AI 힌트용)

    private Attributes attributes; // 핵심 속성 및 기본 스탯
    private Growth growth; // 레벨 성장 및 전직 트리 정보
    private Skills skills; // 사용 및 패시브 스킬 목록

    // 신규 컴포넌트 기반 구조
    private EquipmentRules equipment; // 장비 제한 및 마스터리 보너스
    private AIBehavior aiBehavior; // AI 행동 패턴
    private Synergy synergy; // 파티 시너지 효과
    private ResourceSettings resourceSettings; // 전용 자원 설정 (재생/감소 모드 포함)
    private ExperienceSources experienceSources; // 경험치 획득원 설정
    private PromotionRequirements requirements; // 전직 조건
    private GUIDisplay guiDisplay; // GUI 표시 메타데이터
    private SkillTree skillTree; // [NEW] 스킬 트리 시스템

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attributes {
        private String primary; // 주 능력치 ID (예: STRENGTH)
        private String combatStyle; // 전투 스타일 (MELEE, RANGED 등)
        private Map<String, Double> base; // 1레벨 기본 스탯 Map
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Growth {
        private Map<String, String> perLevel; // 레벨당 스탯 증가량 (수식 지원)
        private List<Advancement> advancement; // 레벨업 시 전직 가능 정보
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Advancement {
        private int level;
        private List<String> branches; // 해당 레벨에서 전직 가능한 자식 직업 키 목록
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skills {
        private List<ActiveSkill> active;
        private List<PassiveSkill> passive;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveSkill {
        private String id;
        private int unlockLevel;
        private int slot;
        // 추후 확장: 선행 스킬, 필요 포인트 등
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassiveSkill {
        private String id;
        private int unlockLevel;
    }
}
