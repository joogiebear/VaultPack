package com.vaultpack.gui;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.config.MenuConfig;
import com.vaultpack.models.EnderPage;
import com.vaultpack.models.PlayerBackpackData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the ender chest page selector menu (menus/enderchest.yml)
 * Shows all 9 ender chest pages
 */
public class EnderChestGUI {

    private final VaultPackPlugin plugin;
    private final GUIBuilder builder;

    public EnderChestGUI(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.builder = new GUIBuilder(plugin);
    }

    /**
     * Open the ender chest page selector for a player
     */
    public void open(Player player) {
        // Fetch menu config dynamically to support reload
        MenuConfig menuConfig = plugin.getMenuManager().getMenu("enderchest");
        if (menuConfig == null) {
            player.sendMessage(ChatColor.RED + "Ender chest menu not configured!");
            return;
        }

        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // Calculate total items across all pages
        int totalItems = 0;
        for (int i = 1; i <= 9; i++) {
            if (data.isEnderPageUnlocked(i)) {
                EnderPage page = data.getEnderPage(i);
                if (page != null) {
                    totalItems += page.getUsedSlots();
                }
            }
        }

        // Build placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("unlocked_pages", String.valueOf(data.getUnlockedEnderPages()));
        placeholders.put("total_capacity", String.valueOf(data.getUnlockedEnderPages() * 45));
        placeholders.put("total_items", String.valueOf(totalItems));

        // Build base menu
        Inventory inv = builder.buildMenu(menuConfig, player, placeholders);

        // Add all 9 ender pages
        addEnderPages(inv, player, data, menuConfig);

        // Open for player
        player.openInventory(inv);

        // Play sound
        String openSound = menuConfig.getSound("open");
        if (openSound != null) {
            playSound(player, openSound);
        }
    }

    /**
     * Add all ender chest pages to the menu
     */
    private void addEnderPages(Inventory inv, Player player, PlayerBackpackData data, MenuConfig menuConfig) {
        ConfigurationSection pagesSection = menuConfig.getConfig().getConfigurationSection("enderchest-pages");
        if (pagesSection == null) return;

        ConfigurationSection lockedConfig = pagesSection.getConfigurationSection("locked");
        ConfigurationSection emptyConfig = pagesSection.getConfigurationSection("unlocked-empty");
        ConfigurationSection activeConfig = pagesSection.getConfigurationSection("unlocked-active");

        // Process all 9 pages
        for (int pageNumber = 1; pageNumber <= 9; pageNumber++) {
            String pageKey = "page-" + pageNumber;
            ConfigurationSection pageSection = pagesSection.getConfigurationSection(pageKey);

            if (pageSection == null) continue;

            int guiSlot = pageSection.getInt("slot", -1);
            if (guiSlot == -1) continue;

            boolean unlocked = data.isEnderPageUnlocked(pageNumber);
            EnderPage page = data.getEnderPage(pageNumber);

            ItemStack item;

            if (!unlocked) {
                // Locked page
                item = createLockedPageItem(player, pageNumber, lockedConfig);
            } else if (page == null || page.isEmpty()) {
                // Empty page
                item = createEmptyPageItem(player, pageNumber, emptyConfig);
            } else {
                // Active page with items
                item = createActivePageItem(player, pageNumber, page, activeConfig);
            }

            inv.setItem(guiSlot, item);
        }
    }

    /**
     * Create a locked page item
     */
    private ItemStack createLockedPageItem(Player player, int pageNumber, ConfigurationSection config) {
        String material = config.getString("item", "gray_dye");
        String name = config.getString("name", "&7Page %page% &c✖ LOCKED");
        List<String> lore = config.getStringList("lore");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("page", String.valueOf(pageNumber));
        placeholders.put("cost", String.valueOf(plugin.getConfigManager().getEnderPageUnlockCost(pageNumber)));

        return builder.createDynamicItem(material, name, lore, player, placeholders);
    }

    /**
     * Create an empty page item
     */
    private ItemStack createEmptyPageItem(Player player, int pageNumber, ConfigurationSection config) {
        String material = config.getString("item", "ender_pearl");
        String name = config.getString("name", "&5Page %page% &a✓ &7EMPTY");
        List<String> lore = config.getStringList("lore");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("page", String.valueOf(pageNumber));

        return builder.createDynamicItem(material, name, lore, player, placeholders);
    }

    /**
     * Create an active page item
     */
    private ItemStack createActivePageItem(Player player, int pageNumber, EnderPage page, ConfigurationSection config) {
        String material = config.getString("item", "ender_eye");
        String name = config.getString("name", "&5Page %page% &a✓ &8[%fill_percent%%]");
        List<String> lore = config.getStringList("lore");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("page", String.valueOf(pageNumber));
        placeholders.put("used", String.valueOf(page.getUsedSlots()));

        // Calculate fill percentage (45 slots per page)
        int fillPercent = (page.getUsedSlots() * 100) / 45;
        placeholders.put("fill_percent", String.valueOf(fillPercent));
        placeholders.put("fill_bar", page.getFullnessBar());

        return builder.createDynamicItem(material, name, lore, player, placeholders);
    }

