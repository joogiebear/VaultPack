package com.vaultpack.managers;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.models.BackpackTier;
import com.vaultpack.models.GUIItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final VaultPackPlugin plugin;
    private FileConfiguration config;

    // Config values
    private int maxBackpackSlots;
    private int defaultUnlockedSlots;
    private boolean slotUnlockEnabled;
    private boolean useEconomy;
    private boolean usePermissions;
    private int slotUnlockBaseCost;
    private int slotUnlockCostPerSlot;
    private String permissionFormat;

    private boolean upgradeEnabled;
    private boolean upgradeUseEconomy;
    private Map<String, Integer> upgradeCosts;

    // Blacklist
    private boolean blacklistEnabled;
    private List<Material> blacklistedMaterials;
    private String blacklistMessage;

    // v2.0.0: Ender Chest
    private boolean enderUnlockEnabled;
    private boolean enderUseEconomy;
    private boolean enderUsePermissions;
    private int enderUnlockBaseCost;
    private int enderUnlockCostPerPage;
    private String enderPermissionFormat;

    // v2.0.0: GUI Customization
    private Map<String, GUIItem> guiItems;

    public ConfigManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load storage limits
        maxBackpackSlots = config.getInt("storage.max-backpack-slots", 18);
        defaultUnlockedSlots = config.getInt("storage.default-unlocked-slots", 1);

        // Load economy settings
        boolean economyEnabled = config.getBoolean("economy.enabled", true);

        slotUnlockEnabled = config.getBoolean("economy.slot-unlock.enabled", true);
        useEconomy = economyEnabled && slotUnlockEnabled;
        slotUnlockBaseCost = config.getInt("economy.slot-unlock.base-cost", 1000);
        slotUnlockCostPerSlot = config.getInt("economy.slot-unlock.cost-per-slot", 500);

        enderUnlockEnabled = config.getBoolean("economy.page-unlock.enabled", true);
        enderUseEconomy = economyEnabled && enderUnlockEnabled;
        enderUnlockBaseCost = config.getInt("economy.page-unlock.base-cost", 2000);
        enderUnlockCostPerPage = config.getInt("economy.page-unlock.cost-per-page", 1000);

        upgradeEnabled = true;
        upgradeUseEconomy = economyEnabled;

        // Load upgrade costs
        upgradeCosts = new HashMap<>();
        upgradeCosts.put("small-to-medium", config.getInt("economy.upgrade-costs.small-to-medium", 5000));
        upgradeCosts.put("medium-to-large", config.getInt("economy.upgrade-costs.medium-to-large", 10000));
        upgradeCosts.put("large-to-huge", config.getInt("economy.upgrade-costs.large-to-huge", 20000));
        upgradeCosts.put("huge-to-massive", config.getInt("economy.upgrade-costs.huge-to-massive", 40000));
        upgradeCosts.put("massive-to-colossal", config.getInt("economy.upgrade-costs.massive-to-colossal", 80000));
        upgradeCosts.put("colossal-to-greater", config.getInt("economy.upgrade-costs.colossal-to-greater", 160000));
        upgradeCosts.put("greater-to-jumbo", config.getInt("economy.upgrade-costs.greater-to-jumbo", 320000));

        // Load permission settings
        usePermissions = config.getBoolean("permissions.use-for-slots", true);
        enderUsePermissions = config.getBoolean("permissions.use-for-pages", true);
        permissionFormat = config.getString("permissions.slot-format", "vaultpack.slots.%slot%");
        enderPermissionFormat = config.getString("permissions.page-format", "vaultpack.enderchest.page.%page%");

        // Load blacklist
        blacklistEnabled = config.getBoolean("item-blacklist.enabled", true);
        blacklistedMaterials = new ArrayList<>();
        blacklistMessage = config.getString("item-blacklist.message", "&cThis item cannot be stored in backpacks!");

        List<String> materialNames = config.getStringList("item-blacklist.materials");
        for (String materialName : materialNames) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                blacklistedMaterials.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in blacklist: " + materialName);
            }
        }

        // GUI items no longer loaded here - handled by MenuManager
        guiItems = new HashMap<>();

        plugin.getLogger().info("Configuration loaded successfully!");
        plugin.getLogger().info("Max backpack slots: " + maxBackpackSlots);
        plugin.getLogger().info("Default unlocked slots: " + defaultUnlockedSlots);
        plugin.getLogger().info("Blacklisted materials: " + blacklistedMaterials.size());
    }

    public int getMaxBackpackSlots() {
        return maxBackpackSlots;
    }

    public int getDefaultUnlockedSlots() {
        return defaultUnlockedSlots;
    }

    public boolean isSlotUnlockEnabled() {
        return slotUnlockEnabled;
    }

    public boolean useEconomy() {
        return useEconomy;
    }

    public boolean usePermissions() {
        return usePermissions;
    }

    public int getSlotUnlockCost(int slotNumber) {
        return slotUnlockBaseCost + (slotNumber * slotUnlockCostPerSlot);
    }

    public String getSlotPermission(int slotNumber) {
        return permissionFormat.replace("%slot%", String.valueOf(slotNumber));
    }

    public boolean isUpgradeEnabled() {
        return upgradeEnabled;
    }

    public boolean upgradeUseEconomy() {
        return upgradeUseEconomy;
    }

    public int getUpgradeCost(BackpackTier from, BackpackTier to) {
        String key = from.name().toLowerCase() + "-to-" + to.name().toLowerCase();
        return upgradeCosts.getOrDefault(key, 0);
    }

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

    public String getPrefix() {
        return getMessage("prefix");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean isBlacklistEnabled() {
        return blacklistEnabled;
    }

    public boolean isBlacklisted(Material material) {
        return blacklistEnabled && blacklistedMaterials.contains(material);
    }

    public String getBlacklistMessage() {
        return blacklistMessage.replace("&", "§");
    }

    // v2.0.0: Ender Chest methods
    public boolean isEnderUnlockEnabled() {
        return enderUnlockEnabled;
    }

    public boolean enderUseEconomy() {
        return enderUseEconomy;
    }

    public boolean enderUsePermissions() {
        return enderUsePermissions;
    }

    public int getEnderPageUnlockCost(int pageNumber) {
        return enderUnlockBaseCost + (pageNumber * enderUnlockCostPerPage);
    }

    public String getEnderPagePermission(int pageNumber) {
        return enderPermissionFormat.replace("%page%", String.valueOf(pageNumber));
    }

    // Note: GUI customization is now handled by MenuManager
    // See menus/ folder for GUI configurations
}
