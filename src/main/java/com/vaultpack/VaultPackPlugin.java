package com.vaultpack;

import com.vaultpack.commands.BackpackCommand;
import com.vaultpack.commands.VaultPackCommand;
import com.vaultpack.data.BackpackDataManager;
import com.vaultpack.listeners.BackpackListener;
import com.vaultpack.listeners.PlayerListener;
import com.vaultpack.managers.BackpackManager;
import com.vaultpack.managers.BackpackTypeManager;
import com.vaultpack.managers.ConfigManager;
import com.vaultpack.managers.EconomyManager;
import com.vaultpack.placeholder.BackpackPlaceholder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class VaultPackPlugin extends JavaPlugin {

    private static VaultPackPlugin instance;
    private Logger logger;

    // Managers
    private ConfigManager configManager;
    private com.vaultpack.config.MenuManager menuManager;  // v1.0.0: Menu system
    private BackpackDataManager dataManager;
    private BackpackManager backpackManager;
    private EconomyManager economyManager;
    private BackpackTypeManager backpackTypeManager;
    private com.vaultpack.managers.EnderChestManager enderChestManager; // v2.0.0

    // Economy
    private Economy economy = null;
    private boolean vaultEnabled = false;
    private boolean placeholderAPIEnabled = false;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        logger.info("Starting VaultPack plugin...");

        // Initialize API
        com.vaultpack.api.VaultPackAPI.initialize(this);

        // Initialize managers
        initializeManagers();

        // Setup hooks
        setupVault();
        setupPlaceholderAPI();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // Load player data
        dataManager.loadAllData();

        logger.info("VaultPack plugin enabled successfully!");
        logger.info("Vault/VaultUnlocked: " + (vaultEnabled ? "✓" : "✗"));
        logger.info("PlaceholderAPI: " + (placeholderAPIEnabled ? "✓" : "✗"));
    }

    @Override
    public void onDisable() {
        logger.info("Disabling VaultPack plugin...");

        // Save all data
        if (dataManager != null) {
            dataManager.saveAllData();
        }

        // Close backpack inventories
        if (backpackManager != null) {
            backpackManager.closeAllBackpacks();
        }

        logger.info("VaultPack plugin disabled successfully!");
    }

    private void initializeManagers() {
        logger.info("Initializing managers...");

        // Config manager
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Validate configuration
        com.vaultpack.utils.ConfigValidator validator = new com.vaultpack.utils.ConfigValidator(this);
        if (!validator.validate()) {
            logger.severe("Configuration validation failed! Plugin may not work correctly.");
            logger.severe("Please fix the errors above and reload the plugin.");
        }

        // Menu manager (v1.0.0 - load menus from menus/ folder)
        menuManager = new com.vaultpack.config.MenuManager(this);
        menuManager.loadMenus();

        // Backpack type manager
        backpackTypeManager = new BackpackTypeManager(this);
        backpackTypeManager.loadBackpackTypes();

        // Data manager
        dataManager = new BackpackDataManager(this);

        // Backpack manager
        backpackManager = new BackpackManager(this);

        // Economy manager
        economyManager = new EconomyManager(this);

        // Ender Chest manager (v2.0.0)
        enderChestManager = new com.vaultpack.managers.EnderChestManager(this);
    }

    private void setupVault() {
        // Check for both Vault and VaultUnlocked (modern fork)
        boolean hasVault = getServer().getPluginManager().getPlugin("Vault") != null;
        boolean hasVaultUnlocked = getServer().getPluginManager().getPlugin("VaultUnlocked") != null;

        if (!hasVault && !hasVaultUnlocked) {
            logger.warning("Vault/VaultUnlocked not found! Economy features will be disabled.");
            logger.warning("Install Vault or VaultUnlocked along with an economy plugin (e.g., EssentialsX)");
            vaultEnabled = false;
            return;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.warning("No economy plugin found! Economy features will be disabled.");
            logger.warning("Install an economy plugin like EssentialsX, CMI, or TheNewEconomy");
            vaultEnabled = false;
            return;
        }

        economy = rsp.getProvider();
        vaultEnabled = true;

        // Log which variant we detected
        String vaultVariant = hasVaultUnlocked ? "VaultUnlocked" : "Vault";
        logger.info(vaultVariant + " hooked successfully with " + economy.getName() + "!");
    }

    private void setupPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            logger.warning("PlaceholderAPI not found! Placeholders will not work.");
            logger.warning("Install PlaceholderAPI for GUI functionality!");
            placeholderAPIEnabled = false;
            return;
        }

        new BackpackPlaceholder(this).register();
        placeholderAPIEnabled = true;
        logger.info("PlaceholderAPI hooked successfully!");
    }

    private void registerCommands() {
        logger.info("Registering commands...");

        // Main command
        BackpackCommand backpackCommand = new BackpackCommand(this);
        getCommand("backpack").setExecutor(backpackCommand);
        getCommand("backpack").setTabCompleter(backpackCommand);

        // v2.0.0: Ender chest command
        com.vaultpack.commands.EnderChestCommand enderChestCommand = new com.vaultpack.commands.EnderChestCommand(this);
        getCommand("enderchest").setExecutor(enderChestCommand);
        getCommand("enderchest").setTabCompleter(enderChestCommand);

        // v2.0.0: Storage command
        com.vaultpack.commands.StorageCommand storageCommand = new com.vaultpack.commands.StorageCommand(this);
        getCommand("storage").setExecutor(storageCommand);
        getCommand("storage").setTabCompleter(storageCommand);

        // Internal command
        VaultPackCommand vaultPackCommand = new VaultPackCommand(this);
        getCommand("vaultpack").setExecutor(vaultPackCommand);
        getCommand("vaultpack").setTabCompleter(vaultPackCommand);
    }

    private void registerListeners() {
        logger.info("Registering event listeners...");

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BackpackListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.vaultpack.listeners.EnderChestListener(this), this); // v2.0.0
        Bukkit.getPluginManager().registerEvents(new com.vaultpack.listeners.DeathProtectionListener(this), this); // v2.0.0
        Bukkit.getPluginManager().registerEvents(new com.vaultpack.listeners.CraftingListener(this), this); // Recipe validation with amounts
        Bukkit.getPluginManager().registerEvents(new com.vaultpack.gui.MenuClickHandler(this), this); // v1.0.0: Menu system click handler
    }

    public void reload() {
        logger.info("Reloading VaultPack...");

        // Reload config
        configManager.loadConfig();

        // Reload menus (v1.0.0)
        menuManager.reloadMenus();

        // Reload data
        dataManager.saveAllData();
        dataManager.loadAllData();

        logger.info("VaultPack reloaded successfully!");
    }

    // Getters
    public static VaultPackPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public BackpackDataManager getDataManager() {
        return dataManager;
    }

    public BackpackManager getBackpackManager() {
        return backpackManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public BackpackTypeManager getBackpackTypeManager() {
        return backpackTypeManager;
    }

    public com.vaultpack.managers.EnderChestManager getEnderChestManager() {
        return enderChestManager;
    }

    public com.vaultpack.config.MenuManager getMenuManager() {
        return menuManager;
    }
}
