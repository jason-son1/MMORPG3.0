package com.antigravity.rpg.core.ecs.system;

import com.antigravity.rpg.core.ecs.System;
import com.antigravity.rpg.core.ecs.System;
import com.google.inject.Singleton;
import org.bukkit.Bukkit;

/**
 * 스킬 및 아이템의 쿨타임을 관리하고 감소시키는 시스템입니다.
 */
@Singleton
public class CooldownSystem implements System {
    // PlayerProfileService removed as it was unused in the skeleton implementation.

    public CooldownSystem() {
    }

    @Override
    public void tick(double deltaTime) {
        // Implementation omitted for now. Used for timestamp-based checks elsewhere.
    }

    @Override
    public boolean isAsync() {
        return true; // 별도의 Bukkit API 호출이 없다면 비동기 가능
    }
}
