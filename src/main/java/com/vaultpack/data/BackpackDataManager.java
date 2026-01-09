package com.vaultpack.data;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.models.Backpack;
import com.vaultpack.models.BackpackTier;
import com.vaultpack.models.EnderPage;
import com.vaultpack.models.PlayerBackpackData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BackpackDataManager {

    private final VaultPackPlugin plugin;
    private final File dataFile;
    private final File backupFolder;
    private FileConfiguration dataConfig;

    // Phase 1: Enhanced caching system
    private final Map<UUID, PlayerBackpackData> playerDataCache;
    private final Map<UUID, LoadingState> loadingStates;
    private final Map<UUID, Long> lastAccessTime; // For LRU tracking
    private final Set<UUID> scheduledUnloads; // Track pending unloads

    // Cache configuration
    private static final int DEFAULT_MAX_CACHE_SIZE = 200;
    private static final long DEFAULT_UNLOAD_DELAY_TICKS = 6000L; // 5 minutes

    /**
     * Represents the loading state of player data
     */
    public enum LoadingState {
        NOT_LOADED,
        LOADING,
        LOADED,
        UNLOADING,
        FAILED
    }

    // Phase 1: Cache statistics
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long asyncLoads = 0;

    public BackpackDataManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.backupFolder = new File(plugin.getDataFolder(), "backups");

        // Phase 1: Initialize enhanced caching system with thread-safe maps
        this.playerDataCache = new LinkedHashMap<>(16, 0.75f, true); // LRU cache (access-order)
        this.loadingStates = new ConcurrentHashMap<>();
        this.lastAccessTime = new ConcurrentHashMap<>();
        this.scheduledUnloads = ConcurrentHashMap.newKeySet();

        // Create data file if it doesn't exist
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create playerdata.yml!", e);
            }
        }

        // Create backup folder
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }

        loadDataFile();
        scheduleAutoBackup();
    }

    private void loadDataFile() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveDataFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save playerdata.yml!", e);
        }
    }

    /**
     * Phase 1: SYNCHRONOUS getter for backward compatibility
     * This should only be used when data is guaranteed to be loaded
     * For new code, use loadPlayerDataAsync()
     *
     * @param playerId Player UUID
     * @return PlayerBackpackData or creates new if not exists
     */
    public PlayerBackpackData getPlayerData(UUID playerId) {
        // Update last access time for LRU
        lastAccessTime.put(playerId, System.currentTimeMillis());

        // Check cache first
        PlayerBackpackData cached = playerDataCache.get(playerId);
        if (cached != null) {
            cacheHits++;
            return cached;
        }

        cacheMisses++;

        // Synchronous fallback - load immediately (legacy behavior)
        // This blocks the thread but maintains backward compatibility
        plugin.getLogger().warning("Synchronous data load for " + playerId + " - consider using async loading!");
        return playerDataCache.computeIfAbsent(playerId, id -> {
            loadingStates.put(id, LoadingState.LOADING);
            PlayerBackpackData data = loadPlayerDataSync(id);
            loadingStates.put(id, LoadingState.LOADED);
            return data;
        });
    }

    /**
     * Phase 1: ASYNC loading for player data (PREFERRED METHOD)
     * Loads data in background and caches it
     *
     * @param playerId Player UUID
     * @return CompletableFuture with PlayerBackpackData
     */
    public CompletableFuture<PlayerBackpackData> loadPlayerDataAsync(UUID playerId) {
        // Cancel any scheduled unload for this player
        if (scheduledUnloads.remove(playerId)) {
            plugin.getLogger().info("Cancelled scheduled unload for " + playerId + " (player rejoined)");
        }

        // Update last access time
        lastAccessTime.put(playerId, System.currentTimeMillis());

        // Check if already loaded
        PlayerBackpackData cached = playerDataCache.get(playerId);
        if (cached != null) {
            cacheHits++;
            loadingStates.put(playerId, LoadingState.LOADED);
            return CompletableFuture.completedFuture(cached);
        }

        // Check if already loading
        LoadingState state = loadingStates.get(playerId);
        if (state == LoadingState.LOADING) {
            // Wait for existing load to complete
            return waitForLoad(playerId);
        }

        cacheMisses++;
        asyncLoads++;

        // Mark as loading
        loadingStates.put(playerId, LoadingState.LOADING);

        // Load asynchronously
        return CompletableFuture.supplyAsync(() -> {
            try {
                PlayerBackpackData data = loadPlayerDataSync(playerId);

                // Cache the data
                synchronized (playerDataCache) {
                    playerDataCache.put(playerId, data);
                    enforceCacheSizeLimit();
                }

                loadingStates.put(playerId, LoadingState.LOADED);

                plugin.getLogger().fine("Loaded data for " + playerId + " asynchronously");
                return data;

            } catch (Exception e) {
                loadingStates.put(playerId, LoadingState.FAILED);
                plugin.getLogger().log(Level.SEVERE, "Failed to load data for " + playerId, e);

                // Return fresh data as fallback
                PlayerBackpackData fallback = new PlayerBackpackData(playerId);
                fallback.setUnlockedSlots(plugin.getConfigManager().getDefaultUnlockedSlots());
                fallback.setUnlockedEnderPages(1);
                return fallback;
            }
        });
    }

    /**
     * Wait for an ongoing load to complete
     */
    private CompletableFuture<PlayerBackpackData> waitForLoad(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            // Poll for up to 5 seconds
            for (int i = 0; i < 100; i++) {
                LoadingState state = loadingStates.get(playerId);
                if (state == LoadingState.LOADED || state == LoadingState.FAILED) {
                    return playerDataCache.get(playerId);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Timeout - return fresh data
            plugin.getLogger().warning("Timeout waiting for data load: " + playerId);
            PlayerBackpackData fallback = new PlayerBackpackData(playerId);
            fallback.setUnlockedSlots(plugin.getConfigManager().getDefaultUnlockedSlots());
            fallback.setUnlockedEnderPages(1);
            return fallback;
        });
    }

    /**
     * Phase 1: Renamed from loadPlayerData to loadPlayerDataSync
     * Synchronous data loading from YAML file
     */
    private PlayerBackpackData loadPlayerDataSync(UUID playerId) {
        String path = "players." + playerId.toString();

        if (!dataConfig.contains(path)) {
            // Return fresh data for new players
            PlayerBackpackData data = new PlayerBackpackData(playerId);
            data.setUnlockedSlots(plugin.getConfigManager().getDefaultUnlockedSlots());
            data.setUnlockedEnderPages(1);
            return data;
        }

        PlayerBackpackData data = new PlayerBackpackData(playerId);

        // Load unlocked slots
        int unlockedSlots = dataConfig.getInt(path + ".unlocked-slots", 1);
        data.setUnlockedSlots(unlockedSlots);

        // v2.0.0: Load unlocked ender pages
        int unlockedEnderPages = dataConfig.getInt(path + ".unlocked-ender-pages", 1);
        data.setUnlockedEnderPages(unlockedEnderPages);

        // Load backpacks
        ConfigurationSection backpacksSection = dataConfig.getConfigurationSection(path + ".backpacks");
        if (backpacksSection != null) {
            for (String slotKey : backpacksSection.getKeys(false)) {
                try {
                    int slotNumber = Integer.parseInt(slotKey);
                    Backpack backpack = loadBackpack(playerId, slotNumber, path + ".backpacks." + slotKey);
                    if (backpack != null) {
                        data.setBackpack(slotNumber, backpack);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid slot number: " + slotKey);
                }
            }
        }

        // v2.0.0: Load ender pages
        ConfigurationSection enderPagesSection = dataConfig.getConfigurationSection(path + ".ender-pages");
        if (enderPagesSection != null) {
            for (String pageKey : enderPagesSection.getKeys(false)) {
                try {
                    int pageNumber = Integer.parseInt(pageKey);
                    EnderPage enderPage = loadEnderPage(playerId, pageNumber, path + ".ender-pages." + pageKey);
                    if (enderPage != null) {
                        data.setEnderPage(pageNumber, enderPage);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid ender page number: " + pageKey);
                }
            }
        }

        return data;
    }

    private Backpack loadBackpack(UUID playerId, int slotNumber, String path) {
        String tierName = dataConfig.getString(path + ".tier", "SMALL");
        BackpackTier tier = BackpackTier.fromString(tierName);

        // Load backpack type ID if it exists
        String backpackTypeId = dataConfig.getString(path + ".type-id", null);

        Backpack backpack;
        if (backpackTypeId != null) {
            backpack = new Backpack(playerId, slotNumber, tier, backpackTypeId);
        } else {
            backpack = new Backpack(playerId, slotNumber, tier);
        }

        // Load contents
        ConfigurationSection contentsSection = dataConfig.getConfigurationSection(path + ".contents");
        if (contentsSection != null) {
            Map<Integer, ItemStack> contents = new HashMap<>();
            for (String indexKey : contentsSection.getKeys(false)) {
                try {
                    int index = Integer.parseInt(indexKey);
                    ItemStack item = dataConfig.getItemStack(path + ".contents." + indexKey);
                    if (item != null) {
                        contents.put(index, item);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid content index: " + indexKey);
                }
            }
            backpack.setContents(contents);
        }

        return backpack;
    }

    /**
     * v2.0.0: Load ender page from configuration
     */
    private EnderPage loadEnderPage(UUID playerId, int pageNumber, String path) {
        EnderPage enderPage = new EnderPage(playerId, pageNumber);

        // Load contents
        ConfigurationSection contentsSection = dataConfig.getConfigurationSection(path + ".contents");
        if (contentsSection != null) {
            Map<Integer, ItemStack> contents = new HashMap<>();
            for (String indexKey : contentsSection.getKeys(false)) {
                try {
                    int index = Integer.parseInt(indexKey);
                    ItemStack item = dataConfig.getItemStack(path + ".contents." + indexKey);
                    if (item != null) {
                        contents.put(index, item);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid ender page content index: " + indexKey);
                }
            }
            enderPage.setContents(contents);
        }

        return enderPage;
    }

    public void savePlayerData(UUID playerId) {
        savePlayerData(playerId, false);
    }

    public void savePlayerDataSync(UUID playerId) {
        savePlayerData(playerId, true);
    }

    private void savePlayerData(UUID playerId, boolean synchronous) {
        PlayerBackpackData data = playerDataCache.get(playerId);
        if (data == null) {
            return;
        }

        String path = "players." + playerId.toString();

        // Save unlocked slots
        dataConfig.set(path + ".unlocked-slots", data.getUnlockedSlots());

        // v2.0.0: Save unlocked ender pages
        dataConfig.set(path + ".unlocked-ender-pages", data.getUnlockedEnderPages());

        // Clear old backpack data
        dataConfig.set(path + ".backpacks", null);

        // Save backpacks
        for (Map.Entry<Integer, Backpack> entry : data.getBackpacks().entrySet()) {
            int slotNumber = entry.getKey();
            Backpack backpack = entry.getValue();

            String backpackPath = path + ".backpacks." + slotNumber;

            // Save tier
            dataConfig.set(backpackPath + ".tier", backpack.getTier().name());

            // Save backpack type ID if it exists
            if (backpack.getBackpackTypeId() != null) {
                dataConfig.set(backpackPath + ".type-id", backpack.getBackpackTypeId());
            }

            // Save contents
            Map<Integer, ItemStack> contents = backpack.getContents();
            for (Map.Entry<Integer, ItemStack> contentEntry : contents.entrySet()) {
                int index = contentEntry.getKey();
                ItemStack item = contentEntry.getValue();

                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    dataConfig.set(backpackPath + ".contents." + index, item);
                }
            }
        }

        // v2.0.0: Clear old ender page data
        dataConfig.set(path + ".ender-pages", null);

        // v2.0.0: Save ender pages
        for (Map.Entry<Integer, EnderPage> entry : data.getEnderPages().entrySet()) {
            int pageNumber = entry.getKey();
            EnderPage enderPage = entry.getValue();

            String enderPagePath = path + ".ender-pages." + pageNumber;

            // Save contents
            Map<Integer, ItemStack> contents = enderPage.getContents();
            for (Map.Entry<Integer, ItemStack> contentEntry : contents.entrySet()) {
                int index = contentEntry.getKey();
                ItemStack item = contentEntry.getValue();

                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    dataConfig.set(enderPagePath + ".contents." + index, item);
                }
            }
        }

        // Save async or sync
        if (synchronous) {
            saveDataFile();
        } else {
            saveDataFileAsync();
        }
    }

    private void saveDataFileAsync() {
        // Folia-compatible: Use AsyncScheduler for file I/O operations
        org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, task -> saveDataFile());
    }

    public void saveAllData() {
        // Use synchronous saving for shutdown to ensure all data is written
        for (UUID playerId : playerDataCache.keySet()) {
            savePlayerDataSync(playerId);
        }
    }

    /**
     * Phase 1: DEPRECATED - No longer loads all data at startup
     * Data is now loaded lazily when players join
     * Kept for backward compatibility but does nothing
     */
    @Deprecated
    public void loadAllData() {
        plugin.getLogger().info("Phase 1: Lazy loading enabled - player data will load on join");
        plugin.getLogger().info("Skipping startup data load for improved performance");

        // Count total players in file for statistics
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection != null) {
            int totalPlayers = playersSection.getKeys(false).size();
            plugin.getLogger().info("Found " + totalPlayers + " players in database (will load on demand)");
        }
    }

    /**
     * Phase 1: Enhanced unload with delayed removal
     * Schedules data unload after configured delay
     * If player rejoins during delay, unload is cancelled
     *
     * @param playerId Player UUID
     */
    public void unloadPlayerData(UUID playerId) {
        unloadPlayerData(playerId, DEFAULT_UNLOAD_DELAY_TICKS);
    }

    /**
     * Phase 1: Unload player data with custom delay
     *
     * @param playerId Player UUID
     * @param delayTicks Delay in ticks before unloading (20 ticks = 1 second)
     */
    public void unloadPlayerData(UUID playerId, long delayTicks) {
        // Save immediately
        savePlayerData(playerId);

        // Mark as scheduled for unload
        scheduledUnloads.add(playerId);

        // Schedule delayed unload using Folia-compatible GlobalRegionScheduler
        org.bukkit.Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
            // Check if still scheduled (might have been cancelled by rejoin)
            if (scheduledUnloads.remove(playerId)) {
                // Remove from cache
                synchronized (playerDataCache) {
                    playerDataCache.remove(playerId);
                }
                loadingStates.remove(playerId);
                lastAccessTime.remove(playerId);

                plugin.getLogger().fine("Unloaded data for " + playerId + " after " + (delayTicks / 20) + " seconds");
            }
        }, delayTicks);
    }

    /**
     * Phase 1: Immediate unload without delay (for shutdown)
     */
    public void unloadPlayerDataImmediate(UUID playerId) {
        savePlayerDataSync(playerId);
        scheduledUnloads.remove(playerId);

        synchronized (playerDataCache) {
            playerDataCache.remove(playerId);
        }
        loadingStates.remove(playerId);
        lastAccessTime.remove(playerId);
    }

    public void clearCache() {
        playerDataCache.clear();
    }

    /**
     * Reset all data for a player (admin command)
     */
    public void resetPlayerData(UUID playerId) {
        // Remove from cache
        playerDataCache.remove(playerId);

        // Remove from config file
        String path = "players." + playerId.toString();
        dataConfig.set(path, null);
        saveDataFile();

        // Create fresh data with defaults
        PlayerBackpackData newData = new PlayerBackpackData(playerId);
        newData.setUnlockedSlots(plugin.getConfigManager().getDefaultUnlockedSlots());
        newData.setUnlockedEnderPages(1); // Default to 1 page unlocked
        playerDataCache.put(playerId, newData);

        // Save the fresh data
        savePlayerData(playerId);
    }

    /**
     * Create a timestamped backup of player data asynchronously
     * PERFORMANCE FIX: Runs backup async to prevent server lag
     */
    public void createBackup() {
        // Folia-compatible: Use AsyncScheduler for file I/O operations
        org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            try {
                // Generate timestamp for backup name
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
                File backupFile = new File(backupFolder, "playerdata_" + timestamp + ".yml");

                // Copy current data file to backup
                java.nio.file.Files.copy(dataFile.toPath(), backupFile.toPath());

                plugin.getLogger().info("Backup created: " + backupFile.getName());

                // Clean up old backups (keep last 10)
                cleanupOldBackups(com.vaultpack.utils.Constants.BACKUP_RETENTION_COUNT);

            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to create backup!", e);
            }
        });
    }

    /**
     * Delete old backup files, keeping only the most recent N backups
     */
    private void cleanupOldBackups(int keepCount) {
        File[] backups = backupFolder.listFiles((dir, name) -> name.startsWith("playerdata_") && name.endsWith(".yml"));

        if (backups == null || backups.length <= keepCount) {
            return; // No cleanup needed
        }

        // Sort by last modified time (newest first)
        java.util.Arrays.sort(backups, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        // Delete old backups
        int deleted = 0;
        for (int i = keepCount; i < backups.length; i++) {
            if (backups[i].delete()) {
                deleted++;
            }
        }

        if (deleted > 0) {
            plugin.getLogger().info("Cleaned up " + deleted + " old backup(s)");
        }
    }

    /**
     * Schedule automatic backups every 30 minutes
     */
    private void scheduleAutoBackup() {
        // Folia-compatible: Use GlobalRegionScheduler for periodic global tasks
        // Run backup every 30 minutes (36000 ticks)
        org.bukkit.Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            plugin.getLogger().info("Running automatic backup...");
            createBackup();
        }, 36000L, 36000L); // 30 minutes = 36000 ticks
    }

    /**
     * Phase 1: Enforce cache size limit using LRU eviction
     * Removes least recently accessed data when cache exceeds max size
     * Must be called inside synchronized block on playerDataCache
     */
    private void enforceCacheSizeLimit() {
        int maxSize = plugin.getConfig().getInt("performance.max-cached-players", DEFAULT_MAX_CACHE_SIZE);

        if (playerDataCache.size() <= maxSize) {
            return; // Within limit
        }

        // Find least recently accessed player
        UUID oldestPlayer = null;
        long oldestAccess = Long.MAX_VALUE;

        for (Map.Entry<UUID, Long> entry : lastAccessTime.entrySet()) {
            if (entry.getValue() < oldestAccess) {
                oldestAccess = entry.getValue();
                oldestPlayer = entry.getKey();
            }
        }

        // Evict oldest player
        if (oldestPlayer != null) {
            plugin.getLogger().fine("Cache limit reached - evicting " + oldestPlayer);

            // Save before eviction
            savePlayerDataSync(oldestPlayer);

            // Remove from cache
            playerDataCache.remove(oldestPlayer);
            loadingStates.remove(oldestPlayer);
            lastAccessTime.remove(oldestPlayer);
        }
    }

    /**
     * Phase 1: Get current loading state for a player
     */
    public LoadingState getLoadingState(UUID playerId) {
        return loadingStates.getOrDefault(playerId, LoadingState.NOT_LOADED);
    }

    /**
     * Phase 1: Check if player data is loaded
     */
    public boolean isDataLoaded(UUID playerId) {
        return loadingStates.get(playerId) == LoadingState.LOADED;
    }

    /**
     * Phase 1: Get cache statistics
     */
    public String getCacheStats() {
        double hitRate = cacheHits + cacheMisses > 0
            ? (double) cacheHits / (cacheHits + cacheMisses) * 100
            : 0;

        return String.format(
            "Cache Stats: %d/%d entries | Hits: %d | Misses: %d | Hit Rate: %.1f%% | Async Loads: %d",
            playerDataCache.size(),
            plugin.getConfig().getInt("performance.max-cached-players", DEFAULT_MAX_CACHE_SIZE),
            cacheHits,
            cacheMisses,
            hitRate,
            asyncLoads
        );
    }

    /**
     * Phase 1: Get current cache size
     */
    public int getCacheSize() {
        return playerDataCache.size();
    }

    /**
     * Phase 1: Get number of scheduled unloads
     */
    public int getScheduledUnloadsCount() {
        return scheduledUnloads.size();
    }

    /**
     * Get the backup folder
     */
    public File getBackupFolder() {
        return backupFolder;
    }
}
