package com.antigravity.rpg.feature.combat;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.*;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.antigravity.rpg.core.ecs.SimpleEntityRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import com.antigravity.rpg.feature.social.PartyManager;
import com.antigravity.rpg.feature.quest.QuestManager;
import com.antigravity.rpg.feature.ui.DamageIndicatorService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Monster;

import java.util.concurrent.CompletableFuture;

/**
 * CombatService는 전투 시스템의 핵심 제어 서비스입니다.
 * Bukkit의 데미지 이벤트를 가로채서 자체 데미지 연산 파이프라인(DamageProcessor)으로 전달하고 최종 결과를 적용합니다.
 */
@Singleton
public class CombatService implements Service, Listener {

    private final JavaPlugin plugin;
    private final DamageProcessor damageProcessor;
    private final PlayerProfileService playerProfileService;
    private final SimpleEntityRegistry entityRegistry;

    private final PartyManager partyManager;
    private final QuestManager questManager;
    private final DamageIndicatorService damageIndicatorService;

    @Inject
    public CombatService(JavaPlugin plugin, DamageProcessor damageProcessor,
            PlayerProfileService playerProfileService, SimpleEntityRegistry entityRegistry,
            PartyManager partyManager, QuestManager questManager,
            DamageIndicatorService damageIndicatorService) {
        this.plugin = plugin;
        this.damageProcessor = damageProcessor;
        this.playerProfileService = playerProfileService;
        this.entityRegistry = entityRegistry;
        this.partyManager = partyManager;
        this.questManager = questManager;
        this.damageIndicatorService = damageIndicatorService;
    }

    @Override
    public void onEnable() {
        // 이벤트 리스너 등록 (엔티티 데미지 이벤트 감지 목적)
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[CombatService] 데미지 파이프라인이 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getName() {
        return "CombatService";
    }

    /**
     * 엔티티가 다른 엔티티나 발사체에 의해 데미지를 입을 때 호출됩니다.
     * 바닐라 데미지 계산을 무시하고 RPG 스탯 기반의 커스텀 공식을 적용합니다.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity))
            return;

        LivingEntity victim = (LivingEntity) event.getEntity();
        LivingEntity attacker = null;

        // 공격자 판별: 직접 타격 또는 발사체(화살, 마법 등)
        if (event.getDamager() instanceof LivingEntity) {
            attacker = (LivingEntity) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof LivingEntity) {
                attacker = (LivingEntity) projectile.getShooter();
            }
        }

        if (attacker == null)
            return;

        // 파티원 간 데미지 방지 (Friendly Fire)
        if (attacker instanceof Player && victim instanceof Player) {
            if (partyManager.isInSameParty(attacker.getUniqueId(), victim.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        // 공격자와 피격자의 스탯 정보를 가져옵니다.
        EntityStatData attackerStats = getStats(attacker);
        EntityStatData victimStats = getStats(victim);

        // 데이터가 없는 경우 (예: 로딩 중) 처리를 중단합니다.
        if (attackerStats == null || victimStats == null) {
            return;
        }

        // 데미지 연산을 위한 컨텍스트(Context) 생성
        DamageContext context = new DamageContext(attacker, victim, attackerStats, victimStats, event.getDamage());

        // 기본 태그 설정 (여기서는 물리 데미지로 가정)
        context.addTag(DamageTag.PHYSICAL);

        // 데미지 프로세서 실행 (컴포넌트 기반 연산 수행)
        damageProcessor.process(context);

        // 연산 결과인 최종 데미지를 이벤트에 설정합니다.
        event.setDamage(context.getFinalDamage());

        // 데미지 인디케이터 표시
        damageIndicatorService.displayDamage(victim.getLocation(), context.getFinalDamage(), context.isCritical());

        // 치명타(Critical) 발생 시 시각/청각 효과 재생
        if (context.isCritical()) {
            victim.getWorld().spawnParticle(org.bukkit.Particle.CRIT, victim.getLocation().add(0, 1, 0), 10);
            victim.getWorld().playSound(victim.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
        }
    }

    /**
     * 엔티티의 스탯 정보를 추출합니다.
     * 플레이어는 저장된 프로필에서, 몬스터는 엔티티 레지스트리에서 가져옵니다.
     */
    private EntityStatData getStats(LivingEntity entity) {
        EntityStatData stats = new EntityStatData();

        if (entity instanceof Player) {
            Player p = (Player) entity;
            // 플레이어 프로필을 비동기 캐시에서 즉시 조회합니다.
            CompletableFuture<PlayerData> future = playerProfileService.find(p.getUniqueId());
            PlayerData data = future.getNow(null);

            if (data != null) {
                // 저장된 모든 스탯을 EntityStatData로 복사합니다.
                data.getSavedStats().forEach((key, value) -> {
                    if (value != null) {
                        stats.setStat(key, value);
                    }
                });

                // 최소 공격력 보장
                if (stats.getStat("PHYSICAL_DAMAGE") == 0)
                    stats.setStat("PHYSICAL_DAMAGE", 1);
            } else {
                return null; // 프로필 로딩 미완료 시
            }
        } else {
            // 몬스터의 경우 ECS 레지스트리에서 스탯 컴포넌트를 조회합니다.
            if (entityRegistry.hasComponent(entity.getUniqueId(), EntityStatData.class)) {
                return entityRegistry.getComponent(entity.getUniqueId(), EntityStatData.class)
                        .orElse(getDefaultMonsterStats());
            } else {
                return getDefaultMonsterStats();
            }
        }

        return stats;
    }

    /**
     * 스탯 정보가 없는 몬스터를 위한 기본 스탯을 반환합니다.
     */
    private EntityStatData getDefaultMonsterStats() {
        EntityStatData stats = new EntityStatData();
        stats.setStat("DEFENSE", 0);
        stats.setStat("PHYSICAL_DAMAGE", 5);
        return stats;
    }
}
