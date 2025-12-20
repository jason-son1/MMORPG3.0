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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
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
        PlayerData data = new PlayerData(key);

        try (Connection conn = databaseService.getConnection()) {
            // 1. Load basic data (player_data)
            String sqlData = "SELECT class_id, level, experience, current_mana, current_stamina FROM player_data WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlData)) {
                stmt.setString(1, key.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        data.setClassId(rs.getString("class_id"));
                        data.setLevel(rs.getInt("level"));
                        data.setExperience(rs.getDouble("experience"));
                        data.getResources().setCurrentMana(rs.getDouble("current_mana"));
                        data.getResources().setCurrentStamina(rs.getDouble("current_stamina"));
                    }
                }
            }

            // 2. Load skills (player_skills)
            String sqlSkills = "SELECT skill_id, level, cooldown_end FROM player_skills WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlSkills)) {
                stmt.setString(1, key.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String skillId = rs.getString("skill_id");
                        int level = rs.getInt("level");
                        long cd = rs.getLong("cooldown_end");

                        data.getSkillLevels().put(skillId, level);
                        if (cd > 0) {
                            data.getSkillCooldowns().put(skillId, cd);
                        }
                    }
                }
            }

            // 3. Load professions (player_professions)
            String sqlProfessions = "SELECT profession_id, level FROM player_professions WHERE uuid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlProfessions)) {
                stmt.setString(1, key.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String profId = rs.getString("profession_id");
                        int level = rs.getInt("level");
                        data.getProfessions().put(profId, level);
                    }
                }
            }
        }

        data.setLoaded(true);
        return data;
    }

    @Override
    protected void saveToDb(UUID key, PlayerData value) throws Exception {
        try (Connection conn = databaseService.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Start Transaction

            try {
                // 1. Save basic data (player_data)
                String sqlData = "INSERT INTO player_data (uuid, class_id, level, experience, current_mana, current_stamina, last_login) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "class_id = VALUES(class_id), " +
                        "level = VALUES(level), " +
                        "experience = VALUES(experience), " +
                        "current_mana = VALUES(current_mana), " +
                        "current_stamina = VALUES(current_stamina), " +
                        "last_login = CURRENT_TIMESTAMP";

                try (PreparedStatement stmt = conn.prepareStatement(sqlData)) {
                    stmt.setString(1, key.toString());
                    stmt.setString(2, value.getClassId());
                    stmt.setInt(3, value.getLevel());
                    stmt.setDouble(4, value.getExperience());
                    stmt.setDouble(5, value.getResources().getCurrentMana());
                    stmt.setDouble(6, value.getResources().getCurrentStamina());
                    stmt.executeUpdate();
                }

                // 2. Save skills (player_skills)
                if (!value.getSkillLevels().isEmpty()) {
                    String sqlSkills = "INSERT INTO player_skills (uuid, skill_id, level, cooldown_end) " +
                            "VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "level = VALUES(level), " +
                            "cooldown_end = VALUES(cooldown_end)";

                    try (PreparedStatement stmt = conn.prepareStatement(sqlSkills)) {
                        for (Map.Entry<String, Integer> entry : value.getSkillLevels().entrySet()) {
                            String skillId = entry.getKey();
                            int level = entry.getValue();
                            long cd = value.getSkillCooldowns().getOrDefault(skillId, 0L);

                            stmt.setString(1, key.toString());
                            stmt.setString(2, skillId);
                            stmt.setInt(3, level);
                            stmt.setLong(4, cd);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                // 3. Save professions (player_professions)
                if (!value.getProfessions().isEmpty()) {
                    String sqlProf = "INSERT INTO player_professions (uuid, profession_id, level, experience) " +
                            "VALUES (?, ?, ?, 0) " +
                            "ON DUPLICATE KEY UPDATE " +
                            "level = VALUES(level)";

                    try (PreparedStatement stmt = conn.prepareStatement(sqlProf)) {
                        for (Map.Entry<String, Integer> entry : value.getProfessions().entrySet()) {
                            stmt.setString(1, key.toString());
                            stmt.setString(2, entry.getKey());
                            stmt.setInt(3, entry.getValue());
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    @Override
    protected void deleteFromDb(UUID key) throws Exception {
        // DB 삭제 로직
    }
}
