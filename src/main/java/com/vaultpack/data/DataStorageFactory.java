package com.vaultpack.data;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.data.sql.MySQLBackpackDataManager;

import java.util.concurrent.CompletableFuture;

/**
 * Phase 2: Factory for creating appropriate data storage backend
 * Switches between YAML and MySQL based on configuration
 */
public class DataStorageFactory {

    private final VaultPackPlugin plugin;
    private DatabaseManager databaseManager;

    public DataStorageFactory(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize and return the configured data storage backend
     */
    public CompletableFuture<Object> createDataStorage() {
        String storageType = plugin.getConfig().getString("data.storage-type", "yaml").toLowerCase();

        plugin.getLogger().info("Initializing data storage: " + storageType.toUpperCase());

        if (storageType.equals("mysql")) {
            return initializeMySQL();
        } else {
            return initializeYAML();
        }
    }

    /**
     * Initialize MySQL storage
     */
    private CompletableFuture<Object> initializeMySQL() {
        databaseManager = new DatabaseManager(plugin);

        return databaseManager.connect().thenApply(success -> {
            if (!success) {
                plugin.getLogger().severe("Failed to connect to MySQL! Falling back to YAML storage.");
                plugin.getLogger().severe("Please check your database configuration in config.yml");
                return initializeYAMLFallback();
            }

            plugin.getLogger().info("MySQL connection established successfully");
            plugin.getLogger().info("Using MySQL for data storage (high-performance mode)");

            MySQLBackpackDataManager mysqlManager = new MySQLBackpackDataManager(plugin, databaseManager);

            // Check if migration is needed
            checkMigrationNeeded();

            return mysqlManager;
        }).exceptionally(ex -> {
            plugin.getLogger().severe("MySQL initialization failed: " + ex.getMessage());
            plugin.getLogger().severe("Falling back to YAML storage");
            return initializeYAMLFallback();
        });
    }

    /**
     * Initialize YAML storage (default)
     */
    private CompletableFuture<Object> initializeYAML() {
        return CompletableFuture.completedFuture(new BackpackDataManager(plugin));
    }

    /**
     * Fallback to YAML when MySQL fails
     */
    private BackpackDataManager initializeYAMLFallback() {
        plugin.getLogger().warning("Using YAML storage (file-based, not recommended for large servers)");
        return new BackpackDataManager(plugin);
    }

    /**
     * Check if migration from YAML to MySQL is needed
     */
    private void checkMigrationNeeded() {
        java.io.File yamlFile = new java.io.File(plugin.getDataFolder(), "playerdata.yml");

        if (yamlFile.exists()) {
            long fileSize = yamlFile.length();
            if (fileSize > 0) {
                plugin.getLogger().warning("=========================================");
                plugin.getLogger().warning("YAML player data file detected!");
                plugin.getLogger().warning("You can migrate your data to MySQL using:");
                plugin.getLogger().warning("/vaultpack migrate yaml-to-mysql");
                plugin.getLogger().warning("=========================================");
            }
        }
    }

    /**
     * Shutdown database connections
     */
    public void shutdown() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    /**
     * Get database manager (if using MySQL)
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
