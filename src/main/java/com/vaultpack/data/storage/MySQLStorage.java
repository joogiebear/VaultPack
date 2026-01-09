package com.vaultpack.data.storage;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.data.DatabaseManager;
import com.vaultpack.data.holders.PlayerDataHolder;
import org.bukkit.configuration.file.YamlConfiguration;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * MySQL-based storage backend using component architecture.
 * Stores player data in MySQL database with HikariCP connection pooling.
 *
 * <p>Phase 9: Modern MySQL storage with component support</p>
 * <p>Uses simple key-value approach: each player's data is stored as serialized YAML</p>
 */
public class MySQLStorage implements StorageBackend {

    private final VaultPackPlugin plugin;
    private final DatabaseManager database;
    private boolean ready = false;

    public MySQLStorage(VaultPackPlugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return database.connect().thenApply(connected -> {
            if (connected) {
                ready = true;
                plugin.getLogger().info("MySQL storage initialized successfully");
                plugin.getLogger().info(database.getPoolStats());
                return true;
            } else {
                plugin.getLogger().severe("Failed to initialize MySQL storage");
                ready = false;
                return false;
            }
        });
    }

    @Override
    public PlayerDataHolder loadPlayerData(UUID playerId) {
        PlayerDataHolder dataHolder = new PlayerDataHolder(playerId);

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT component_data FROM vaultpack_player_data WHERE uuid = ?")) {

            stmt.setString(1, playerId.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String yamlData = rs.getString("component_data");

                    // Deserialize YAML data into components
                    YamlConfiguration yaml = new YamlConfiguration();
                    yaml.loadFromString(yamlData);
                    dataHolder.load(yaml);
                    dataHolder.markClean();
                } else {
                    // New player - set defaults
                    dataHolder.setUnlockedSlots(plugin.getConfigManager().getDefaultUnlockedSlots());

                    // Insert new player record
                    insertNewPlayer(playerId, dataHolder);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load MySQL data for " + playerId, e);
        }

        return dataHolder;
    }

    @Override
    public CompletableFuture<Void> savePlayerData(UUID playerId, PlayerDataHolder data) {
        return CompletableFuture.runAsync(() -> {
            // Only save if dirty
            if (!data.isDirty()) {
                return;
            }

            try {
                // Serialize data to YAML string
                YamlConfiguration yaml = new YamlConfiguration();
                data.saveAll(yaml);
                String yamlData = yaml.saveToString();

                // Upsert to database
                try (Connection conn = database.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO vaultpack_player_data (uuid, component_data, updated_at) " +
                         "VALUES (?, ?, NOW()) " +
                         "ON DUPLICATE KEY UPDATE component_data = VALUES(component_data), updated_at = NOW()")) {

                    stmt.setString(1, playerId.toString());
                    stmt.setString(2, yamlData);
                    stmt.executeUpdate();

                    data.markClean();
                }

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save MySQL data for " + playerId, e);
            }
        });
    }

    @Override
    public Collection<UUID> loadAllPlayerIds() {
        List<UUID> playerIds = new ArrayList<>();

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid FROM vaultpack_player_data")) {

            while (rs.next()) {
                try {
                    UUID playerId = UUID.fromString(rs.getString("uuid"));
                    playerIds.add(playerId);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in database: " + rs.getString("uuid"));
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player IDs from MySQL", e);
        }

        return playerIds;
    }

    @Override
    public void saveAllData(Map<UUID, PlayerDataHolder> dataMap) {
        int saved = 0;
        for (Map.Entry<UUID, PlayerDataHolder> entry : dataMap.entrySet()) {
            savePlayerData(entry.getKey(), entry.getValue()).join();
            saved++;
        }
        plugin.getLogger().info("MySQL: Saved " + saved + " player records");
    }

    @Override
    public boolean hasPlayerData(UUID playerId) {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT 1 FROM vaultpack_player_data WHERE uuid = ? LIMIT 1")) {

            stmt.setString(1, playerId.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check player data existence", e);
            return false;
        }
    }

    @Override
    public CompletableFuture<Boolean> deletePlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM vaultpack_player_data WHERE uuid = ?")) {

                stmt.setString(1, playerId.toString());
                int deleted = stmt.executeUpdate();
                return deleted > 0;

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete MySQL data for " + playerId, e);
                return false;
            }
        });
    }

    @Override
    public void shutdown() {
        database.disconnect();
        ready = false;
        plugin.getLogger().info("MySQL storage shutdown complete");
    }

    @Override
    public String getType() {
        return "MySQL";
    }

    @Override
    public boolean isReady() {
        return ready && database.isConnected();
    }

    /**
     * Insert new player record with default data.
     */
    private void insertNewPlayer(UUID playerId, PlayerDataHolder dataHolder) {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            dataHolder.saveAll(yaml);
            String yamlData = yaml.saveToString();

            try (Connection conn = database.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO vaultpack_player_data (uuid, component_data) VALUES (?, ?)")) {

                stmt.setString(1, playerId.toString());
                stmt.setString(2, yamlData);
                stmt.executeUpdate();
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to insert new player record", e);
        }
    }
}
