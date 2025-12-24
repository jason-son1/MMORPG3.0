package com.antigravity.rpg.feature.player;

import com.antigravity.rpg.AntiGravityPlugin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 전역 플레이어 자원 회복을 관리하는 태스크입니다.
 * 1초(20틱)마다 실행됩니다.
 */
@Singleton
public class ResourceRegenTask extends BukkitRunnable {

    private final PlayerProfileService profileService;
    private final com.antigravity.rpg.core.formula.ExpressionEngine expressionEngine;

    @Inject
    public ResourceRegenTask(AntiGravityPlugin plugin, PlayerProfileService profileService,
            com.antigravity.rpg.core.formula.ExpressionEngine expressionEngine) {
        this.profileService = profileService;
        this.expressionEngine = expressionEngine;
        this.runTaskTimer(plugin, 20L, 20L); // 1초 간격 실행
    }

    @Override
    public void run() {
        // 현재 캐시에 로드된(접속 중인) 모든 플레이어 데이터에 대해 처리
        for (PlayerData data : profileService.getAllCached().values()) {
            processRegen(data);
        }
    }

    private void processRegen(PlayerData data) {
        ResourcePool pool = data.getResources();
        pool.updateCombatState(); // 전투 상태 갱신

        String classId = data.getClassId();
        if (classId == null || classId.isEmpty())
            return;

        PlayerData.getClassRegistry().getClass(classId).ifPresent(def -> {
            com.antigravity.rpg.feature.classes.component.ResourceSettings settings = def.getResourceSettings();
            if (settings != null) {
                com.antigravity.rpg.feature.classes.component.ResourceSettings.ResourceType type = settings.getType();
                com.antigravity.rpg.feature.classes.component.ResourceSettings.RegenMode mode = settings.getRegenMode();

                String rTypeStr = type.name();
                double maxResource = data.getStat("MAX_" + rTypeStr, settings.getMax());

                // 1. 재생 모드에 따른 처리
                if (mode == com.antigravity.rpg.feature.classes.component.ResourceSettings.RegenMode.PASSIVE) {
                    // 공식 기반 회복 시도
                    String formulaKey = rTypeStr.toLowerCase() + "-regen";
                    double regen = expressionEngine.evaluate(formulaKey, data);

                    // 공식이 없으면(0.0) 기본 설정값 사용 (하위 호환성)
                    if (regen == 0.0) {
                        regen = settings.getRegenAmount();
                    }

                    pool.recover(rTypeStr, regen, maxResource);
                } else if (mode == com.antigravity.rpg.feature.classes.component.ResourceSettings.RegenMode.DECAY) {
                    // 비전투 시 감소
                    if (!pool.isInCombat()) {
                        double decay = settings.getDecayAmount();
                        pool.decay(rTypeStr, decay);
                    }
                }
            }

            // 2. 공통: 스태미나 회복
            // 스태미나도 공식 사용 시도
            double maxStamina = data.getStat("MAX_STAMINA", 100.0);
            double stamRegen = expressionEngine.evaluate("stamina-regen", data);

            // 공식 없으면 기본값
            if (stamRegen == 0.0) {
                stamRegen = data.getStat("STAMINA_REGEN", 10.0);
            }

            pool.recover("STAMINA", stamRegen, maxStamina);

            data.markDirty();
        });
    }
}
