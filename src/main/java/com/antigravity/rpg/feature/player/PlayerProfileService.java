package com.antigravity.rpg.feature.player;

import com.antigravity.rpg.api.service.Service;
import com.antigravity.rpg.core.engine.StatRegistry;
import com.antigravity.rpg.data.repository.AbstractCachedRepository;
import com.antigravity.rpg.data.service.DatabaseService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * 플레이어의 프로필 데이터(PlayerData)를 관리하는 서비스입니다.
 * 데이터베이스 저장, 캐싱, 접속 시 데이터 로드 및 종료 시 저장을 담당합니다.
 */
@Singleton
public class PlayerProfileService extends AbstractCachedRepository<UUID, PlayerData> implements Service, Listener {

    private final DatabaseService databaseService;
    private final JavaPlugin plugin;
    private final StatRegistry statRegistry;
    private final Gson gson = new Gson();

    @Inject
    public PlayerProfileService(DatabaseService databaseService, JavaPlugin plugin, StatRegistry statRegistry) {
        // 캐시 작업을 위한 전용 스레드 풀 생성
        super(Executors.newCachedThreadPool());
        this.databaseService = databaseService;
        this.plugin = plugin;
        this.statRegistry = statRegistry;
    }

    @Override
    public void onEnable() {
        // 이벤트 리스너 등록
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[PlayerProfileService] 플레이어 이벤트 리스너가 등록되었습니다.");

        // 서버가 켜져 있는 도중에 로드된 경우 현재 접속자 데이터 강제 로드
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            loadProfileInternal(p.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        // 플러그인 종료 시 데이터 유실 방지를 위해 모든 데이터를 저장합니다.
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            save(p.getUniqueId(), find(p.getUniqueId()).getNow(null));
        }
    }

    @Override
    public String getName() {
        return "PlayerProfileService";
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // 접속 시 데이터 비동기 로드 시작
        loadProfileInternal(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // 퇴장 시 데이터 저장 후 캐시에서 해제
        find(uuid).thenAccept(data -> {
            if (data != null) {
                save(uuid, data).thenRun(() -> unregister(uuid));
            }
        });
    }

    private void loadProfileInternal(UUID uuid) {
        find(uuid);
    }

    /**
     * DB에서 JSON 형태의 플레이어 데이터를 불러와 객체로 변환합니다.
     */
    @Override
    protected PlayerData loadFromDb(UUID key) throws Exception {
        PlayerData data = null;

        // 세션 락 체크 (단순 구현: PlayerData가 이미 캐시에 있으면 로드 방지 가능하지만,
        // 여기서는 DB의 다른 필드나 Redis/SQL 등을 통한 실제 락 로직이 들어갈 자리입니다.)
        if (isSessionLocked(key)) {
            throw new IllegalStateException("Player session is already locked on another server/session.");
        }

        String selectSql = "SELECT json_data FROM player_data_v2 WHERE uuid = ?";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, key.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("json_data");
                    if (json != null && !json.isEmpty()) {
                        Type type = new TypeToken<Map<String, Object>>() {
                        }.getType();
                        Map<String, Object> map = gson.fromJson(json, type);
                        data = PlayerData.fromMap(key, map);
                    }
                }
            }
        }

        // 신규 플레이어인 경우 기본 데이터 생성
        if (data == null) {
            data = new PlayerData(key);
        }

        data.setLoaded(true);
        return data;
    }

    /**
     * PlayerData 객체를 JSON으로 직렬화하여 DB에 저장합니다 (UPSERT).
     */
    @Override
    protected void saveToDb(UUID key, PlayerData value) throws Exception {
        String upsertSql = "INSERT INTO player_data_v2 (uuid, json_data) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE json_data = VALUES(json_data)";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(upsertSql)) {

            // JSON 직렬화
            Map<String, Object> map = value.toMap();
            String json = gson.toJson(map);

            stmt.setString(1, key.toString());
            stmt.setString(2, json);
            stmt.executeUpdate();
        }
    }

    /**
     * DB에서 해당 플레이어 데이터를 영구히 제거합니다.
     */
    @Override
    protected void deleteFromDb(UUID key) throws Exception {
        String sql = "DELETE FROM player_data_v2 WHERE uuid = ?";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key.toString());
            stmt.executeUpdate();
        }
    }

    /**
     * 특정 플레이어의 세션이 다른 곳에서 잠겨 있는지 확인합니다.
     */
    private boolean isSessionLocked(UUID uuid) {
        // 실제 구현에서는 Redis나 전역 상태 DB를 조회해야 함
        // 여기서는 같은 서버 내 중복 캐시 존재 여부만 체크하는 더미 구현
        return false;
    }

    public PlayerData getProfileSync(UUID uuid) {
        return find(uuid).getNow(null);
    }

    /**
     * 특정 플레이어의 ECS 컴포넌트를 가져옵니다.
     */
    public <T> T getComponent(UUID uuid, Class<T> componentClass) {
        PlayerData data = getProfileSync(uuid);
        if (data == null) {
            return null;
        }
        return data.getComponent(componentClass);
    }
}
