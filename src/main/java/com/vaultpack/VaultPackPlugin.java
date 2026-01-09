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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class VaultPackPlugin extends JavaPlugin {

    private static VaultPackPlugin instance;
    private Logger logger;

    // Managers
    private ConfigManager configManager;
    private com.vaultpack.managers.MessageManager messageManager;  // Message system (Phase 3: Enhanced with Adventure API)
    private com.vaultpack.managers.AdventureMessageManager adventureMessageManager;  // Phase 3: Pure Component-based messaging (optional)
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

        logger.info("Starting VaultPack v" + getDescription().getVersion() + "...");

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

        logger.info("VaultPack v" + getDescription().getVersion() + " enabled successfully!");
        logger.info("Vault: " + (vaultEnabled ? "✓" : "✗") + " | PlaceholderAPI: " + (placeholderAPIEnabled ? "✓" : "✗"));
    }

    @Override
    public void onDisable() {
        // Cancel all scheduled tasks (Folia-compatible)
        Bukkit.getGlobalRegionScheduler().cancelTasks(this);
        Bukkit.getAsyncScheduler().cancelTasks(this);

        // Save all data
        if (dataManager != null) {
            dataManager.saveAllData();
        }

        // Close backpack inventories
        if (backpackManager != null) {
            backpackManager.closeAllBackpacks();
        }

        logger.info("VaultPack v" + getDescription().getVersion() + " disabled.");
    }

    private void initializeManagers() {
        // Config manager
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Message manager - Load messages from lang.yml (Phase 3: Enhanced with Adventure API support)
        messageManager = new com.vaultpack.managers.MessageManager(this);

        // Adventure Message manager - Pure Component-based messaging (Phase 3: Optional for advanced usage)
        adventureMessageManager = new com.vaultpack.managers.AdventureMessageManager(this);

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
        // Check for Vault, VaultUnlocked, or VaultUnlockedAPI
        Plugin vaultPlugin = getServer().getPluginManager().getPlugin("Vault");
        if (vaultPlugin == null) {
            vaultPlugin = getServer().getPluginManager().getPlugin("VaultUnlocked");
        }
        if (vaultPlugin == null) {
            vaultPlugin = getServer().getPluginManager().getPlugin("VaultUnlockedAPI");
        }

        if (vaultPlugin == null) {
            logger.warning("Vault/VaultUnlocked not found! Economy features will be disabled.");
            logger.info("Install Vault, VaultUnlocked, or VaultUnlockedAPI to enable economy features.");
            vaultEnabled = false;
            return;
        }

        logger.info("Found " + vaultPlugin.getName() + " v" + vaultPlugin.getDescription().getVersion());

        // Check for economy plugins
        Plugin royaleEconomy = getServer().getPluginManager().getPlugin("RoyaleEconomy");
        Plugin ecoPlugin = getServer().getPluginManager().getPlugin("eco");

        if (royaleEconomy != null) {
            logger.info("Detected RoyaleEconomy v" + royaleEconomy.getDescription().getVersion());
        }
        if (ecoPlugin != null) {
            logger.info("Detected eco v" + ecoPlugin.getDescription().getVersion() + " (may act as economy bridge)");
        }

        // Try to get the Economy service provider
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.warning("Vault found, but no economy provider registered!");
            logger.warning("Make sure you have an economy plugin installed (e.g., EssentialsX, CMI, RoyaleEconomy, etc.)");

            // Debug: Check if RoyaleEconomy is loaded but not registered
            if (royaleEconomy != null && royaleEconomy.isEnabled()) {
                logger.warning("RoyaleEconomy is loaded but not registered with Vault!");

                if (ecoPlugin != null && ecoPlugin.isEnabled()) {
                    logger.warning("The 'eco' plugin is loaded - it may need to register the economy provider.");
                    logger.warning("Check eco's configuration to ensure it's set to provide economy services via Vault.");
                } else {
                    logger.warning("RoyaleEconomy requires the 'eco' plugin to bridge with Vault!");
                    logger.warning("Make sure the eco plugin is installed and loaded.");
                }
            }

            vaultEnabled = false;
            return;
        }

        economy = rsp.getProvider();
        vaultEnabled = true;
        logger.info("Vault hooked successfully! Economy provider: " + economy.getName());
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

        // Reload messages
        messageManager.reload();
        adventureMessageManager.reload();

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

    public com.vaultpack.managers.MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Get the Adventure Message Manager for pure Component-based messaging (Phase 3)
     * Use this for advanced text formatting with gradients, hover, click events, etc.
     *
     * @return AdventureMessageManager instance
     */
    public com.vaultpack.managers.AdventureMessageManager getAdventureMessageManager() {
        return adventureMessageManager;
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
