package com.antigravity.rpg.data.sql;

import com.antigravity.rpg.AntiGravityPlugin;
import com.antigravity.rpg.data.service.DatabaseService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.sql.Connection;
import java.sql.SQLException;

@Singleton
public class HikariDatabaseService implements DatabaseService {

    private final AntiGravityPlugin plugin;
    private HikariDataSource dataSource;

    @Inject
    public HikariDatabaseService(AntiGravityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        plugin.getLogger().info("데이터베이스 연결 시도 중...");

        // config.yml에서 설정 로드
        HikariConfig config = new HikariConfig();
        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String dbName = plugin.getConfig().getString("database.name", "minecraft_rpg");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName);
        config.setUsername(username);
        config.setPassword(password);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);
        config.setPoolName("AntiGravityRPG-Pool");

        try {
            this.dataSource = new HikariDataSource(config);
            plugin.getLogger().info("데이터베이스 연결 성공.");
            initDatabase();
        } catch (Exception e) {
            plugin.getLogger().severe("데이터베이스 연결 실패!");
            e.printStackTrace();
        }
    }

    private void initDatabase() throws SQLException {
        try (Connection conn = dataSource.getConnection();
                java.sql.Statement stmt = conn.createStatement()) {

            // 새로운 JSON 기반 플레이어 데이터 테이블 (player_data_v2)
            stmt.execute("CREATE TABLE IF NOT EXISTS player_data_v2 (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "json_data LONGTEXT, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ");");

            // 구형 테이블(player_data, player_skills 등) 생성 코드 제거됨 (Cleanup)

            plugin.getLogger().info("데이터베이스 스키마 초기화 완료.");
        }
    }

    @Override
    public void onDisable() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public String getName() {
        return "HikariDatabaseService";
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized!");
        }
        return dataSource.getConnection();
    }
}
