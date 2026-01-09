package com.vaultpack.data.storage;

import com.vaultpack.data.holders.PlayerDataHolder;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage backend interface for VaultPack data persistence.
 * Implementations can use different storage methods (YAML, MySQL, MongoDB, etc.)
 * while maintaining the same component-based architecture.
 *
 * <p>Phase 9: Modern storage abstraction with async support</p>
 */
public interface StorageBackend {

    /**
     * Initialize the storage backend.
     *
     * @return CompletableFuture that completes when initialization is done
     */
    CompletableFuture<Boolean> initialize();

    /**
     * Load player data from storage.
     *
     * @param playerId The player's UUID
     * @return PlayerDataHolder with loaded data, or new instance if not found
     */
    PlayerDataHolder loadPlayerData(UUID playerId);

    /**
     * Save player data to storage.
     *
     * @param playerId The player's UUID
     * @param data     The data to save
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> savePlayerData(UUID playerId, PlayerDataHolder data);

    /**
     * Load all player data from storage.
     * Used for server startup data preloading.
     *
     * @return Collection of all player UUIDs that have data
     */
    Collection<UUID> loadAllPlayerIds();

    /**
     * Save all currently loaded player data.
     * Used for server shutdown or plugin reload.
     *
     * @param dataMap Map of player UUIDs to their data
     */
    void saveAllData(java.util.Map<UUID, PlayerDataHolder> dataMap);

    /**
     * Check if player data exists in storage.
     *
     * @param playerId The player's UUID
     * @return true if data exists
     */
    boolean hasPlayerData(UUID playerId);

    /**
     * Delete player data from storage.
     *
     * @param playerId The player's UUID
     * @return CompletableFuture that completes when deletion is done
     */
    CompletableFuture<Boolean> deletePlayerData(UUID playerId);

    /**
     * Shutdown the storage backend cleanly.
     */
    void shutdown();

    /**
     * Get the storage type name.
     *
     * @return Storage type (e.g., "YAML", "MySQL")
     */
    String getType();

    /**
     * Check if the storage backend is connected/ready.
     *
     * @return true if ready to handle operations
     */
    boolean isReady();
}
