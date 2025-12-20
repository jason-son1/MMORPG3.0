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
        plugin.getLogger().info("Connecting to Database...");

        // In a real scenario, load these from config.yml
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/minecraft_rpg");
        config.setUsername("root");
        config.setPassword("sks74537453!");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);
        config.setPoolName("AntiGravityRPG-Pool");

        try {
            this.dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Database connected successfully.");
            initDatabase();
        } catch (Exception e) {
            plugin.getLogger().severe("Could not connect to database!");
            e.printStackTrace();
        }
    }

    private void initDatabase() throws SQLException {
        try (Connection conn = dataSource.getConnection();
                java.sql.Statement stmt = conn.createStatement()) {

            // Player Data
            stmt.execute("CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "class_id VARCHAR(32), " +
                    "level INT DEFAULT 1, " +
                    "experience DOUBLE DEFAULT 0, " +
                    "current_mana DOUBLE, " +
                    "current_stamina DOUBLE, " +
                    "last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // Player Skills
            stmt.execute("CREATE TABLE IF NOT EXISTS player_skills (" +
                    "uuid VARCHAR(36), " +
                    "skill_id VARCHAR(64), " +
                    "level INT DEFAULT 0, " +
                    "cooldown_end BIGINT, " +
                    "PRIMARY KEY (uuid, skill_id), " +
                    "FOREIGN KEY (uuid) REFERENCES player_data(uuid) ON DELETE CASCADE" +
                    ");");

            // Player Professions
            stmt.execute("CREATE TABLE IF NOT EXISTS player_professions (" +
                    "uuid VARCHAR(36), " +
                    "profession_id VARCHAR(32), " +
                    "level INT DEFAULT 1, " +
                    "experience DOUBLE DEFAULT 0, " +
                    "PRIMARY KEY (uuid, profession_id), " +
                    "FOREIGN KEY (uuid) REFERENCES player_data(uuid) ON DELETE CASCADE" +
                    ");");

            plugin.getLogger().info("Database schema initialized.");
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
