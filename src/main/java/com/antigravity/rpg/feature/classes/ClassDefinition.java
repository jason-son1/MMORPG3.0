package com.antigravity.rpg.feature.classes;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 직업 정의를 담는 클래스입니다.
 * 완전한 데이터 기반 시스템을 위해 스탯, 성장, 스킬, 장비 제한, AI, 시너지 정보를 모두 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDefinition {
    private String key; // 직업 고유 키 (예: warrior)
    private String parent; // 부모 직업 키 (상속용)
    private String displayName; // 표시 이름
    private String lore; // 직업 설명
    private Role role; // 직업 역할 (TANK, DPS 등)

    private Attributes attributes; // 핵심 속성
    private Growth growth; // 레벨 성장 정보
    private Skills skills; // 사용 및 패시브 스킬
    private Equipment equipment; // 장비 제한 및 마스터리
    private AIBehavior aiBehavior; // AI 행동 패턴
    private Synergy synergy; // 파티 시너지

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Attributes {
        private String primary; // 주 능력치 (예: STRENGTH)
        private CombatStyle combatStyle; // 전투 스타일 (MELEE, RANGED 등)
        private ResourceType resourceType; // 자원 유형 (MANA, RAGE 등)
        private Map<String, Double> base; // 1레벨 기본 스탯
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Growth {
        private Map<String, String> perLevel; // 레벨당 증가량 (고정값 또는 Lua 수식)
        private List<Advancement> advancement; // 전직/진화 단계
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Advancement {
        private int level;
        private List<String> branches; // 전직 가능한 직업 목록
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
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassiveSkill {
        private String id;
        private int unlockLevel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Equipment {
        private List<String> allowWeapons; // 허용 무기 타입
        private List<String> allowArmors; // 허용 방어구 타입
        private List<MasteryBonus> masteryBonus; // 마스터리 보너스
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MasteryBonus {
        private String condition; // 조건 (예: holds_dual_wield)
        private Map<String, Double> stats; // 적용 스탯
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIBehavior {
        private TargetPriority targetPriority;
        private double combatDistance;
        private List<SkillRotation> skillRotation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillRotation {
        private String skill;
        private String condition; // 시전 조건
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Synergy {
        private double auraRange;
        private List<SynergyEffect> effects;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SynergyEffect {
        private String type; // 효과 타입 (BUFF, DEBUFF)
        private String target; // 대상 (PARTY, ALLY, SELF)
        private String stat; // 변경할 스탯
        private double value; // 변경 수치
    }

    public enum Role {
        TANK, MELEE_DPS, RANGE_DPS, MAGIC_DPS, SUPPORT, HEALER
    }

    public enum CombatStyle {
        MELEE, RANGED, MAGIC, HYBRID
    }

    public enum ResourceType {
        MANA, RAGE, ENERGY, STAMINA, NONE
    }

    public enum TargetPriority {
        LOWEST_HP, CLOSEST, HIGHEST_THREAT, RANDOM
    }
}
