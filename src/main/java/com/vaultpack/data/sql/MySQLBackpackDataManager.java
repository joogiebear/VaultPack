package com.vaultpack.data.sql;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.data.DatabaseManager;
import com.vaultpack.models.Backpack;
import com.vaultpack.models.BackpackTier;
import com.vaultpack.models.EnderPage;
import com.vaultpack.models.PlayerBackpackData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Phase 2: MySQL-based data manager
 * High-performance async data storage using connection pooling
 */
public class MySQLBackpackDataManager {

    private final VaultPackPlugin plugin;
    private final DatabaseManager database;

    // Phase 1 compatibility: In-memory cache
    private final Map<UUID, PlayerBackpackData> cache;
    private final Map<UUID, Long> lastAccessTime;
    private final Map<UUID, LoadingState> loadingStates;

    // Statistics
    private long dbReads = 0;
    private long dbWrites = 0;
    private long cacheHits = 0;

    public enum LoadingState {
        NOT_LOADED, LOADING, LOADED, SAVING, FAILED
    }

    public MySQLBackpackDataManager(VaultPackPlugin plugin, DatabaseManager database) {
        this.plugin = plugin;
        this.database = database;
        this.cache = new ConcurrentHashMap<>();
        this.lastAccessTime = new ConcurrentHashMap<>();
        this.loadingStates = new ConcurrentHashMap<>();
    }

    // ==================== PLAYER DATA OPERATIONS ====================

    /**
     * Load player data from database (async)
     */
    public CompletableFuture<PlayerBackpackData> loadPlayerDataAsync(UUID playerId) {
        // Check cache first
        PlayerBackpackData cached = cache.get(playerId);
        if (cached != null) {
            cacheHits++;
            lastAccessTime.put(playerId, System.currentTimeMillis());
            return CompletableFuture.completedFuture(cached);
        }

        loadingStates.put(playerId, LoadingState.LOADING);

        return database.executeQueryAsync(conn -> {
            dbReads++;

            // Load player record
            PlayerBackpackData data = loadPlayerRecord(conn, playerId);

            // Load backpacks
            loadBackpacks(conn, data);

            // Load ender pages
            loadEnderPages(conn, data);

            // Cache the data
            cache.put(playerId, data);
            lastAccessTime.put(playerId, System.currentTimeMillis());
            loadingStates.put(playerId, LoadingState.LOADED);

            plugin.getLogger().fine("Loaded data for " + playerId + " from MySQL");
            return data;

        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data from MySQL: " + playerId, ex);
            loadingStates.put(playerId, LoadingState.FAILED);

            // Return fresh data as fallback
            PlayerBackpackData fallback = new PlayerBackpackData(playerId);
            fallback.setUnlockedSlots(plugin.getConfigManager().getDefaultUnlockedSlots());
            fallback.setUnlockedEnderPages(1);
            return fallback;
        });
    }

