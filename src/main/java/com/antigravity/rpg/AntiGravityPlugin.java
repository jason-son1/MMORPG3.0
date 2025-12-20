package com.antigravity.rpg;

import com.antigravity.rpg.core.di.RpgCoreModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 플러그인의 메인 진입점(Main Entry Point)입니다.
 * Guice를 이용한 의존성 주입(DI) 컨테이너를 초기화하고, 핵심 서비스들을 순차적으로 실행합니다.
 */
public class AntiGravityPlugin extends JavaPlugin {

    private Injector injector;

    @Override
    public void onEnable() {
        getLogger().info("Initializing AntiGravityRPG Core...");

        // 1. 의존성 주입 초기화 (Initialize Dependency Injection)
        // [중요] config.yml이 없으면 기본 파일을 복사하여 저장합니다.
        saveDefaultConfig();

        try {
            this.injector = Guice.createInjector(new RpgCoreModule(this));

            // Initialize PlayerData static references
            com.antigravity.rpg.feature.player.PlayerData.initialize(
                    injector.getInstance(com.antigravity.rpg.core.engine.StatCalculator.class),
                    injector.getInstance(com.antigravity.rpg.feature.classes.ClassRegistry.class));

        } catch (Exception e) {
            getLogger().severe("Failed to initialize Guice Injector! (DI 컨테이너 초기화 실패)");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. 서비스 시작 (Start Services)
        // ServiceManager를 통해 등록된 서비스들의 생명주기(onEnable)를 관리합니다.
        try {
            com.antigravity.rpg.core.ServiceManager serviceManager = injector
                    .getInstance(com.antigravity.rpg.core.ServiceManager.class);

            // 핵심 인프라 서비스 (Core Infrastructure)
            serviceManager.startService(injector.getInstance(com.antigravity.rpg.data.service.DatabaseService.class));
            serviceManager.startService(injector.getInstance(com.antigravity.rpg.core.network.NetworkService.class));

            // 기능별 서비스 (Features)
            serviceManager.startService(injector.getInstance(com.antigravity.rpg.core.script.LuaScriptService.class));
            serviceManager
                    .startService(injector.getInstance(com.antigravity.rpg.core.engine.trigger.TriggerService.class)); // NEW

            // 플레이어 프로필 관리 서비스
            serviceManager
                    .startService(injector.getInstance(com.antigravity.rpg.feature.player.PlayerProfileService.class));

            // 아이템 관리 서비스
            serviceManager.startService(injector.getInstance(com.antigravity.rpg.feature.item.ItemService.class));

            // 전투 시스템 서비스
            serviceManager.startService(injector.getInstance(com.antigravity.rpg.feature.combat.CombatService.class));

            // 스킬 시전 서비스
            serviceManager.startService(injector.getInstance(com.antigravity.rpg.feature.skill.SkillCastService.class));

            // 데이터 Import/Export 서비스 및 관리자 명령어
            if (getCommand("rpgadmin") != null) {
                getCommand("rpgadmin").setExecutor(new com.antigravity.rpg.command.admin.DataSyncCommand(
                        injector.getInstance(com.antigravity.rpg.data.service.DataImportExportService.class),
                        injector.getInstance(com.antigravity.rpg.core.script.LuaScriptService.class),
                        injector.getInstance(com.antigravity.rpg.feature.skill.SkillManager.class),
                        injector.getInstance(com.antigravity.rpg.feature.classes.ClassRegistry.class),
                        injector.getInstance(com.antigravity.rpg.core.engine.StatRegistry.class)));
            }

        } catch (Exception e) {
            getLogger().severe("Failed to start services! (서비스 시작 중 오류 발생)");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("AntiGravityRPG Core enabled successfully. (플러그인 활성화 완료)");
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down AntiGravityRPG...");
        if (injector != null) {
            try {
                com.antigravity.rpg.core.ServiceManager serviceManager = injector
                        .getInstance(com.antigravity.rpg.core.ServiceManager.class);
                // 모든 서비스를 역순으로 종료 (Graceful Shutdown)
                serviceManager.shutdownAll();
            } catch (Exception e) {
                getLogger().severe("Error during shutdown! (종료 중 오류 발생)");
                e.printStackTrace();
            }
        }
    }

    public Injector getInjector() {
        return injector;
    }
}
