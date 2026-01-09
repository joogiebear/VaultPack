package com.vaultpack.config;

import com.vaultpack.config.base.BaseConfig;
import com.vaultpack.config.base.IgnoreField;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main configuration for VaultPack.
 * Handles storage, database, performance, and feature settings.
 *
 * <p>All fields are automatically serialized using camelCase → kebab-case conversion.</p>
 * <p>Example: storageType → storage-type in config.yml</p>
 */
@Getter
public class MainConfig extends BaseConfig {

    // ============================================
    //            Storage Settings
    // ============================================

    private String storageType = "yaml";
    private Boolean backupEnabled = true;
    private Integer autoSaveInterval = 300;
    private Integer maxBackups = 5;
    private String backupFormat = "yyyy-MM-dd_HH-mm-ss";

    // ============================================
    //            MySQL Settings
    // ============================================

    private String mysqlHost = "localhost";
    private Integer mysqlPort = 3306;
    private String mysqlDatabase = "vaultpack";
    private String mysqlUsername = "root";
    private String mysqlPassword = "password";
    private Integer mysqlPoolSize = 10;
    private Integer mysqlMaxLifetime = 1800000;
    private Integer mysqlConnectionTimeout = 30000;
    private Boolean mysqlUseSSL = false;

    // ============================================
    //            Performance Settings
    // ============================================

    private Integer cacheExpiryMinutes = 30;
    private Boolean asyncDataSaving = true;
    private Integer saveQueueSize = 100;
    private Boolean enableMetrics = true;

    // ============================================
    //            Feature Flags
    // ============================================

    private Boolean enableBackpacks = true;
    private Boolean enableEnderChests = true;
    private Boolean enableVirtualStorage = true;
    private Boolean enableCrafting = true;

    // ============================================
    //            Debug Settings
    // ============================================

    private Boolean debugMode = false;
    private Boolean verboseLogging = false;
    private Boolean logSqlQueries = false;

    // ============================================
    //            General Settings
    // ============================================

    private String pluginPrefix = "&8[&6VaultPack&8]&r";
    private String defaultLanguage = "en_US";
    private Boolean checkForUpdates = true;

    // ============================================
    //         Backpack Slot Settings
    // ============================================

    private Integer maxBackpackSlots = 18;
    private Integer defaultUnlockedSlots = 1;

    // ============================================
    //         Economy Settings
    // ============================================

    private Boolean economyEnabled = true;
    private Boolean slotUnlockEnabled = true;
    private Integer slotUnlockBaseCost = 1000;
    private Integer slotUnlockCostPerSlot = 500;

    private Boolean pageUnlockEnabled = true;
    private Integer pageUnlockBaseCost = 2000;
    private Integer pageUnlockCostPerPage = 1000;

    private Boolean upgradeEnabled = true;

    // Upgrade costs (tier transitions)
    private Integer upgradeSmallToMedium = 5000;
    private Integer upgradeMediumToLarge = 10000;
    private Integer upgradeLargeToHuge = 20000;
    private Integer upgradeHugeToMassive = 40000;
    private Integer upgradeMassiveToColossal = 80000;
    private Integer upgradeColossalToGreater = 160000;
    private Integer upgradeGreaterToJumbo = 320000;

    // ============================================
    //         Permission Settings
    // ============================================

    private Boolean usePermissionsForSlots = true;
    private Boolean usePermissionsForPages = true;
    private String slotPermissionFormat = "vaultpack.slots.%slot%";
    private String pagePermissionFormat = "vaultpack.enderchest.page.%page%";

    // ============================================
    //         Blacklist Settings
    // ============================================

    private Boolean blacklistEnabled = true;
    private String blacklistMessage = "&cThis item cannot be stored in backpacks!";

    /**
     * Creates a new MainConfig instance.
     *
     * @param file The config.yml file
     */
    public MainConfig(File file) {
        super(file);
    }

