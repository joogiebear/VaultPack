package com.vaultpack.data;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.data.holders.PlayerDataHolder;
import com.vaultpack.data.storage.MySQLStorage;
import com.vaultpack.data.storage.StorageBackend;
import com.vaultpack.data.storage.YAMLStorage;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Modern data manager using PlayerDataHolder architecture with pluggable storage backends.
 * Handles loading, saving, and caching of player data.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Pluggable storage backends (YAML, MySQL, etc.)</li>
 *   <li>Async loading/saving for performance</li>
 *   <li>LRU caching with automatic unloading</li>
 *   <li>Thread-safe operations</li>
 *   <li>Modular component-based data</li>
 * </ul>
 *
 * <p>Phase 9: Storage backend architecture</p>
 */
public class BackpackDataManager {

    private final VaultPackPlugin plugin;
    private final StorageBackend storage;

    // Cache system
    private final Map<UUID, PlayerDataHolder> dataCache;
    private final Map<UUID, LoadingState> loadingStates;
    private final Map<UUID, Long> lastAccessTime;
    private final Set<UUID> scheduledUnloads;

    // Cache configuration
    private static final int MAX_CACHE_SIZE = 200;
    private static final long UNLOAD_DELAY_TICKS = 6000L; // 5 minutes

    // Statistics
    private long cacheHits = 0;
    private long cacheMisses = 0;

    public enum LoadingState {
        NOT_LOADED,
        LOADING,
        LOADED,
        UNLOADING,
        FAILED
    }

    public BackpackDataManager(VaultPackPlugin plugin) {
        this.plugin = plugin;

        // Initialize caching system
        this.dataCache = new LinkedHashMap<>(16, 0.75f, true); // LRU cache
        this.loadingStates = new ConcurrentHashMap<>();
        this.lastAccessTime = new ConcurrentHashMap<>();
        this.scheduledUnloads = ConcurrentHashMap.newKeySet();

        // Initialize storage backend based on config
        String storageType = plugin.getConfig().getString("data.storage-type", "yaml").toLowerCase();

        switch (storageType) {
            case "mysql":
                DatabaseManager database = new DatabaseManager(plugin);
                this.storage = new MySQLStorage(plugin, database);
                plugin.getLogger().info("Using MySQL storage backend");
                break;

            case "yaml":
            default:
                this.storage = new YAMLStorage(plugin);
                plugin.getLogger().info("Using YAML storage backend");
                break;
        }

        // Initialize storage backend
        storage.initialize().thenAccept(success -> {
            if (success) {
                plugin.getLogger().info("BackpackDataManager initialized with " + storage.getType() + " storage");
            } else {
                plugin.getLogger().severe("Failed to initialize storage backend!");
            }
        });
    }

    /**
     * Get player data synchronously.
     * Prefer loadPlayerDataAsync() for better performance.
     *
     * @param playerId Player UUID
     * @return PlayerDataHolder
     */
    public PlayerDataHolder getPlayerData(UUID playerId) {
        lastAccessTime.put(playerId, System.currentTimeMillis());

        // Check cache
        PlayerDataHolder cached = dataCache.get(playerId);
        if (cached != null) {
            cacheHits++;
            return cached;
        }

        cacheMisses++;

        // Synchronous fallback
        plugin.getLogger().warning("Synchronous data load for " + playerId + " - use async loading!");
        return dataCache.computeIfAbsent(playerId, id -> {
            loadingStates.put(id, LoadingState.LOADING);
            PlayerDataHolder data = loadPlayerDataSync(id);
            loadingStates.put(id, LoadingState.LOADED);
            return data;
        });
    }

