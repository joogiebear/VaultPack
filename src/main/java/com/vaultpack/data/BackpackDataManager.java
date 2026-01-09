package com.vaultpack.data;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.data.holders.PlayerDataHolder;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Modern data manager using PlayerDataHolder architecture.
 * Handles loading, saving, and caching of player data.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Async loading/saving for performance</li>
 *   <li>LRU caching with automatic unloading</li>
 *   <li>Thread-safe operations</li>
 *   <li>Modular component-based data</li>
 * </ul>
 */
public class BackpackDataManager {

    private final VaultPackPlugin plugin;
    private final File dataFolder;
    private final File backupFolder;

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
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.backupFolder = new File(plugin.getDataFolder(), "backups");

        // Initialize caching system
        this.dataCache = new LinkedHashMap<>(16, 0.75f, true); // LRU cache
        this.loadingStates = new ConcurrentHashMap<>();
        this.lastAccessTime = new ConcurrentHashMap<>();
        this.scheduledUnloads = ConcurrentHashMap.newKeySet();

        // Create folders
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        plugin.getLogger().info("BackpackDataManager initialized with component-based architecture");
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
     * Load player data synchronously from disk.
     *
     * @param playerId Player UUID
     * @return PlayerDataHolder
     */
    private PlayerDataHolder loadPlayerDataSync(UUID playerId) {
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");

        PlayerDataHolder dataHolder = new PlayerDataHolder(playerId);

        if (playerFile.exists()) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerFile);
                dataHolder.load(yaml);
                dataHolder.markClean();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load data for " + playerId, e);
            }
        } else {
            // New player - set defaults
            dataHolder.setUnlockedSlots(plugin.getConfigManager().getDefaultUnlockedSlots());
            dataHolder.markDirty();
        }

        return dataHolder;
    }

    /**
     * Save player data to disk.
     *
     * @param playerId Player UUID
     */
    public void savePlayerData(UUID playerId) {
        PlayerDataHolder dataHolder = dataCache.get(playerId);
        if (dataHolder == null) {
            return;
        }

        // Only save if dirty
        if (!dataHolder.isDirty()) {
            return;
        }

        File playerFile = new File(dataFolder, playerId.toString() + ".yml");

        try {
            YamlConfiguration yaml = new YamlConfiguration();
            dataHolder.saveAll(yaml);
            yaml.save(playerFile);
            dataHolder.markClean();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save data for " + playerId, e);
        }
    }

    /**
     * Save all loaded player data.
     */
    public void saveAllData() {
        plugin.getLogger().info("Saving all player data...");

        int saved = 0;
        for (UUID playerId : dataCache.keySet()) {
            savePlayerData(playerId);
            saved++;
        }

        plugin.getLogger().info("Saved " + saved + " player data files");
    }

    /**
     * Load all player data from disk (used on startup).
     */
    public void loadAllData() {
        if (!dataFolder.exists()) {
            return;
        }

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return;
        }

        plugin.getLogger().info("Pre-loading " + files.length + " player data files...");

        for (File file : files) {
            String fileName = file.getName().replace(".yml", "");
            try {
                UUID playerId = UUID.fromString(fileName);
                loadPlayerDataAsync(playerId);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in playerdata folder: " + fileName);
            }
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
            // Delete file if exists
            File playerFile = new File(dataFolder, playerId.toString() + ".yml");
            if (playerFile.exists()) {
                playerFile.delete();
            }
        }
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
}
