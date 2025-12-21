package com.antigravity.rpg.core.ecs.system;

import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.feature.player.PlayerProfileService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 스킬 및 아이템의 쿨타임을 관리하고 감소시키는 시스템입니다.
 */
@Singleton
public class CooldownSystem implements System {

    private final PlayerProfileService playerProfileService;

    @Inject
    public CooldownSystem(PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }

    @Override
    public void tick(double deltaTime) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 플레이어 데이터 접근 (비동기 데이터지만 쿨타임은 메모리상에서 빠르게 처리 필요)
            // 구현 편의상 여기서는 생략하고, 추후 SkillCastService 등에서
            // 쿨타임을 Map<String, Long> (만료 시간 타임스탬프) 방식으로 관리한다면
            // 굳이 매 틱마다 감소시킬 필요 없이 사용 시점 에 체크하면 됩니다.

            // 하지만 "남은 쿨타임 표시" 등을 위해 지속적인 업데이트가 필요하다면 여기서 처리합니다.
            // 본 프로젝트에서는 타임스탬프 방식(System.currentTimeMillis() < endTime)을 권장하므로
            // 이 시스템은 활성화된 버프나 지속 효과 시간 감소에 더 집중할 수 있습니다.
        }
    }

    @Override
    public boolean isAsync() {
        return true; // 별도의 Bukkit API 호출이 없다면 비동기 가능
    }
}
