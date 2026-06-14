package dev.velmax.velkoth.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.velmax.velkoth.config.PluginConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages database connections and queries using HikariCP.
 * Supports SQLite (default) and MySQL.
 */
public final class DatabaseManager {

    private final JavaPlugin plugin;
    private HikariDataSource dataSource;
    private final boolean isMySQL;

    public DatabaseManager(JavaPlugin plugin, PluginConfig.DatabaseConfig config) {
        this.plugin = plugin;
        this.isMySQL = config.getType().equalsIgnoreCase("MYSQL");
        setupDataSource(config);
        createTables();
    }

    private void setupDataSource(PluginConfig.DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();

        if (config.getType().equalsIgnoreCase("MYSQL")) {
            hikariConfig.setJdbcUrl(
                    "jdbc:mysql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
        }

        hikariConfig.setMaximumPoolSize(config.getPoolSize());
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setMaxLifetime(600000);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setPoolName("VelKoth-Pool");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection();
                var stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS koth_stats (
                            uuid VARCHAR(36) PRIMARY KEY,
                            player_name VARCHAR(16) NOT NULL,
                            total_wins INT DEFAULT 0,
                            last_win_timestamp BIGINT DEFAULT 0
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS koth_metadata (
                            meta_key VARCHAR(64) PRIMARY KEY,
                            meta_value VARCHAR(256) NOT NULL
                        )
                    """);
            
            String logTableQuery = isMySQL ? """
                        CREATE TABLE IF NOT EXISTS koth_win_log (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            uuid VARCHAR(36) NOT NULL,
                            arena_id VARCHAR(64) NOT NULL,
                            win_timestamp BIGINT NOT NULL
                        )
                    """ : """
                        CREATE TABLE IF NOT EXISTS koth_win_log (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            uuid VARCHAR(36) NOT NULL,
                            arena_id VARCHAR(64) NOT NULL,
                            win_timestamp BIGINT NOT NULL
                        )
                    """;
            stmt.executeUpdate(logTableQuery);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables", e);
        }
    }

    /**
     * Get a metadata value synchronously. Safe to call on startup.
     */
    public String getMetadataSync(String key) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT meta_value FROM koth_metadata WHERE meta_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("meta_value");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get metadata sync for key: " + key, e);
        }
        return null;
    }

    /**
     * Get a metadata value asynchronously.
     */
    public CompletableFuture<String> getMetadata(String key) {
        return CompletableFuture.supplyAsync(() -> getMetadataSync(key));
    }

    /**
     * Save a metadata value asynchronously.
     */
    public CompletableFuture<Void> setMetadata(String key, String value) {
        return CompletableFuture.runAsync(() -> setMetadataSync(key, value));
    }

    /**
     * Save a metadata value synchronously. Safe to call during shutdown or stop events.
     */
    public void setMetadataSync(String key, String value) {
        try (Connection conn = dataSource.getConnection()) {
            String query = isMySQL ? """
                        INSERT INTO koth_metadata (meta_key, meta_value)
                        VALUES (?, ?)
                        ON DUPLICATE KEY UPDATE meta_value = VALUES(meta_value)
                    """ : """
                        INSERT INTO koth_metadata (meta_key, meta_value)
                        VALUES (?, ?)
                        ON CONFLICT(meta_key) DO UPDATE SET meta_value = excluded.meta_value
                    """;
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set metadata sync for key: " + key, e);
        }
    }

    /**
     * Record a win for a player (async).
     */
    public CompletableFuture<Void> recordWin(UUID uuid, String playerName, String arenaId) {
        return CompletableFuture.runAsync(() -> {
            long now = System.currentTimeMillis();
            try (Connection conn = dataSource.getConnection()) {
                // Upsert stats
                String upsertQuery = isMySQL ? """
                            INSERT INTO koth_stats (uuid, player_name, total_wins, last_win_timestamp)
                            VALUES (?, ?, 1, ?)
                            ON DUPLICATE KEY UPDATE
                                player_name = VALUES(player_name),
                                total_wins = total_wins + 1,
                                last_win_timestamp = VALUES(last_win_timestamp)
                        """ : """
                            INSERT INTO koth_stats (uuid, player_name, total_wins, last_win_timestamp)
                            VALUES (?, ?, 1, ?)
                            ON CONFLICT(uuid) DO UPDATE SET
                                player_name = excluded.player_name,
                                total_wins = total_wins + 1,
                                last_win_timestamp = excluded.last_win_timestamp
                        """;
                try (PreparedStatement ps = conn.prepareStatement(upsertQuery)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, playerName);
                    ps.setLong(3, now);
                    ps.executeUpdate();
                }
                // Log the win
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO koth_win_log (uuid, arena_id, win_timestamp) VALUES (?, ?, ?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, arenaId);
                    ps.setLong(3, now);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to record win for " + playerName, e);
            }
        });
    }

    /**
     * Get stats for a player (async).
     */
    public CompletableFuture<PlayerStats> getStats(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT uuid, player_name, total_wins, last_win_timestamp FROM koth_stats WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new PlayerStats(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("player_name"),
                            rs.getInt("total_wins"),
                            rs.getLong("last_win_timestamp"));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get stats for " + uuid, e);
            }
            return PlayerStats.empty(uuid, "Unknown");
        });
    }

    /**
     * Get the top N players by wins (async).
     */
    public CompletableFuture<List<PlayerStats>> getLeaderboard(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerStats> list = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT uuid, player_name, total_wins, last_win_timestamp FROM koth_stats ORDER BY total_wins DESC LIMIT ?")) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new PlayerStats(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("player_name"),
                            rs.getInt("total_wins"),
                            rs.getLong("last_win_timestamp")));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get leaderboard", e);
            }
            return list;
        });
    }

    /**
     * Get wins within a time window (for daily/weekly queries).
     */
    public CompletableFuture<Integer> getWinsSince(UUID uuid, long sinceTimestamp) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(
                            "SELECT COUNT(*) FROM koth_win_log WHERE uuid = ? AND win_timestamp >= ?")) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, sinceTimestamp);
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    return rs.getInt(1);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get wins since " + sinceTimestamp, e);
            }
            return 0;
        });
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
