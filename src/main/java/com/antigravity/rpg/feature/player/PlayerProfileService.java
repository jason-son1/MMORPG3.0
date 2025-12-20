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
 * 플레이어의 데이터(PlayerData)를 관리하는 서비스입니다.
 * JSON 직렬화를 통해 DB 유연성을 확보했습니다.
 */
@Singleton
public class PlayerProfileService extends AbstractCachedRepository<UUID, PlayerData> implements Service, Listener {

    private final DatabaseService databaseService;
    private final JavaPlugin plugin;
    private final StatRegistry statRegistry;
    private final Gson gson = new Gson();

    @Inject
    public PlayerProfileService(DatabaseService databaseService, JavaPlugin plugin, StatRegistry statRegistry) {
        super(Executors.newCachedThreadPool());
        this.databaseService = databaseService;
        this.plugin = plugin;
        this.statRegistry = statRegistry;
    }

    @Override
    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[PlayerProfileService] Listeners registered. (플레이어 리스너 등록됨)");

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            loadProfileInternal(p.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
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
        loadProfileInternal(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        find(uuid).thenAccept(data -> {
            if (data != null) {
                save(uuid, data).thenRun(() -> delete(uuid));
            }
        });
    }

    private void loadProfileInternal(UUID uuid) {
        find(uuid);
    }

    @Override
    protected PlayerData loadFromDb(UUID key) throws Exception {
        PlayerData data = null;

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

        if (data == null) {
            data = new PlayerData(key); // New Profile
        }

        data.setLoaded(true);
        return data;
    }

    @Override
    protected void saveToDb(UUID key, PlayerData value) throws Exception {
        String upsertSql = "INSERT INTO player_data_v2 (uuid, json_data) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE json_data = VALUES(json_data)";

        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(upsertSql)) {

            // Serialize to JSON
            Map<String, Object> map = value.toMap();
            String json = gson.toJson(map);

            stmt.setString(1, key.toString());
            stmt.setString(2, json);
            stmt.executeUpdate();
        }
    }

    @Override
    protected void deleteFromDb(UUID key) throws Exception {
        String sql = "DELETE FROM player_data_v2 WHERE uuid = ?";
        try (Connection conn = databaseService.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key.toString());
            stmt.executeUpdate();
        }
    }
}
