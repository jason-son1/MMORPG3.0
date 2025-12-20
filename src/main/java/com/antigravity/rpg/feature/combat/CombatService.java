package com.antigravity.rpg.feature.combat;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.*;
import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * CombatService는 전투 시스템의 핵심 진입점입니다.
 * Bukkit의 데미지 이벤트를 가로채서 자체 데미지 파이프라인(DamageProcessor)으로 전달합니다.
 */
@Singleton
public class CombatService implements Service, Listener {

    private final JavaPlugin plugin;
    private final DamageProcessor damageProcessor;
    private final PlayerProfileService playerProfileService;

    @Inject
    public CombatService(JavaPlugin plugin, DamageProcessor damageProcessor,
            PlayerProfileService playerProfileService) {
        this.plugin = plugin;
        this.damageProcessor = damageProcessor;
        this.playerProfileService = playerProfileService;
    }

    @Override
    public void onEnable() {
        // 이벤트 리스너 등록
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[CombatService] Damage pipeline active. (전투 시스템 활성화됨)");
    }

    @Override
    public void onDisable() {
        // 필요시 리스너 해제 (대부분 플러그인 비활성화 시 자동 처리됨)
    }

    @Override
    public String getName() {
        return "CombatService";
    }

    /**
     * 엔티티가 데미지를 입을 때 호출됩니다.
     * 여기서 바닐라 데미지 계산 대신 커스텀 공식을 적용합니다.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity))
            return;

        LivingEntity victim = (LivingEntity) event.getEntity();
        LivingEntity attacker = null;

        // 공격자 판별 (직접 공격 또는 발사체)
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

        // 스탯 가져오기 (동기 처리 필요 - 이벤트가 메인 스레드에서 발생함)
        // 플레이어의 경우 PlayerProfileService 캐시에서 조회
        EntityStatData attackerStats = getStats(attacker);
        EntityStatData victimStats = getStats(victim);

        if (attackerStats == null || victimStats == null) {
            // 데이터가 없는 경우 (로딩 중 등), 바닐라 데미지 적용 또는 취소
            return;
        }

        // 데미지 컨텍스트 생성 (DamageContext)
        DamageContext context = new DamageContext(attacker, victim, attackerStats, victimStats, event.getDamage());

        // 태그 추가 (예: 발사체는 PHYSICAL, 포션은 MAGIC 등 설정 가능)
        context.addTag(DamageTag.PHYSICAL); // 기본적으로 근접 물리 공격으로 가정

        // 데미지 프로세서 실행 (핵심 데미지 공식 적용)
        damageProcessor.process(context);

        // 최종 데미지 적용
        event.setDamage(context.getFinalDamage());

        // TODO: 시각 효과 및 피드백 추가
    }

    private EntityStatData getStats(LivingEntity entity) {
        EntityStatData stats = new EntityStatData();

        if (entity instanceof Player) {
            Player p = (Player) entity;
            // 캐시에서 동기적으로 가져옴 (Get from cache synchronously)
            CompletableFuture<PlayerData> future = playerProfileService.find(p.getUniqueId());
            PlayerData data = future.getNow(null);

            if (data != null) {
                // PlayerData의 스탯을 EntityStatData로 복사
                data.getSavedStats().forEach(stats::setStat);

                // 기본값 보장
                if (stats.getStat(StatRegistry.PHYSICAL_DAMAGE) == 0)
                    stats.setStat(StatRegistry.PHYSICAL_DAMAGE, 1);
            } else {
                return null; // 플레이어 데이터가 아직 로드되지 않음
            }
        } else {
            // 몬스터: 임시 기본 스탯 제공
            // Todo: MobManager와 연동 필요
            stats.setStat(StatRegistry.DEFENSE, 0);
            stats.setStat(StatRegistry.PHYSICAL_DAMAGE, 5); // 기본 몬스터 데미지
        }

        return stats;
    }
}
