package com.antigravity.rpg.feature.skill.context;

import com.antigravity.rpg.core.engine.StatRegistry;
import com.antigravity.rpg.feature.player.PlayerData;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 스킬 시전 시 생성되어 종료 시까지 유지되는 통합 컨텍스트 객체입니다.
 * Caster Snapshot, Target List, Variables 등을 관리하며 모든 모듈이 이를 통해 데이터를 공유합니다.
 */
@Getter
@Setter
public class SkillCastContext {

    // --- Caster Snapshot ---
    private final UUID casterId;
    private final PlayerData casterData;
    private final Map<String, Double> casterStatsSnapshot;
    private final Entity casterEntity;

    // --- Target List ---
    private final List<Entity> targets = new ArrayList<>();
    private final List<ActiveMob> mythicTargets = new ArrayList<>();

    // --- Variables Map ---
    private final Map<String, Object> variables = new ConcurrentHashMap<>();

    // --- Metadata ---
    private final UUID instanceId = UUID.randomUUID();
    private final long startTime = System.currentTimeMillis();
    private Location originLocation;

    @Builder
    public SkillCastContext(Player caster, PlayerData data, StatRegistry statRegistry) {
        this.casterId = caster.getUniqueId();
        this.casterEntity = caster;
        this.casterData = data;
        this.originLocation = caster.getLocation();

        // 스탯 스냅샷 생성 (실시간 변동 방지)
        this.casterStatsSnapshot = new HashMap<>();
        if (data != null && statRegistry != null) {
            for (String statId : statRegistry.getStatIds()) {
                this.casterStatsSnapshot.put(statId, data.getStat(statId));
            }
        }
    }

    /**
     * 특정 변수 값을 가져옵니다.
     */
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, T defaultValue) {
        return (T) variables.getOrDefault(key, defaultValue);
    }

    /**
     * 변수 값을 설정합니다.
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    /**
     * 타겟 목록에 엔티티를 추가합니다. (MythicMob 포함 여부 자동 확인)
     */
    public void addTarget(Entity entity) {
        if (!targets.contains(entity)) {
            targets.add(entity);

            // MythicMob 확인 (MythicBukkit.inst().getMobManager() 사용)
            MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId())
                    .ifPresent(mythicTargets::add);
        }
    }

    /**
     * 타겟 목록을 초기화하고 새로 설정합니다.
     */
    public void setTargets(Collection<Entity> newTargets) {
        targets.clear();
        mythicTargets.clear();
        newTargets.forEach(this::addTarget);
    }

    /**
     * 첫 번째 타겟을 반환합니다.
     */
    public Optional<Entity> getFirstTarget() {
        return targets.isEmpty() ? Optional.empty() : Optional.of(targets.get(0));
    }

    /**
     * 현재 컨텍스트의 사본을 생성합니다. (사본은 독립된 타겟 목록과 변수 맵을 가집니다)
     */
    public SkillCastContext copy() {
        SkillCastContext copy = new SkillCastContext((Player) casterEntity, casterData, null);
        // statRegistry는 이미 스냅샷이 있으므로 null 전달 (생성자에서 null 체크함)

        copy.casterStatsSnapshot.putAll(this.casterStatsSnapshot);
        copy.targets.addAll(this.targets);
        copy.mythicTargets.addAll(this.mythicTargets);
        copy.variables.putAll(this.variables);
        copy.originLocation = this.originLocation.clone();

        return copy;
    }
}
