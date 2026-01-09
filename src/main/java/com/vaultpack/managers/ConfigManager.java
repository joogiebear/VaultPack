package com.vaultpack.managers;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.config.BackpacksConfig;
import com.vaultpack.config.BackpackType;
import com.vaultpack.config.MainConfig;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration manager for VaultPack 3.0.
 * Uses the new BaseConfig system for cleaner, reflection-based config management.
 *
 * <p>This class serves as a facade for accessing configuration values from
 * MainConfig and BackpacksConfig.</p>
 */
public class ConfigManager {

    private final VaultPackPlugin plugin;

    @Getter
    private MainConfig mainConfig;

    @Getter
    private BackpacksConfig backpacksConfig;

    // Legacy FileConfiguration for compatibility with plugin.getConfig()
    private FileConfiguration config;

    public ConfigManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all configuration files using the new BaseConfig system.
     */
    public void loadConfig() {
        // Ensure default configs are saved
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Initialize and load MainConfig
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        mainConfig = new MainConfig(configFile);
        mainConfig.load();
        mainConfig.validate();

        // Initialize and load BackpacksConfig
        File backpacksFile = new File(plugin.getDataFolder(), "backpacks.yml");
        if (!backpacksFile.exists()) {
            plugin.saveResource("backpacks.yml", false);
        }
        backpacksConfig = new BackpacksConfig(backpacksFile);
        backpacksConfig.load();

        plugin.getLogger().info("Configuration loaded successfully - VaultPack 3.0");
    }

    /**
     * Reload all configurations from disk.
     */
    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        if (mainConfig != null) {
            mainConfig.reload();
            mainConfig.validate();
        }

        if (backpacksConfig != null) {
            backpacksConfig.reload();
        }

