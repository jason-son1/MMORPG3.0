package com.antigravity.rpg.core.engine;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import java.util.HashSet;
import java.util.Set;

/**
 * 데미지 계산에 필요한 모든 정보를 담는 컨텍스트입니다.
 */
public class DamageContext {

    private final Entity attacker;
    private final Entity victim;
    private final EntityStatData attackerStats;
    private final EntityStatData victimStats;

    private double initialDamage;
    private double finalDamage;
    private boolean isCritical;

    private Set<String> tags = new HashSet<>();
    private Set<String> attackerTags = new HashSet<>();
    private Set<String> victimTags = new HashSet<>();

    // 기존 CombatService 호환 생성자
    public DamageContext(Entity attacker, Entity victim, EntityStatData attackerStats, EntityStatData victimStats,
            double initialDamage) {
        this.attacker = attacker;
        this.victim = victim;
        this.attackerStats = attackerStats;
        this.victimStats = victimStats;
        this.initialDamage = initialDamage;
    }

    // 신규 Action 시스템용 생성자 (스탯 없이 시작 가능)
    public DamageContext(Entity attacker, Entity victim, double initialDamage) {
        this(attacker, victim, null, null, initialDamage);
    }

    public Entity getAttacker() {
        return attacker;
    }

    public Entity getVictim() {
        return victim;
    }

    public EntityStatData getAttackerStats() {
        return attackerStats;
    }

    public EntityStatData getVictimStats() {
        return victimStats;
    }

    public double getInitialDamage() {
        return initialDamage;
    }

    public double getFinalDamage() {
        return finalDamage;
    }

    public void setFinalDamage(double finalDamage) {
        this.finalDamage = finalDamage;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    // Tag Methods
    public void addTag(String tag) {
        tags.add(tag);
    }

    public void addTag(Enum<?> tag) {
        tags.add(tag.name());
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public Set<String> getTags() {
        return tags;
    }

    public Set<String> getAttackerTags() {
        return attackerTags;
    }

    public void setAttackerTags(Set<String> attackerTags) {
        this.attackerTags = attackerTags;
    }

    public Set<String> getVictimTags() {
        return victimTags;
    }

    public void setVictimTags(Set<String> victimTags) {
        this.victimTags = victimTags;
    }
}
