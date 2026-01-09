package com.vaultpack.data.migration;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.data.BackpackDataManager;
import com.vaultpack.data.DatabaseManager;
import com.vaultpack.data.sql.MySQLBackpackDataManager;
import com.vaultpack.models.PlayerBackpackData;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 2: Migration tool to convert data from YAML to MySQL
 */
public class DataMigrationTool {

    private final VaultPackPlugin plugin;

    public DataMigrationTool(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Migrate all data from YAML to MySQL
     */
    public CompletableFuture<MigrationResult> migrateYAMLtoMySQL(CommandSender sender) {
        return CompletableFuture.supplyAsync(() -> {
            sender.sendMessage("§e[VaultPack] Starting YAML to MySQL migration...");

            try {
                // Step 1: Load YAML data
                sender.sendMessage("§7[1/4] Loading YAML data...");
                File yamlFile = new File(plugin.getDataFolder(), "playerdata.yml");

                if (!yamlFile.exists()) {
                    sender.sendMessage("§c[Error] No YAML data file found!");
                    return new MigrationResult(false, 0, 0, "No YAML file found");
                }

                FileConfiguration yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
                ConfigurationSection playersSection = yamlConfig.getConfigurationSection("players");

                if (playersSection == null) {
                    sender.sendMessage("§c[Error] No player data found in YAML file!");
                    return new MigrationResult(false, 0, 0, "No player data");
                }

                List<UUID> playerIds = new ArrayList<>();
                for (String uuidString : playersSection.getKeys(false)) {
                    try {
                        playerIds.add(UUID.fromString(uuidString));
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("§c[Warning] Invalid UUID: " + uuidString);
                    }
                }

                sender.sendMessage("§a[1/4] Found " + playerIds.size() + " players to migrate");

                // Step 2: Initialize MySQL connection
                sender.sendMessage("§7[2/4] Connecting to MySQL...");
                DatabaseManager database = new DatabaseManager(plugin);

                if (!database.connect().join()) {
                    sender.sendMessage("§c[Error] Failed to connect to MySQL!");
                    return new MigrationResult(false, 0, 0, "Database connection failed");
                }

                sender.sendMessage("§a[2/4] MySQL connection established");

                // Step 3: Migrate each player
                sender.sendMessage("§7[3/4] Migrating player data...");

                BackpackDataManager yamlManager = new BackpackDataManager(plugin);
                MySQLBackpackDataManager mysqlManager = new MySQLBackpackDataManager(plugin, database);

                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger failedCount = new AtomicInteger(0);

                // Migrate in batches to avoid overwhelming the database
                int batchSize = 50;
                for (int i = 0; i < playerIds.size(); i += batchSize) {
                    List<UUID> batch = playerIds.subList(i, Math.min(i + batchSize, playerIds.size()));

                    List<CompletableFuture<Void>> futures = new ArrayList<>();

                    for (UUID playerId : batch) {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            try {
                                // Load from YAML
                                PlayerBackpackData data = yamlManager.getPlayerData(playerId);

                                // Save to MySQL
                                mysqlManager.loadPlayerDataAsync(playerId).join(); // Ensure player record exists
                                mysqlManager.savePlayerDataAsync(playerId).join();

                                successCount.incrementAndGet();

                            } catch (Exception e) {
                                plugin.getLogger().warning("Failed to migrate player " + playerId + ": " + e.getMessage());
                                failedCount.incrementAndGet();
                            }
                        });

                        futures.add(future);
                    }

                    // Wait for batch to complete
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                    // Progress update
                    int progress = Math.min(i + batchSize, playerIds.size());
                    int percentage = (progress * 100) / playerIds.size();
                    sender.sendMessage("§7[3/4] Progress: " + progress + "/" + playerIds.size() +
                        " (" + percentage + "%)");
                }

                sender.sendMessage("§a[3/4] Migration complete: " + successCount.get() + " succeeded, " +
                    failedCount.get() + " failed");

                // Step 4: Create backup and cleanup
                sender.sendMessage("§7[4/4] Creating backup...");

                // Create backup of YAML file
                File backupFile = new File(plugin.getDataFolder(),
                    "playerdata_pre_migration_" +
                        new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date()) +
                        ".yml");

                java.nio.file.Files.copy(yamlFile.toPath(), backupFile.toPath());

                sender.sendMessage("§a[4/4] Backup created: " + backupFile.getName());

                // Disconnect from database
                database.disconnect();

                sender.sendMessage("§a========================================");
                sender.sendMessage("§a[Success] Migration completed!");
                sender.sendMessage("§aMigrated: " + successCount.get() + " players");
                sender.sendMessage("§cFailed: " + failedCount.get() + " players");
                sender.sendMessage("§7Backup: " + backupFile.getName());
                sender.sendMessage("§a========================================");
                sender.sendMessage("§eNext steps:");
                sender.sendMessage("§e1. Change 'storage-type' to 'mysql' in config.yml");
                sender.sendMessage("§e2. Restart the server");
                sender.sendMessage("§e3. Verify everything works correctly");
                sender.sendMessage("§e4. Optionally delete old YAML file (backup saved)");

                return new MigrationResult(true, successCount.get(), failedCount.get(),
                    "Migration successful");

            } catch (Exception e) {
                sender.sendMessage("§c[Error] Migration failed: " + e.getMessage());
                plugin.getLogger().severe("Migration error: " + e.getMessage());
                e.printStackTrace();
                return new MigrationResult(false, 0, 0, e.getMessage());
            }
        });
    }

    /**
     * Migration result data class
     */
    public static class MigrationResult {
        public final boolean success;
        public final int migratedCount;
        public final int failedCount;
        public final String message;

        public MigrationResult(boolean success, int migratedCount, int failedCount, String message) {
            this.success = success;
            this.migratedCount = migratedCount;
            this.failedCount = failedCount;
            this.message = message;
        }
    }
}
