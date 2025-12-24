package com.antigravity.rpg.core.engine.listener;

import com.antigravity.rpg.core.ecs.EntityRegistry;
import com.antigravity.rpg.core.engine.DamageContext;
import com.antigravity.rpg.core.engine.DamageProcessor;
import com.antigravity.rpg.core.engine.EntityStatData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

/**
 * Bukkit의 데미지 이벤트를 ECS 시스템으로 전달하는 어댑터 클래스입니다.
 */
public class CombatEventListener implements Listener {

    private final EntityRegistry entityRegistry;
    private final DamageProcessor damageProcessor;
    private final PlayerProfileService playerProfileService;

    @Inject
    public CombatEventListener(EntityRegistry entityRegistry, DamageProcessor damageProcessor,
            PlayerProfileService playerProfileService) {
        this.entityRegistry = entityRegistry;
        this.damageProcessor = damageProcessor;
        this.playerProfileService = playerProfileService;
    }

    /**
     * 엔티티가 다른 엔티티에게 데미지를 입힐 때 발생하는 이벤트입니다.
     * HIGH 우선순위로 설정하여 다른 플러그인의 보정을 받은 후 최종 MMORPG 계산을 수행합니다.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity attacker = event.getDamager();
        Entity victim = event.getEntity();

        if (!(victim instanceof LivingEntity))
            return;

        // 공격자와 피해자가 ECS 시스템에 등록된 엔티티인지 확인
        EntityStatData attackerStats = getStatData(attacker);
        EntityStatData victimStats = getStatData(victim);

        // 둘 중 하나라도 등록된 경우 (플레이어 또는 특수 몹) RPG 데미지 계산 수행
        if (attackerStats != null || victimStats != null) {
            DamageContext context = new DamageContext(
                    attacker,
                    victim,
                    attackerStats,
                    victimStats,
                    event.getDamage());

            // DamageProcessor를 통해 최종 데미지 계산
            damageProcessor.process(context);

            // 계산된 최종 데미지를 이벤트에 반영
            event.setDamage(context.getFinalDamage());

            // [NEW] Lua Hooks Trigger
            triggerLuaHooks(attacker, victim, context.getFinalDamage());
        }
    }

    private void triggerLuaHooks(Entity attacker, Entity victim, double damage) {
        // 1. 공격자 Hook (onDamageDealt)
        if (attacker instanceof org.bukkit.entity.Player pAttacker) {
            try {
                com.antigravity.rpg.feature.player.PlayerData pd = playerProfileService
                        .getProfileSync(pAttacker.getUniqueId());
                if (pd != null) {
                    pd.getClassData().getActiveClasses().values().forEach(classId -> {
                        com.antigravity.rpg.feature.player.PlayerData.getClassRegistry().getClass(classId)
                                .ifPresent(def -> {
                                    def.onEvent("onDamageDealt", attacker, victim, damage);
                                    def.onEvent("onHit", attacker, victim, damage); // Alias
                                });
                    });
                }
            } catch (Exception e) {
            }
        }

        // 2. 피해자 Hook (onDamageTaken)
        if (victim instanceof org.bukkit.entity.Player pVictim) {
            try {
                com.antigravity.rpg.feature.player.PlayerData pd = playerProfileService
                        .getProfileSync(pVictim.getUniqueId());
                if (pd != null) {
                    pd.getClassData().getActiveClasses().values().forEach(classId -> {
                        com.antigravity.rpg.feature.player.PlayerData.getClassRegistry().getClass(classId)
                                .ifPresent(def -> {
                                    def.onEvent("onDamageTaken", attacker, victim, damage);
                                });
                    });
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * 엔티티로부터 StatData 컴포넌트를 가져옵니다.
     * 플레이어의 경우 PlayerProfileService를 우선 참조하며, 일반 엔티티는 EntityRegistry를 참조합니다.
     */
    private EntityStatData getStatData(Entity entity) {
        UUID uuid = entity.getUniqueId();

        // 1. EntityRegistry 확인 (몬스터 등)
        var componentOpt = entityRegistry.getComponent(uuid, EntityStatData.class);
        if (componentOpt.isPresent()) {
            return componentOpt.get();
        }

        // 2. 플레이어 데이터 확인
        if (entity instanceof org.bukkit.entity.Player) {
            var dataFuture = playerProfileService.find(uuid);
            if (dataFuture.isDone()) {
                try {
                    var data = dataFuture.get();
                    if (data != null) {
                        // PlayerData 자체가 StatHolder를 구현하므로,
                        // DamageContext가 StatHolder를 받도록 설계되었다면 바로 사용 가능하지만
                        // 현재 DamageContext는 EntityStatData를 요구함.
                        // PlayerData에서 EntityStatData 컴포넌트를 가져오거나
                        // PlayerData의 스탯을 EntityStatData로 래핑해야 함.

                        EntityStatData stats = data.getComponent(EntityStatData.class);
                        if (stats == null) {
                            // 임시로 PlayerData의 스탯을 복사하거나 리턴 (추후 통합 필요)
                            return createStatDataFromPlayer(data);
                        }
                        return stats;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    private EntityStatData createStatDataFromPlayer(com.antigravity.rpg.feature.player.PlayerData data) {
        EntityStatData stats = new EntityStatData();
        // 주요 스탯 복사 (예시)
        stats.setStat("attack_damage", data.getStat("attack_damage"));
        stats.setStat("defense", data.getStat("defense"));
        return stats;
    }
}