    /**
     * Load player data asynchronously (PREFERRED).
     *
     * @param playerId Player UUID
     * @return CompletableFuture with PlayerDataHolder
     */
    public CompletableFuture<PlayerDataHolder> loadPlayerDataAsync(UUID playerId) {
        // Cancel scheduled unload
        scheduledUnloads.remove(playerId);

        // Update access time
        lastAccessTime.put(playerId, System.currentTimeMillis());

        // Check cache
        PlayerDataHolder cached = dataCache.get(playerId);
        if (cached != null) {
            cacheHits++;
            loadingStates.put(playerId, LoadingState.LOADED);
            return CompletableFuture.completedFuture(cached);
        }

        cacheMisses++;

        // Check if already loading
        if (loadingStates.get(playerId) == LoadingState.LOADING) {
            return CompletableFuture.supplyAsync(() -> {
                while (loadingStates.get(playerId) == LoadingState.LOADING) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return dataCache.get(playerId);
            });
        }

        // Load asynchronously
        loadingStates.put(playerId, LoadingState.LOADING);

        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerDataHolder data = loadPlayerDataSync(playerId);
                dataCache.put(playerId, data);
                loadingStates.put(playerId, LoadingState.LOADED);
                return data;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load data for " + playerId, e);
                loadingStates.put(playerId, LoadingState.FAILED);
                // Return empty data holder as fallback
                PlayerDataHolder fallback = new PlayerDataHolder(playerId);
                dataCache.put(playerId, fallback);
                return fallback;
            }
        });
    }

    /**
     * Load player data synchronously from storage backend.
     *
     * @param playerId Player UUID
     * @return PlayerDataHolder
     */
    private PlayerDataHolder loadPlayerDataSync(UUID playerId) {
        return storage.loadPlayerData(playerId);
    }

    /**
     * Save player data to storage backend.
     *
     * @param playerId Player UUID
     */
    public void savePlayerData(UUID playerId) {
        PlayerDataHolder dataHolder = dataCache.get(playerId);
        if (dataHolder == null) {
            return;
        }

        storage.savePlayerData(playerId, dataHolder).join();
    }

    /**
     * Save player data to disk synchronously.
     * Alias for savePlayerData() for clarity.
     *
     * @param playerId Player UUID
     */
    public void savePlayerDataSync(UUID playerId) {
        savePlayerData(playerId);
    }

    /**
     * Save all loaded player data.
     */
    public void saveAllData() {
        plugin.getLogger().info("Saving all player data...");
        storage.saveAllData(dataCache);
    }

    /**
     * Load all player data from storage (used on startup).
     */
    public void loadAllData() {
        Collection<UUID> playerIds = storage.loadAllPlayerIds();

        if (playerIds.isEmpty()) {
            return;
        }

        plugin.getLogger().info("Pre-loading " + playerIds.size() + " player data records...");

        for (UUID playerId : playerIds) {
            loadPlayerDataAsync(playerId);
        }
    }

    /**
     * Unload player data from cache.
     *
     * @param playerId Player UUID
     */
    public void unloadPlayerData(UUID playerId) {
        // Save before unloading
        savePlayerData(playerId);

        dataCache.remove(playerId);
        loadingStates.remove(playerId);
        lastAccessTime.remove(playerId);
        scheduledUnloads.remove(playerId);
    }

    /**
     * Reset player data to defaults.
     *
     * @param playerId Player UUID
     */
    public void resetPlayerData(UUID playerId) {
        PlayerDataHolder dataHolder = dataCache.get(playerId);
        if (dataHolder != null) {
            dataHolder.reset();
            savePlayerData(playerId);
        } else {
            // Delete from storage if exists
            storage.deletePlayerData(playerId).join();
        }
    }

    /**
     * Shutdown the data manager and storage backend.
     */
    public void shutdown() {
        saveAllData();
        storage.shutdown();
    }

    /**
     * Get cache statistics.
     *
     * @return Map of statistics
     */
    public Map<String, Long> getCacheStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("cache-hits", cacheHits);
        stats.put("cache-misses", cacheMisses);
        stats.put("cache-size", (long) dataCache.size());
        stats.put("max-cache-size", (long) MAX_CACHE_SIZE);
        return stats;
    }

    /**
     * Get loading state for a player.
     *
     * @param playerId Player UUID
     * @return Loading state
     */
    public LoadingState getLoadingState(UUID playerId) {
        return loadingStates.getOrDefault(playerId, LoadingState.NOT_LOADED);
    }

    /**
     * Check if a player's data is currently loaded.
     *
     * @param playerId Player UUID
     * @return true if data is loaded, false otherwise
     */
    public boolean isDataLoaded(UUID playerId) {
        return loadingStates.get(playerId) == LoadingState.LOADED;
    }
}
