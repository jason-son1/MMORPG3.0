package com.antigravity.rpg.feature.player;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.StatRegistry;
import com.antigravity.rpg.data.repository.AbstractCachedRepository;
import com.antigravity.rpg.data.service.DatabaseService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * 플레이어의 데이터(PlayerData)를 관리하는 서비스입니다.
 * 게임 접속/종료 시 데이터를 로드/저장하며, 캐싱을 통해 성능을 최적화합니다.
 */
@Singleton
public class PlayerProfileService extends AbstractCachedRepository<UUID, PlayerData> implements Service, Listener {

    private final DatabaseService databaseService;
    private final JavaPlugin plugin;
    private final StatRegistry statRegistry;

    @Inject
    public PlayerProfileService(DatabaseService databaseService, JavaPlugin plugin, StatRegistry statRegistry) {
        // 가상 스레드 또는 캐시드 스레드 풀 사용 (비동기 로딩)
        super(Executors.newCachedThreadPool());
        this.databaseService = databaseService;
        this.plugin = plugin;
        this.statRegistry = statRegistry;
    }

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[PlayerProfileService] Listeners registered. (플레이어 리스너 등록됨)");

        // 이미 접속 중인 플레이어 처리 (플러그인 리로드 시)
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            loadProfileInternal(p.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        // 모든 캐시된 프로필 저장
        // 참고: 정확한 종료 처리를 위해 접속 중인 플레이어 데이터를 강제 저장
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            // 비동기로 저장 요청 (실제 환경에서는 완료 대기 필요)
            save(p.getUniqueId(), find(p.getUniqueId()).getNow(null));
        }
    }

    @Override
    public String getName() {
        return "PlayerProfileService";
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadProfileInternal(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // 플레이어 종료 시 데이터 저장 후 캐시에서 제거
        // 메모리 관리를 위해 오프라인 플레이어는 캐시에 남기지 않음
        UUID uuid = event.getPlayer().getUniqueId();
        find(uuid).thenAccept(data -> {
            if (data != null) {
                save(uuid, data).thenRun(() -> delete(uuid)); // 저장 후 캐시 삭제
            }
        });
    }

    private void loadProfileInternal(UUID uuid) {
        find(uuid); // 캐시에 없으면 loadFromDb 호출
    }

    @Override
    protected PlayerData loadFromDb(UUID key) throws Exception {
        // DB 로드 로직 (구현 필요)
        // 실제 구현 시 DatabaseService를 사용하여 SQL 쿼리 실행
        return new PlayerData(key);
    }

    @Override
    protected void saveToDb(UUID key, PlayerData value) throws Exception {
        // DB 저장 로직 (구현 필요)
    }

    @Override
    protected void deleteFromDb(UUID key) throws Exception {
        // DB 삭제 로직
    }
}
