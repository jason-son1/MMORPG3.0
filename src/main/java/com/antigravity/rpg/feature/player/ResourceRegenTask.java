package com.antigravity.rpg.feature.player;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.feature.classes.ClassDefinition;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 전역 플레이어 자원 회복을 관리하는 태스크입니다.
 * 1초(20틱)마다 실행됩니다.
 */
@Singleton
public class ResourceRegenTask extends BukkitRunnable {

    private final PlayerProfileService profileService;

    @Inject
    public ResourceRegenTask(AntiGravityPlugin plugin, PlayerProfileService profileService) {
        this.profileService = profileService;
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
        String classId = data.getClassId();

        if (classId == null || classId.isEmpty())
            return;

        PlayerData.getClassRegistry().getClass(classId).ifPresent(def -> {
            ClassDefinition.ResourceType type = def.getAttributes().getResourceType();

            // 1. 마나 회복 (MANA)
            if (type == ClassDefinition.ResourceType.MANA) {
                double maxMana = data.getStat("MAX_MANA", 100.0);
                double regen = data.getStat("MANA_REGEN", 5.0);
                pool.recover("MANA", regen, maxMana);
            }

            // 2. 기력 회복 (ENERGY)
            if (type == ClassDefinition.ResourceType.ENERGY) {
                double maxEnergy = data.getStat("MAX_ENERGY", 100.0);
                double regen = data.getStat("ENERGY_REGEN", 10.0);
                pool.recover("ENERGY", regen, maxEnergy);
            }

            // 3. 분노 감소/회복 (RAGE) - 일반적으로 비전투 시 감소
            if (type == ClassDefinition.ResourceType.RAGE) {
                if (!pool.isInCombat()) {
                    pool.consume("RAGE", 2.0); // 비전투 시 분노 감소
                }
            }

            // 공통: 스태미나 회복
            double maxStam = data.getStat("MAX_STAMINA", 100.0);
            double stamRegen = data.getStat("STAMINA_REGEN", 10.0);
            pool.recover("STAMINA", stamRegen, maxStam);

            // 데이터 변경됨을 표시
            data.markDirty();
        });
    }
}
