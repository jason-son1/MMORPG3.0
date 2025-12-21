package com.antigravity.rpg.core.ecs;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.api.service.Service;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * ECS 시스템들을 통합 관리하고 주기적으로 실행하는 매니저입니다.
 * BukkitRunnable을 상속받아 메인 게임 루프 역할을 수행합니다.
 */
@Singleton
public class SystemManager extends BukkitRunnable implements Service {

    private final AntiGravityPlugin plugin;
    private final Injector injector;
    private final List<System> systems = new ArrayList<>();

    // 마지막 업데이트 시간을 기록하여 델타 타임(초 단위) 계산
    private long lastTickTime;

    @Inject
    public SystemManager(AntiGravityPlugin plugin, Injector injector) {
        this.plugin = plugin;
        this.injector = injector;
    }

    @Override
    public void onEnable() {
        this.lastTickTime = java.lang.System.currentTimeMillis();
        // 1틱(50ms)마다 실행
        this.runTaskTimer(plugin, 0L, 1L);
        plugin.getLogger().info("[SystemManager] ECS 게임 루프가 시작되었습니다.");
    }

    @Override
    public void onDisable() {
        this.cancel();
        plugin.getLogger().info("[SystemManager] ECS 게임 루프가 종료되었습니다.");
    }

    @Override
    public String getName() {
        return "SystemManager";
    }

    /**
     * 시스템을 등록합니다.
     * 
     * @param systemClass 등록할 시스템 클래스
     */
    public void registerSystem(Class<? extends System> systemClass) {
        System system = injector.getInstance(systemClass);
        systems.add(system);
        plugin.getLogger().info("[SystemManager] 시스템 등록됨: " + system.getClass().getSimpleName());
    }

    @Override
    public void run() {
        long currentTime = java.lang.System.currentTimeMillis();
        // 델타 타임 계산 (초 단위)
        double deltaTime = (currentTime - lastTickTime) / 1000.0;
        lastTickTime = currentTime;

        // 모든 등록된 시스템 업데이트
        for (System system : systems) {
            try {
                system.tick(deltaTime);
            } catch (Exception e) {
                plugin.getLogger().severe("[SystemManager] 시스템 오류 발생: " + system.getClass().getSimpleName());
                e.printStackTrace();
            }
        }
    }

    public void registerSystemInstance(System system) {
        systems.add(system);
        plugin.getLogger().info("[SystemManager] 시스템 인스턴스 등록됨: " + system.getClass().getSimpleName());
    }
}
