package com.example.myplugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for MyPlugin
 * 
 * This plugin is designed to be compatible with both Paper and Folia servers.
 * All schedulers used are Folia-compatible and will work on both platforms.
 */
public class MyPlugin extends JavaPlugin {
    
    private static MyPlugin instance;
    
    // Core managers
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    
    // PlaceholderAPI expansion
    private MyPluginExpansion placeholderExpansion;
    
    @Override
    public void onLoad() {
        // Early initialization - most Bukkit API is not available here
        instance = this;
        
        getLogger().info("MyPlugin is loading...");
    }
    
    @Override
    public void onEnable() {
        try {
            // 1. Load configuration
            saveDefaultConfig();
            
            // 2. Initialize message manager
            this.messageManager = new MessageManager(this);
            
            // 3. Initialize database (if enabled)
            if (getConfig().getBoolean("database.enabled")) {
                this.databaseManager = new DatabaseManager(this);
                databaseManager.connect().join(); // Wait for connection
                getLogger().info("Database connected successfully!");
            }
            
            // 4. Register event listeners
            registerListeners();
            
            // 5. Register commands
            registerCommands();
            
            // 6. Register PlaceholderAPI expansion (if available)
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                this.placeholderExpansion = new MyPluginExpansion(this);
                placeholderExpansion.register();
                getLogger().info("PlaceholderAPI expansion registered!");
            }
            
            // 7. Start scheduled tasks (using Folia-compatible schedulers)
            startScheduledTasks();
            
            getLogger().info("MyPlugin has been enabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable MyPlugin!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            getLogger().info("MyPlugin is shutting down...");
            
            // 1. Cancel all scheduled tasks
            Bukkit.getGlobalRegionScheduler().cancelTasks(this);
            Bukkit.getAsyncScheduler().cancelTasks(this);
            
            // 2. Save all data
            if (databaseManager != null) {
                getLogger().info("Saving all data...");
                // Add your save logic here
                // Example: dataManager.saveAll().join();
            }
            
            // 3. Unregister PlaceholderAPI expansion
            if (placeholderExpansion != null) {
                placeholderExpansion.unregister();
            }
            
            // 4. Close database connections
            if (databaseManager != null) {
                databaseManager.close();
                getLogger().info("Database connections closed.");
            }
            
            getLogger().info("MyPlugin has been disabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin shutdown!", e);
        }
    }
    
    /**
     * Reload plugin configuration
     */
    public void reload() {
        reloadConfig();
        this.messageManager = new MessageManager(this);
        getLogger().info("Configuration reloaded!");
    }
    
    private void registerListeners() {
        // Register your event listeners here
        // Example:
        // getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }
    
    private void registerCommands() {
        // Register your commands here
        // Example:
        // getCommand("myplugin").setExecutor(new MyPluginCommand(this));
    }
    
    private void startScheduledTasks() {
        // Auto-save task (if enabled)
        if (getConfig().getBoolean("features.auto-save.enabled")) {
            int intervalMinutes = getConfig().getInt("features.auto-save.interval-minutes", 5);
            long intervalTicks = intervalMinutes * 60 * 20L;
            
            // Use Folia-compatible global scheduler for auto-save
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
                if (databaseManager != null) {
                    getLogger().info("Auto-saving data...");
                    // Add your auto-save logic here
                    // Example: dataManager.saveAll();
                }
            }, intervalTicks, intervalTicks);
            
            getLogger().info("Auto-save enabled (every " + intervalMinutes + " minutes)");
        }
    }
    
    /**
     * Check if running on Folia
     */
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    // Getters
    public static MyPlugin getInstance() {
        return instance;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
