package com.vaultpack;

import co.aikar.commands.PaperCommandManager;
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
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class VaultPackPlugin extends JavaPlugin {

    private static VaultPackPlugin instance;
    private Logger logger;

    // Phase 3: ACF Command Manager
    @Getter
    private PaperCommandManager commandManager;

    // Managers
    private ConfigManager configManager;
    private com.vaultpack.messages.MessageManager messageManager;
    private com.vaultpack.config.MenuManager menuManager;
    private BackpackDataManager dataManager;
    private BackpackManager backpackManager;
    private EconomyManager economyManager;
    private BackpackTypeManager backpackTypeManager;
    private com.vaultpack.managers.EnderChestManager enderChestManager;
    private com.vaultpack.managers.ExpansionManager expansionManager; // Phase 7: Expansion system

    // Economy
    private Economy economy = null;
    private boolean vaultEnabled = false;
    private boolean placeholderAPIEnabled = false;
    private Metrics metrics;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();

        logger.info("Starting VaultPack v" + getDescription().getVersion() + "...");

        // Initialize API
        com.vaultpack.api.VaultPackAPI.initialize(this);

        // Initialize managers
        initializeManagers();

        // Phase 3: Initialize ACF Command Manager
        initializeACF();

        // Setup hooks
        setupVault();
        setupPlaceholderAPI();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // Load player data
        dataManager.loadAllData();

        // Phase 7: Register expansions
        registerExpansions();

        // Metrics are optional and require a real bStats plugin id in config.yml.
        initializeMetrics();

        logger.info("VaultPack v" + getDescription().getVersion() + " enabled successfully!");
        logger.info("Vault: " + (vaultEnabled ? "✓" : "✗") + " | PlaceholderAPI: " + (placeholderAPIEnabled ? "✓" : "✗"));
    }

    @Override
    public void onDisable() {
        // Cancel all scheduled tasks (Folia-compatible)
        Bukkit.getGlobalRegionScheduler().cancelTasks(this);
        Bukkit.getAsyncScheduler().cancelTasks(this);

        // Phase 7: Disable all expansions
        if (expansionManager != null) {
            expansionManager.disableAll();
        }

        // Close open storage inventories before final data save so the latest
        // inventory contents are captured before storage shuts down.
        if (backpackManager != null) {
            backpackManager.closeAllBackpacks();
        }
        if (enderChestManager != null) {
            enderChestManager.closeAllEnderPages();
        }

        // Save all data and shutdown storage after open inventories are closed.
        if (dataManager != null) {
            dataManager.shutdown();
        }

        logger.info("VaultPack v" + getDescription().getVersion() + " disabled.");
    }

    private void initializeManagers() {
        // Config manager
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Phase 4: Modern Message manager with Adventure API and MiniMessage
        messageManager = new com.vaultpack.messages.MessageManager(this);

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

        // Phase 7: Expansion manager
        expansionManager = new com.vaultpack.managers.ExpansionManager(this);
    }

    /**
     * Phase 3: Initialize ACF (Aikar's Command Framework)
     * Sets up the modern command framework with annotations and auto-completion.
     */
    private void initializeACF() {
        commandManager = new PaperCommandManager(this);

        // Enable unstable API (required for some features)
        commandManager.enableUnstableAPI("help");

        // Register command contexts (custom parameter types)
        // Example: @CommandAlias("backpack") public void cmd(Player player, @Values("@backpackSlots") int slot)
        // This will be expanded in future phases

        // Register command completions
        commandManager.getCommandCompletions().registerAsyncCompletion("backpackSlots", c -> {
            int maxSlots = configManager.getMaxBackpackSlots();
            java.util.List<String> slots = new java.util.ArrayList<>();
            for (int i = 1; i <= maxSlots; i++) {
                slots.add(String.valueOf(i));
            }
            return slots;
        });

        commandManager.getCommandCompletions().registerAsyncCompletion("enderPages", c -> {
            java.util.List<String> pages = new java.util.ArrayList<>();
            for (int i = 1; i <= 5; i++) { // Max 5 pages for now
                pages.add(String.valueOf(i));
            }
            return pages;
        });

        logger.info("ACF Command Framework initialized successfully");
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

        if (royaleEconomy != null) {
            logger.info("Detected RoyaleEconomy v" + royaleEconomy.getDescription().getVersion());
        }

        // Try to get the Economy service provider
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logger.warning("Vault found, but no economy provider registered!");
            logger.warning("Make sure you have an economy plugin installed (e.g., EssentialsX, CMI, RoyaleEconomy, etc.)");

            // Debug: Check if RoyaleEconomy is loaded but not registered
            if (royaleEconomy != null && royaleEconomy.isEnabled()) {
                logger.warning("RoyaleEconomy is loaded but not registering with Vault!");
                logger.warning("Possible reasons:");
                logger.warning("  - RoyaleEconomy may be loading after Vault (check plugin load order)");
                logger.warning("  - Check RoyaleEconomy's configuration for Vault integration settings");
                logger.warning("  - Try restarting the server to fix load order issues");
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

    /**
     * Initialize optional bStats metrics.
     *
     * <p>The plugin id must be supplied by the project owner after VaultPack is
     * registered on bStats. A value of 0 keeps metrics disabled while still
     * allowing release builds to include the shaded bStats dependency.</p>
     */
    private void initializeMetrics() {
        if (!getConfig().getBoolean("advanced.metrics", true)) {
            logger.info("bStats metrics disabled by configuration.");
            return;
        }

        int pluginId = getConfig().getInt("advanced.bstats-plugin-id", 0);
        if (pluginId <= 0) {
            logger.info("bStats metrics not started: set advanced.bstats-plugin-id after registering VaultPack on bStats.");
            return;
        }

        metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new SimplePie("storage_type", () -> getConfig().getString("data.storage-type", "yaml")));
        metrics.addCustomChart(new SimplePie("vault_hooked", () -> vaultEnabled ? "yes" : "no"));
        metrics.addCustomChart(new SimplePie("placeholderapi_hooked", () -> placeholderAPIEnabled ? "yes" : "no"));
        metrics.addCustomChart(new SimplePie("ecoitems_hooked", () -> Bukkit.getPluginManager().getPlugin("EcoItems") != null ? "yes" : "no"));
        logger.info("bStats metrics initialized.");
    }

    /**
     * Phase 3: Register ACF commands
     * All commands now use ACF (Aikar's Command Framework) for clean, annotation-based structure.
     */
    private void registerCommands() {
        // Register ACF commands
        commandManager.registerCommand(new BackpackCommand(this));
        commandManager.registerCommand(new com.vaultpack.commands.EnderChestCommand(this));
        commandManager.registerCommand(new com.vaultpack.commands.StorageCommand(this));
        commandManager.registerCommand(new VaultPackCommand(this));

        logger.info("Commands registered successfully using ACF");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BackpackListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.vaultpack.listeners.EnderChestListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.vaultpack.listeners.DeathProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new com.vaultpack.listeners.CraftingListener(this), this); // Recipe validation with amounts
        Bukkit.getPluginManager().registerEvents(new com.vaultpack.gui.MenuClickHandler(this), this);
    }

    /**
     * Phase 7: Register expansions
     * Built-in expansions that extend VaultPack functionality.
     */
    private void registerExpansions() {
        // Register built-in expansions
        if (configManager.isDebugMode()) {
            // Only enable logging expansion in debug mode
            expansionManager.registerExpansion(new com.vaultpack.expansions.LoggingExpansion());
        }

        logger.info("Registered " + expansionManager.getExpansions().size() + " expansion(s)");
    }

    public void reload() {
        logger.info("Reloading VaultPack...");

        // Reload config
        configManager.loadConfig();

        // Reload messages (Phase 4: Modern message system)
        messageManager.reload();

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

    /**
     * Get the Message Manager for modern Component-based messaging (Phase 4).
     * Uses Adventure API with MiniMessage for gradients, hover, click events, etc.
     *
     * @return MessageManager instance
     */
    public com.vaultpack.messages.MessageManager getMessageManager() {
        return messageManager;
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

    /**
     * Get the Expansion Manager (Phase 7).
     * Manages registration and lifecycle of plugin expansions.
     *
     * @return ExpansionManager instance
     */
    public com.vaultpack.managers.ExpansionManager getExpansionManager() {
        return expansionManager;
    }
}