    /**
     * Provides migration steps for config updates.
     * Each step is executed in order based on the current config-version.
     *
     * @return List of migration consumers
     */
    @Override
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        return Arrays.asList(
            // Migration 1: Initial version (1.x → 2.0)
            (yaml) -> {
                // Convert old flat structure to new organized structure
                if (yaml.contains("database.type")) {
                    String oldType = yaml.getString("database.type", "yaml");
                    yaml.set("storage-type", oldType);
                    yaml.set("database.type", null);
                }

                // Migrate MySQL settings if they exist
                if (yaml.contains("database.mysql")) {
                    yaml.set("mysql-host", yaml.get("database.mysql.host"));
                    yaml.set("mysql-port", yaml.get("database.mysql.port"));
                    yaml.set("mysql-database", yaml.get("database.mysql.database"));
                    yaml.set("mysql-username", yaml.get("database.mysql.username"));
                    yaml.set("mysql-password", yaml.get("database.mysql.password"));
                    yaml.set("database.mysql", null);
                }

                // Set version
                yaml.set("config-version", 1);
            },

            // Migration 2: Add new performance settings
            (yaml) -> {
                // Add performance defaults if missing
                if (!yaml.contains("async-data-saving")) {
                    yaml.set("async-data-saving", true);
                }
                if (!yaml.contains("save-queue-size")) {
                    yaml.set("save-queue-size", 100);
                }
                if (!yaml.contains("cache-expiry-minutes")) {
                    yaml.set("cache-expiry-minutes", 30);
                }

                yaml.set("config-version", 2);
            },

            // Migration 3: Add feature flags
            (yaml) -> {
                // Add feature flag defaults if missing
                if (!yaml.contains("enable-backpacks")) {
                    yaml.set("enable-backpacks", true);
                }
                if (!yaml.contains("enable-ender-chests")) {
                    yaml.set("enable-ender-chests", true);
                }
                if (!yaml.contains("enable-virtual-storage")) {
                    yaml.set("enable-virtual-storage", true);
                }
                if (!yaml.contains("enable-crafting")) {
                    yaml.set("enable-crafting", true);
                }

                // Add MySQL pool settings
                if (!yaml.contains("mysql-pool-size")) {
                    yaml.set("mysql-pool-size", 10);
                }
                if (!yaml.contains("mysql-max-lifetime")) {
                    yaml.set("mysql-max-lifetime", 1800000);
                }
                if (!yaml.contains("mysql-connection-timeout")) {
                    yaml.set("mysql-connection-timeout", 30000);
                }
                if (!yaml.contains("mysql-use-ssl")) {
                    yaml.set("mysql-use-ssl", false);
                }

                yaml.set("config-version", 3);
            }
        );
    }

    /**
     * Validates the configuration after loading.
     * Called after load() completes.
     *
     * @return true if config is valid, false otherwise
     */
    public boolean validate() {
        boolean valid = true;

        // Validate storage type
        if (!storageType.equalsIgnoreCase("yaml") &&
            !storageType.equalsIgnoreCase("mysql") &&
            !storageType.equalsIgnoreCase("sqlite")) {
            getLogger().warning("Invalid storage-type: " + storageType + ". Defaulting to yaml.");
            storageType = "yaml";
            valid = false;
        }

        // Validate auto-save interval
        if (autoSaveInterval < 60) {
            getLogger().warning("auto-save-interval too low (" + autoSaveInterval + "s). Minimum is 60s.");
            autoSaveInterval = 60;
            valid = false;
        }

        // Validate MySQL port
        if (mysqlPort < 1 || mysqlPort > 65535) {
            getLogger().warning("Invalid mysql-port: " + mysqlPort + ". Using default 3306.");
            mysqlPort = 3306;
            valid = false;
        }

        // Validate pool size
        if (mysqlPoolSize < 1 || mysqlPoolSize > 50) {
            getLogger().warning("Invalid mysql-pool-size: " + mysqlPoolSize + ". Using default 10.");
            mysqlPoolSize = 10;
            valid = false;
        }

        // Validate cache expiry
        if (cacheExpiryMinutes < 1) {
            getLogger().warning("Invalid cache-expiry-minutes: " + cacheExpiryMinutes + ". Using default 30.");
            cacheExpiryMinutes = 30;
            valid = false;
        }

        return valid;
    }

    @IgnoreField
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("VaultPack");

    private java.util.logging.Logger getLogger() {
        return logger;
    }

    // ============================================
    //         Convenience Methods
    // ============================================

    /**
     * Check if MySQL storage is enabled.
     *
     * @return true if storage type is mysql
     */
    public boolean isMySQLEnabled() {
        return storageType.equalsIgnoreCase("mysql");
    }

    /**
     * Check if SQLite storage is enabled.
     *
     * @return true if storage type is sqlite
     */
    public boolean isSQLiteEnabled() {
        return storageType.equalsIgnoreCase("sqlite");
    }

    /**
     * Check if YAML storage is enabled.
     *
     * @return true if storage type is yaml
     */
    public boolean isYAMLEnabled() {
        return storageType.equalsIgnoreCase("yaml");
    }
}
