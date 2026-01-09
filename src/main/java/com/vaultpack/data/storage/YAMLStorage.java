package com.vaultpack.data.storage;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.data.holders.PlayerDataHolder;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * YAML-based storage backend.
 * Stores player data in individual YAML files in the playerdata folder.
 *
 * <p>Phase 9: Modern YAML storage with component architecture</p>
 */
public class YAMLStorage implements StorageBackend {

    private final VaultPackPlugin plugin;
    private final File dataFolder;
    private final File backupFolder;
    private boolean ready = false;

    public YAMLStorage(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create folders
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                if (!backupFolder.exists()) {
                    backupFolder.mkdirs();
                }

                ready = true;
                plugin.getLogger().info("YAML storage initialized at: " + dataFolder.getPath());
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize YAML storage", e);
                ready = false;
                return false;
            }
        });
    }

    @Override
    public PlayerDataHolder loadPlayerData(UUID playerId) {
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        PlayerDataHolder dataHolder = new PlayerDataHolder(playerId);

        if (playerFile.exists()) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerFile);
                dataHolder.load(yaml);
                dataHolder.markClean();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load YAML data for " + playerId, e);
            }
        } else {
            // New player - set defaults
            dataHolder.setUnlockedSlots(plugin.getConfigManager().getDefaultUnlockedSlots());
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

            File playerFile = new File(dataFolder, playerId.toString() + ".yml");

            try {
                YamlConfiguration yaml = new YamlConfiguration();
                data.saveAll(yaml);
                yaml.save(playerFile);
                data.markClean();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save YAML data for " + playerId, e);
            }
        });
    }

    @Override
    public Collection<UUID> loadAllPlayerIds() {
        List<UUID> playerIds = new ArrayList<>();

        if (!dataFolder.exists()) {
            return playerIds;
        }

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            return playerIds;
        }

        for (File file : files) {
            String fileName = file.getName().replace(".yml", "");
            try {
                UUID playerId = UUID.fromString(fileName);
                playerIds.add(playerId);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in playerdata folder: " + fileName);
            }
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
        plugin.getLogger().info("YAML: Saved " + saved + " player data files");
    }

    @Override
    public boolean hasPlayerData(UUID playerId) {
        File playerFile = new File(dataFolder, playerId.toString() + ".yml");
        return playerFile.exists();
    }

    @Override
    public CompletableFuture<Boolean> deletePlayerData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            File playerFile = new File(dataFolder, playerId.toString() + ".yml");
            if (playerFile.exists()) {
                return playerFile.delete();
            }
            return false;
        });
    }

    @Override
    public void shutdown() {
        ready = false;
        plugin.getLogger().info("YAML storage shutdown complete");
    }

    @Override
    public String getType() {
        return "YAML";
    }

    @Override
    public boolean isReady() {
        return ready;
    }
}
