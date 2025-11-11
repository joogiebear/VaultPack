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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class BackpackDataManager {

    private final VaultPackPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private Map<UUID, PlayerBackpackData> playerDataCache;

    public BackpackDataManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.playerDataCache = new HashMap<>();

        // Create data file if it doesn't exist
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create playerdata.yml!", e);
            }
        }

        loadDataFile();
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

    public PlayerBackpackData getPlayerData(UUID playerId) {
        return playerDataCache.computeIfAbsent(playerId, id -> {
            PlayerBackpackData data = loadPlayerData(id);
            if (data == null) {
                data = new PlayerBackpackData(id);
                data.setUnlockedSlots(plugin.getConfigManager().getDefaultUnlockedSlots());
            }
            return data;
        });
    }

    private PlayerBackpackData loadPlayerData(UUID playerId) {
        String path = "players." + playerId.toString();

        if (!dataConfig.contains(path)) {
            return null;
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

        saveDataFile();
    }

    public void saveAllData() {
        plugin.getLogger().info("Saving all player backpack data...");

        for (UUID playerId : playerDataCache.keySet()) {
            savePlayerData(playerId);
        }

        plugin.getLogger().info("All player data saved!");
    }

    public void loadAllData() {
        plugin.getLogger().info("Loading all player backpack data...");

        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidString : playersSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    getPlayerData(playerId); // Loads and caches
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in playerdata.yml: " + uuidString);
                }
            }
        }

        plugin.getLogger().info("Loaded data for " + playerDataCache.size() + " players!");
    }

    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        playerDataCache.remove(playerId);
    }

    public void clearCache() {
        playerDataCache.clear();
    }
}
