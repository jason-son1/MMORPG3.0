package com.antigravity.rpg.core.ecs.system;

import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.core.ecs.component.EffectComponent;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.UUID;

/**
 * 주기적으로 실행되어 EffectComponent 내의 효과 지속시간을 감소시키고
 * 만료된 효과를 제거하는 ECS 시스템입니다.
 */
@Singleton
public class EffectSystem implements System {

    private final PlayerProfileService playerProfileService;

    @Inject
    public EffectSystem(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void tick(double deltaTime) {
        // 모든 온라인 플레이어의 EffectComponent를 순회 (EntityRegistry 구조에 따라 조정 필요)
        // 현재는 PlayerProfileService를 통해 접근한다고 가정
        for (Player player : Bukkit.getOnlinePlayers()) {
            processPlayerEffects(player.getUniqueId(), deltaTime);
        }
    }

    private void processPlayerEffects(UUID playerId, double deltaTime) {
        // PlayerData에서 Component 가져오기
        var future = playerProfileService.find(playerId);
        // 온라인 플레이어이므로 데이터가 캐시에 있어야 함
        var playerData = future.getNow(null);

        if (playerData == null) {
            return;
        }

        EffectComponent effectComponent = playerData.getComponent(EffectComponent.class);
        if (effectComponent == null)
            return;

        Iterator<EffectComponent.ActiveEffect> iterator = effectComponent.getActiveEffects().iterator();
        while (iterator.hasNext()) {
            EffectComponent.ActiveEffect effect = iterator.next();
            effect.duration -= deltaTime;

            // 만료 처리
            if (effect.duration <= 0) {
                // 효과 종료 시 로직 (예: 스탯 보너스 제거 등)이 필요하면 여기서 처리
                iterator.remove();
            }
        }
    }
}