    /**
     * Load player record from database
     */
    private PlayerBackpackData loadPlayerRecord(Connection conn, UUID playerId) throws SQLException {
        String sql = "SELECT unlocked_slots, unlocked_ender_pages FROM vaultpack_players WHERE uuid = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                PlayerBackpackData data = new PlayerBackpackData(playerId);
                data.setUnlockedSlots(rs.getInt("unlocked_slots"));
                data.setUnlockedEnderPages(rs.getInt("unlocked_ender_pages"));
                return data;
            } else {
                // New player - create record
                return createPlayerRecord(conn, playerId);
            }
        }
    }

    /**
     * Create new player record
     */
    private PlayerBackpackData createPlayerRecord(Connection conn, UUID playerId) throws SQLException {
        String sql = "INSERT INTO vaultpack_players (uuid, unlocked_slots, unlocked_ender_pages) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setInt(2, plugin.getConfigManager().getDefaultUnlockedSlots());
            stmt.setInt(3, 1); // Default ender pages

            stmt.executeUpdate();

            PlayerBackpackData data = new PlayerBackpackData(playerId);
            data.setUnlockedSlots(plugin.getConfigManager().getDefaultUnlockedSlots());
            data.setUnlockedEnderPages(1);
            return data;
        }
    }

    /**
     * Load backpacks for a player
     */
    private void loadBackpacks(Connection conn, PlayerBackpackData data) throws SQLException {
        String sql = "SELECT id, slot_number, tier, type_id FROM vaultpack_backpacks WHERE player_uuid = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data.getPlayerId().toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int backpackId = rs.getInt("id");
                int slotNumber = rs.getInt("slot_number");
                String tierName = rs.getString("tier");
                String typeId = rs.getString("type_id");

                BackpackTier tier = BackpackTier.valueOf(tierName);
                Backpack backpack;

                if (typeId != null) {
                    backpack = new Backpack(data.getPlayerId(), slotNumber, tier, typeId);
                } else {
                    backpack = new Backpack(data.getPlayerId(), slotNumber, tier);
                }

                // Load backpack contents
                loadBackpackContents(conn, backpackId, backpack);

                data.setBackpack(slotNumber, backpack);
            }
        }
    }

    /**
     * Load contents for a backpack
     */
    private void loadBackpackContents(Connection conn, int backpackId, Backpack backpack) throws SQLException {
        String sql = "SELECT slot_index, item_data FROM vaultpack_backpack_contents WHERE backpack_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, backpackId);
            ResultSet rs = stmt.executeQuery();

            Map<Integer, ItemStack> contents = new HashMap<>();
            while (rs.next()) {
                int slotIndex = rs.getInt("slot_index");
                String itemData = rs.getString("item_data");

                ItemStack item = deserializeItem(itemData);
                if (item != null) {
                    contents.put(slotIndex, item);
                }
            }

            backpack.setContents(contents);
        }
    }

    /**
     * Load ender pages for a player
     */
    private void loadEnderPages(Connection conn, PlayerBackpackData data) throws SQLException {
        String sql = "SELECT id, page_number FROM vaultpack_ender_pages WHERE player_uuid = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data.getPlayerId().toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int pageId = rs.getInt("id");
                int pageNumber = rs.getInt("page_number");

                EnderPage page = new EnderPage(data.getPlayerId(), pageNumber);

                // Load page contents
                loadEnderPageContents(conn, pageId, page);

                data.setEnderPage(pageNumber, page);
            }
        }
    }

    /**
     * Load contents for an ender page
     */
    private void loadEnderPageContents(Connection conn, int pageId, EnderPage page) throws SQLException {
        String sql = "SELECT slot_index, item_data FROM vaultpack_ender_contents WHERE page_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pageId);
            ResultSet rs = stmt.executeQuery();

            Map<Integer, ItemStack> contents = new HashMap<>();
            while (rs.next()) {
                int slotIndex = rs.getInt("slot_index");
                String itemData = rs.getString("item_data");

                ItemStack item = deserializeItem(itemData);
                if (item != null) {
                    contents.put(slotIndex, item);
                }
            }

            page.setContents(contents);
        }
    }

    // ==================== SAVE OPERATIONS ====================

    /**
     * Save player data to database (async)
     */
    public CompletableFuture<Void> savePlayerDataAsync(UUID playerId) {
        PlayerBackpackData data = cache.get(playerId);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }

        loadingStates.put(playerId, LoadingState.SAVING);

        return database.<Void>executeQueryAsync(conn -> {
            dbWrites++;

            // Use transaction for atomic save
            conn.setAutoCommit(false);

            try {
                // Update player record
                updatePlayerRecord(conn, data);

                // Save backpacks
                saveBackpacks(conn, data);

                // Save ender pages
                saveEnderPages(conn, data);

                // Commit transaction
                conn.commit();

                loadingStates.put(playerId, LoadingState.LOADED);
                plugin.getLogger().fine("Saved data for " + playerId + " to MySQL");

                return null;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data to MySQL: " + playerId, ex);
            loadingStates.put(playerId, LoadingState.FAILED);
            return null;
        });
    }

    /**
     * Update player record
     */
    private void updatePlayerRecord(Connection conn, PlayerBackpackData data) throws SQLException {
        String sql = "UPDATE vaultpack_players SET unlocked_slots = ?, unlocked_ender_pages = ? WHERE uuid = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, data.getUnlockedSlots());
            stmt.setInt(2, data.getUnlockedEnderPages());
            stmt.setString(3, data.getPlayerId().toString());

            stmt.executeUpdate();
        }
    }

    /**
     * Save all backpacks for a player
     */
    private void saveBackpacks(Connection conn, PlayerBackpackData data) throws SQLException {
        // Delete old backpacks
        String deleteSql = "DELETE FROM vaultpack_backpacks WHERE player_uuid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, data.getPlayerId().toString());
            stmt.executeUpdate();
        }

        // Insert backpacks
        String insertSql = "INSERT INTO vaultpack_backpacks (player_uuid, slot_number, tier, type_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            for (Map.Entry<Integer, Backpack> entry : data.getBackpacks().entrySet()) {
                Backpack backpack = entry.getValue();

                stmt.setString(1, data.getPlayerId().toString());
                stmt.setInt(2, backpack.getSlotNumber());
                stmt.setString(3, backpack.getTier().name());
                stmt.setString(4, backpack.getBackpackTypeId());

                stmt.executeUpdate();

                // Get generated backpack ID
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    int backpackId = keys.getInt(1);

                    // Save backpack contents
                    saveBackpackContents(conn, backpackId, backpack);
                }
            }
        }
    }

    /**
     * Save backpack contents
     */
    private void saveBackpackContents(Connection conn, int backpackId, Backpack backpack) throws SQLException {
        String sql = "INSERT INTO vaultpack_backpack_contents (backpack_id, slot_index, item_data) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, ItemStack> entry : backpack.getContents().entrySet()) {
                if (entry.getValue() == null || entry.getValue().getType().isAir()) {
                    continue;
                }

                stmt.setInt(1, backpackId);
                stmt.setInt(2, entry.getKey());
                stmt.setString(3, serializeItem(entry.getValue()));

                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /**
     * Save ender pages
     */
    private void saveEnderPages(Connection conn, PlayerBackpackData data) throws SQLException {
        // Delete old pages
        String deleteSql = "DELETE FROM vaultpack_ender_pages WHERE player_uuid = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setString(1, data.getPlayerId().toString());
            stmt.executeUpdate();
        }

        // Insert pages
        String insertSql = "INSERT INTO vaultpack_ender_pages (player_uuid, page_number) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            for (Map.Entry<Integer, EnderPage> entry : data.getEnderPages().entrySet()) {
                EnderPage page = entry.getValue();

                stmt.setString(1, data.getPlayerId().toString());
                stmt.setInt(2, page.getPageNumber());

                stmt.executeUpdate();

                // Get generated page ID
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    int pageId = keys.getInt(1);

                    // Save page contents
                    saveEnderPageContents(conn, pageId, page);
                }
            }
        }
    }

    /**
     * Save ender page contents
     */
    private void saveEnderPageContents(Connection conn, int pageId, EnderPage page) throws SQLException {
        String sql = "INSERT INTO vaultpack_ender_contents (page_id, slot_index, item_data) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, ItemStack> entry : page.getContents().entrySet()) {
                if (entry.getValue() == null || entry.getValue().getType().isAir()) {
                    continue;
                }

                stmt.setInt(1, pageId);
                stmt.setInt(2, entry.getKey());
                stmt.setString(3, serializeItem(entry.getValue()));

                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    // ==================== ITEM SERIALIZATION ====================

    /**
     * Serialize ItemStack to Base64 string
     */
    private String serializeItem(ItemStack item) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to serialize item", e);
            return null;
        }
    }

    /**
     * Deserialize ItemStack from Base64 string
     */
    private ItemStack deserializeItem(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            return (ItemStack) dataInput.readObject();

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to deserialize item", e);
            return null;
        }
    }

    // ==================== CACHE MANAGEMENT ====================

    /**
     * Get player data from cache (synchronous, for backward compatibility)
     */
    public PlayerBackpackData getPlayerData(UUID playerId) {
        lastAccessTime.put(playerId, System.currentTimeMillis());
        return cache.get(playerId);
    }

    /**
     * Unload player data with delay
     */
    public void unloadPlayerData(UUID playerId) {
        savePlayerDataAsync(playerId).thenRun(() -> {
            cache.remove(playerId);
            lastAccessTime.remove(playerId);
            loadingStates.remove(playerId);
        });
    }

    /**
     * Get statistics
     */
    public String getStats() {
        return String.format(
            "MySQL Stats: Cache=%d, DB Reads=%d, DB Writes=%d, Cache Hits=%d | %s",
            cache.size(),
            dbReads,
            dbWrites,
            cacheHits,
            database.getPoolStats()
        );
    }

    /**
     * Get cache size
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Check if data is loaded
     */
    public boolean isDataLoaded(UUID playerId) {
        return loadingStates.get(playerId) == LoadingState.LOADED;
    }
}