    /**
     * Handle click in this menu
     */
    public void handleClick(Player player, int slot, boolean leftClick, boolean rightClick) {
        // Fetch menu config dynamically to support reload
        MenuConfig menuConfig = plugin.getMenuManager().getMenu("enderchest");
        if (menuConfig == null) return;

        // First check for navigation buttons (close, back, etc.)
        MenuConfig.SlotConfig slotConfig = menuConfig.getSlot(slot);
        if (slotConfig != null && slotConfig.clickAction != null) {
            executeNavigationAction(player, slotConfig.clickAction, slotConfig.menu);
            playSound(player, menuConfig.getSound("click"));
            return;
        }

        // Get the page number from the GUI slot
        int pageNumber = getPageNumberFromGUISlot(slot, menuConfig);
        if (pageNumber == -1) return;

        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        boolean unlocked = data.isEnderPageUnlocked(pageNumber);

        if (!unlocked) {
            if (leftClick) {
                // Try to unlock the page
                handleUnlockClick(player, pageNumber, menuConfig);
            } else if (rightClick) {
                // Show unlock info
                handleShowUnlockInfo(player, pageNumber);
            }
        } else {
            // Open the ender page
            plugin.getEnderChestManager().openEnderPage(player, pageNumber);
            playSound(player, menuConfig.getSound("page-open"));
        }
    }

    /**
     * Execute navigation action (close, open menu, etc.)
     */
    private void executeNavigationAction(Player player, String action, String menu) {
        if (action == null) return;

        switch (action.toLowerCase()) {
            case "close_inventory":
                player.closeInventory();
                break;

            case "open_menu":
                if (menu != null) {
                    openMenu(player, menu);
                }
                break;

            default:
                plugin.getLogger().warning("Unknown navigation action: " + action);
                break;
        }
    }

    /**
     * Open a menu by name
     */
    private void openMenu(Player player, String menuName) {
        switch (menuName.toLowerCase()) {
            case "storage":
                StorageMenuGUI storageGUI = new StorageMenuGUI(plugin);
                storageGUI.open(player);
                break;

            default:
                plugin.getLogger().warning("Unknown menu: " + menuName);
                break;
        }
    }

    /**
     * Map GUI slot to page number
     */
    private int getPageNumberFromGUISlot(int guiSlot, MenuConfig menuConfig) {
        ConfigurationSection pagesSection = menuConfig.getConfig().getConfigurationSection("enderchest-pages");
        if (pagesSection == null) return -1;

        for (int pageNumber = 1; pageNumber <= 9; pageNumber++) {
            String pageKey = "page-" + pageNumber;
            ConfigurationSection pageSection = pagesSection.getConfigurationSection(pageKey);
            if (pageSection != null && pageSection.getInt("slot", -1) == guiSlot) {
                return pageNumber;
            }
        }

        return -1;
    }

    /**
     * Handle unlock page click
     */
    private void handleUnlockClick(Player player, int pageNumber, MenuConfig menuConfig) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // Check if player has permission
        String permission = plugin.getConfigManager().getEnderPagePermission(pageNumber);
        if (player.hasPermission(permission)) {
            // Unlock with permission
            data.unlockEnderPage(pageNumber);
            player.sendMessage(ChatColor.GREEN + "Ender page " + pageNumber + " unlocked!");
            playSound(player, menuConfig.getSound("unlock"));
            // Refresh menu
            open(player);
            return;
        }

        // Try to unlock with economy
        if (plugin.isVaultEnabled() && plugin.getConfigManager().enderUseEconomy()) {
            int cost = plugin.getConfigManager().getEnderPageUnlockCost(pageNumber);

            if (plugin.getEconomy().has(player, cost)) {
                plugin.getEconomy().withdrawPlayer(player, cost);
                data.unlockEnderPage(pageNumber);
                player.sendMessage(ChatColor.GREEN + "Ender page " + pageNumber + " unlocked for $" + cost + "!");
                playSound(player, menuConfig.getSound("unlock"));
                // Refresh menu
                open(player);
            } else {
                player.sendMessage(ChatColor.RED + "You need $" + cost + " to unlock this page!");
                playSound(player, menuConfig.getSound("error"));
            }
        } else {
            player.sendMessage(ChatColor.RED + "You don't have permission to unlock this page!");
            playSound(player, menuConfig.getSound("error"));
        }
    }

    /**
     * Handle show unlock info
     */
    private void handleShowUnlockInfo(Player player, int pageNumber) {
        int cost = plugin.getConfigManager().getEnderPageUnlockCost(pageNumber);
        String permission = plugin.getConfigManager().getEnderPagePermission(pageNumber);

        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Ender Page " + pageNumber + " - Unlock Info");
        player.sendMessage(ChatColor.GRAY + "Capacity: " + ChatColor.AQUA + "45 slots " + ChatColor.GRAY + "(5 rows)");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Unlock Options:");
        player.sendMessage(ChatColor.DARK_AQUA + "  • " + ChatColor.GRAY + "Economy: " + ChatColor.GREEN + "$" + cost);
        player.sendMessage(ChatColor.DARK_AQUA + "  • " + ChatColor.GRAY + "Permission: " + ChatColor.WHITE + permission);
        player.sendMessage("");
    }

    /**
     * Play sound
     */
    private void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty()) return;

        try {
            // Convert enum-style sound names (BLOCK_CHEST_OPEN) to namespaced keys (block.chest.open)
            String convertedSound = soundName.toLowerCase().replace('_', '.');
            player.playSound(player.getLocation(), convertedSound,
                org.bukkit.SoundCategory.MASTER, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
        }
    }
}
