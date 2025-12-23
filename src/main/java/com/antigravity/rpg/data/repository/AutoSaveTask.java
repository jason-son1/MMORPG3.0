package com.antigravity.rpg.data.repository;

import com.antigravity.rpg.feature.player.PlayerData;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 변경된 플래그(Dirty Flag)가 설정된 플레이어 데이터만 선별하여 저장하는 자동 저장 태스크입니다.
 */
public class AutoSaveTask extends BukkitRunnable {

    private final PlayerProfileService playerProfileService;

    public AutoSaveTask(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }

    @Override
    public void run() {
        // 모든 캐시된 데이터를 순회하며 변경된 데이터만 선별
        playerProfileService.getAllCached().forEach((uuid, data) -> {
            if (data != null && data.isDirty() && data.isLoaded()) {
                // 비동기 저장 요청
                playerProfileService.save(uuid, data).thenRun(() -> {
                    // 저장 성공 시 dirty 플래그 해제
                    data.clearDirty();
                });
            }
        });
    }
}
