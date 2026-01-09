package com.vaultpack.utils;

import com.vaultpack.VaultPackPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates plugin configuration on startup to catch errors early
 */
public class ConfigValidator {

    private final VaultPackPlugin plugin;
    private final List<String> errors;
    private final List<String> warnings;

    public ConfigValidator(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    /**
     * Validate all configuration files
     * @return true if validation passed (no errors), false otherwise
     */
    public boolean validate() {
        // Validate main config
        validateMainConfig();

        // Validate backpacks config
        validateBackpacksConfig();

        // Validate menus
        validateMenus();

        // Report results
        if (!warnings.isEmpty()) {
            plugin.getLogger().warning("Configuration warnings:");
            for (String warning : warnings) {
                plugin.getLogger().warning("  - " + warning);
            }
        }

        if (!errors.isEmpty()) {
            plugin.getLogger().severe("Configuration errors found:");
            for (String error : errors) {
                plugin.getLogger().severe("  - " + error);
            }
            plugin.getLogger().severe("Please fix the configuration errors above!");
            return false;
        }

        return true;
    }

    private void validateMainConfig() {
        FileConfiguration config = plugin.getConfig();

        // Check economy settings
        if (config.getBoolean("economy.enabled", false)) {
            if (!plugin.isVaultEnabled()) {
                warnings.add("Economy is enabled but Vault is not installed/loaded");
            }
        }

        // Check slot unlock costs
        ConfigurationSection slotCosts = config.getConfigurationSection("economy.slot-unlock-cost");
        if (slotCosts != null) {
            for (String key : slotCosts.getKeys(false)) {
                try {
                    Integer.parseInt(key);
                    int cost = slotCosts.getInt(key);
                    if (cost < 0) {
                        errors.add("Slot unlock cost for slot " + key + " cannot be negative");
                    }
                } catch (NumberFormatException e) {
                    errors.add("Invalid slot number in slot-unlock-cost: " + key);
                }
            }
        }

        // Check max slots
        int maxSlots = config.getInt("backpacks.max-slots", 18);
        if (maxSlots < 1 || maxSlots > 18) {
            errors.add("max-slots must be between 1 and 18 (found: " + maxSlots + ")");
        }

        // Check default unlocked slots
        int defaultSlots = config.getInt("backpacks.default-unlocked-slots", 1);
        if (defaultSlots < 1 || defaultSlots > maxSlots) {
            errors.add("default-unlocked-slots must be between 1 and " + maxSlots + " (found: " + defaultSlots + ")");
        }
    }

    private void validateBackpacksConfig() {
        try {
            FileConfiguration backpacksConfig = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(new java.io.File(plugin.getDataFolder(), "backpacks.yml"));

            ConfigurationSection backpacks = backpacksConfig.getConfigurationSection("backpacks");
            if (backpacks == null) {
                errors.add("backpacks.yml: Missing 'backpacks' section");
                return;
            }

            for (String backpackId : backpacks.getKeys(false)) {
                ConfigurationSection backpack = backpacks.getConfigurationSection(backpackId);
                if (backpack == null) continue;

                // Check required fields
                if (!backpack.contains("display-name")) {
                    errors.add("backpacks.yml: Backpack '" + backpackId + "' missing display-name");
                }
                if (!backpack.contains("tier")) {
                    errors.add("backpacks.yml: Backpack '" + backpackId + "' missing tier");
                }
                if (!backpack.contains("size")) {
                    errors.add("backpacks.yml: Backpack '" + backpackId + "' missing size");
                } else {
                    int size = backpack.getInt("size");
                    if (size <= 0 || size % 9 != 0) {
                        errors.add("backpacks.yml: Backpack '" + backpackId + "' size must be positive multiple of 9 (found: " + size + ")");
                    }
                    if (size > 45) {
                        warnings.add("backpacks.yml: Backpack '" + backpackId + "' size is very large (" + size + ")");
                    }
                }

                // Check material
                if (!backpack.contains("material")) {
                    warnings.add("backpacks.yml: Backpack '" + backpackId + "' missing material (will use default)");
                }

                // Check recipe
                if (backpack.contains("recipe")) {
                    List<?> recipe = backpack.getList("recipe");
                    if (recipe != null && recipe.size() != 9) {
                        errors.add("backpacks.yml: Backpack '" + backpackId + "' recipe must have exactly 9 slots");
                    }
                }
            }

        } catch (Exception e) {
            errors.add("backpacks.yml: Failed to load or parse file - " + e.getMessage());
        }
    }

    private void validateMenus() {
        // Validate storage menu
        validateMenu("storage");

        // Validate backpack selector menu
        validateMenu("backpack_selector");

        // Validate enderchest menu
        validateMenu("enderchest");
    }

    private void validateMenu(String menuName) {
        try {
            java.io.File menuFile = new java.io.File(plugin.getDataFolder(), "menus/" + menuName + ".yml");
            if (!menuFile.exists()) {
                warnings.add("Menu file missing: menus/" + menuName + ".yml (will be created on first use)");
                return;
            }

            FileConfiguration menuConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(menuFile);

            // Check title
            if (!menuConfig.contains("title")) {
                warnings.add("Menu '" + menuName + "': Missing title");
            }

            // Check size
            if (menuConfig.contains("size")) {
                int size = menuConfig.getInt("size");
                if (size <= 0 || size > 54 || size % 9 != 0) {
                    errors.add("Menu '" + menuName + "': size must be between 9 and 54 and a multiple of 9 (found: " + size + ")");
                }
            }

        } catch (Exception e) {
            warnings.add("Menu '" + menuName + "': Failed to validate - " + e.getMessage());
        }
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