        plugin.getLogger().info("Configurations reloaded successfully");
    }

    /**
     * Save all configurations to disk.
     */
    public void saveConfigs() {
        if (mainConfig != null) {
            mainConfig.save();
        }

        if (backpacksConfig != null) {
            backpacksConfig.save();
        }
    }

    // ============================================
    //         Convenience Delegation Methods
    // ============================================

    /**
     * Get a backpack type by ID.
     *
     * @param id The backpack type ID
     * @return The BackpackType, or null if not found
     */
    public BackpackType getBackpackType(String id) {
        return backpacksConfig.getBackpackType(id);
    }

    /**
     * Get all backpack types ordered by size.
     *
     * @return List of BackpackType ordered by size
     */
    public List<BackpackType> getBackpackTypesBySize() {
        return backpacksConfig.getBackpackTypesBySize();
    }

    /**
     * Check if a material is blacklisted.
     * Note: This needs to be implemented in MainConfig in a future phase.
     *
     * @param material The material to check
     * @return true if blacklisted
     */
    public boolean isBlacklisted(Material material) {
        // TODO Phase 2.5: Migrate blacklist to MainConfig
        boolean blacklistEnabled = config.getBoolean("item-blacklist.enabled", true);
        if (!blacklistEnabled) return false;

        List<String> materialNames = config.getStringList("item-blacklist.materials");
        for (String name : materialNames) {
            try {
                if (Material.valueOf(name.toUpperCase()) == material) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return false;
    }

    /**
     * Get a message from the lang file.
     *
     * @param path         The message path
     * @param replacements Replacement pairs (placeholder, value)
     * @return The formatted message
     */
    public String getMessage(String path, String... replacements) {
        String message = config.getString("messages." + path, path);

        // Apply replacements
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        return message.replace("&", "§");
    }

    /**
     * Get the plugin prefix.
     *
     * @return The prefix from config or MainConfig
     */
    public String getPrefix() {
        if (mainConfig != null) {
            return mainConfig.getPluginPrefix().replace("&", "§");
        }
        return getMessage("prefix");
    }

    /**
     * Get the legacy FileConfiguration.
     * Kept for compatibility with existing code.
     *
     * @return The FileConfiguration
     */
    public FileConfiguration getConfig() {
        return config;
    }

    // ============================================
    //      Storage Type Convenience Methods
    // ============================================

    /**
     * Check if MySQL storage is enabled.
     *
     * @return true if using MySQL
     */
    public boolean isMySQLEnabled() {
        return mainConfig != null && mainConfig.isMySQLEnabled();
    }

    /**
     * Check if YAML storage is enabled.
     *
     * @return true if using YAML
     */
    public boolean isYAMLEnabled() {
        return mainConfig != null && mainConfig.isYAMLEnabled();
    }

    /**
     * Check if SQLite storage is enabled.
     *
     * @return true if using SQLite
     */
    public boolean isSQLiteEnabled() {
        return mainConfig != null && mainConfig.isSQLiteEnabled();
    }

    // ============================================
    //          Feature Flag Methods
    // ============================================

    /**
     * Check if backpacks are enabled.
     *
     * @return true if enabled
     */
    public boolean isBackpacksEnabled() {
        return mainConfig != null && mainConfig.getEnableBackpacks();
    }

    /**
     * Check if ender chests are enabled.
     *
     * @return true if enabled
     */
    public boolean isEnderChestsEnabled() {
        return mainConfig != null && mainConfig.getEnableEnderChests();
    }

    /**
     * Check if virtual storage is enabled.
     *
     * @return true if enabled
     */
    public boolean isVirtualStorageEnabled() {
        return mainConfig != null && mainConfig.getEnableVirtualStorage();
    }

    /**
     * Check if crafting is enabled.
     *
     * @return true if enabled
     */
    public boolean isCraftingEnabled() {
        return mainConfig != null && mainConfig.getEnableCrafting();
    }

    /**
     * Check if debug mode is enabled.
     *
     * @return true if enabled
     */
    public boolean isDebugMode() {
        return mainConfig != null && mainConfig.getDebugMode();
    }

    // ============================================
    //         Backpack Slot Methods
    // ============================================

    /**
     * Get the maximum number of backpack slots.
     *
     * @return Max backpack slots
     */
    public int getMaxBackpackSlots() {
        return mainConfig != null ? mainConfig.getMaxBackpackSlots() : 18;
    }

    /**
     * Get the default number of unlocked slots.
     *
     * @return Default unlocked slots
     */
    public int getDefaultUnlockedSlots() {
        return mainConfig != null ? mainConfig.getDefaultUnlockedSlots() : 1;
    }

    // ============================================
    //         Economy Methods
    // ============================================

    /**
     * Check if economy is enabled.
     *
     * @return true if enabled
     */
    public boolean useEconomy() {
        return mainConfig != null && mainConfig.getEconomyEnabled() && mainConfig.getSlotUnlockEnabled();
    }

    /**
     * Check if slot unlocking is enabled.
     *
     * @return true if enabled
     */
    public boolean isSlotUnlockEnabled() {
        return mainConfig != null && mainConfig.getSlotUnlockEnabled();
    }

    /**
     * Get the cost to unlock a specific slot.
     *
     * @param slotNumber The slot number
     * @return The cost
     */
    public int getSlotUnlockCost(int slotNumber) {
        if (mainConfig == null) return 0;
        return mainConfig.getSlotUnlockBaseCost() + (slotNumber * mainConfig.getSlotUnlockCostPerSlot());
    }

    /**
     * Check if ender page unlocking is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnderUnlockEnabled() {
        return mainConfig != null && mainConfig.getPageUnlockEnabled();
    }

    /**
     * Check if economy is used for ender page unlocking.
     *
     * @return true if enabled
     */
    public boolean enderUseEconomy() {
        return mainConfig != null && mainConfig.getEconomyEnabled() && mainConfig.getPageUnlockEnabled();
    }

    /**
     * Get the cost to unlock an ender chest page.
     *
     * @param pageNumber The page number
     * @return The cost
     */
    public int getEnderPageUnlockCost(int pageNumber) {
        if (mainConfig == null) return 0;
        return mainConfig.getPageUnlockBaseCost() + (pageNumber * mainConfig.getPageUnlockCostPerPage());
    }

    /**
     * Check if upgrades are enabled.
     *
     * @return true if enabled
     */
    public boolean isUpgradeEnabled() {
        return mainConfig != null && mainConfig.getUpgradeEnabled();
    }

    /**
     * Check if economy is used for upgrades.
     *
     * @return true if enabled
     */
    public boolean upgradeUseEconomy() {
        return mainConfig != null && mainConfig.getEconomyEnabled() && mainConfig.getUpgradeEnabled();
    }

    /**
     * Get the cost to upgrade from one tier to another.
     *
     * @param from The current tier
     * @param to   The target tier
     * @return The upgrade cost
     */
    public int getUpgradeCost(com.vaultpack.models.BackpackTier from, com.vaultpack.models.BackpackTier to) {
        if (mainConfig == null) return 0;

        String key = from.name().toLowerCase() + "-to-" + to.name().toLowerCase();

        return switch (key) {
            case "small-to-medium" -> mainConfig.getUpgradeSmallToMedium();
            case "medium-to-large" -> mainConfig.getUpgradeMediumToLarge();
            case "large-to-huge" -> mainConfig.getUpgradeLargeToHuge();
            case "huge-to-massive" -> mainConfig.getUpgradeHugeToMassive();
            case "massive-to-colossal" -> mainConfig.getUpgradeMassiveToColossal();
            case "colossal-to-greater" -> mainConfig.getUpgradeColossalToGreater();
            case "greater-to-jumbo" -> mainConfig.getUpgradeGreaterToJumbo();
            default -> 0;
        };
    }

    // ============================================
    //         Permission Methods
    // ============================================

    /**
     * Check if permissions are used for slot unlocking.
     *
     * @return true if enabled
     */
    public boolean usePermissions() {
        return mainConfig != null && mainConfig.getUsePermissionsForSlots();
    }

    /**
     * Check if permissions are used for ender pages.
     *
     * @return true if enabled
     */
    public boolean enderUsePermissions() {
        return mainConfig != null && mainConfig.getUsePermissionsForPages();
    }

    /**
     * Get the permission node for a specific slot.
     *
     * @param slotNumber The slot number
     * @return The permission node
     */
    public String getSlotPermission(int slotNumber) {
        if (mainConfig == null) return "vaultpack.slots." + slotNumber;
        return mainConfig.getSlotPermissionFormat().replace("%slot%", String.valueOf(slotNumber));
    }

    /**
     * Get the permission node for an ender chest page.
     *
     * @param pageNumber The page number
     * @return The permission node
     */
    public String getEnderPagePermission(int pageNumber) {
        if (mainConfig == null) return "vaultpack.enderchest.page." + pageNumber;
        return mainConfig.getPagePermissionFormat().replace("%page%", String.valueOf(pageNumber));
    }

    // ============================================
    //         Blacklist Methods
    // ============================================

    /**
     * Check if the blacklist is enabled.
     *
     * @return true if enabled
     */
    public boolean isBlacklistEnabled() {
        return mainConfig != null && mainConfig.getBlacklistEnabled();
    }

    /**
     * Get the blacklist message.
     *
     * @return The formatted blacklist message
     */
    public String getBlacklistMessage() {
        if (mainConfig == null) return "&cThis item cannot be stored!";
        return mainConfig.getBlacklistMessage().replace("&", "§");
    }
}
