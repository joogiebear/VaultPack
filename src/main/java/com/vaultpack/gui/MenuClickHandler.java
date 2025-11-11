package com.vaultpack.gui;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.config.MenuConfig;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Handles clicks in menu GUIs
 * Routes clicks to appropriate handlers based on menu type
 */
public class MenuClickHandler implements Listener {

    private final VaultPackPlugin plugin;

    public MenuClickHandler(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();

        // Check if this is one of our menu inventories
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (isVaultPackGUI(title)) {
            // Get the clicked inventory (top = GUI, bottom = player inventory)
            Inventory clickedInv = event.getClickedInventory();

            // If player clicked in their own inventory, allow it (so they can pick up backpack items)
            if (clickedInv != null && clickedInv.equals(player.getInventory())) {
                return; // Don't cancel, allow normal inventory interaction
            }

            // Cancel clicks in the GUI inventory to protect it
            event.setCancelled(true);

            int slot = event.getSlot();

            // Allow placement of items in specific cases (checked in handlers)
            // Don't return early if currentItem is null - might be placing an item

            // Route to appropriate handler based on title
            if (title.contains("Storage")) {
                handleStorageClick(player, slot, event);
            } else if (title.contains("Backpacks")) {
                handleBackpackSelectorClick(player, slot, event);
            } else if (title.contains("Ender")) {
                handleEnderChestClick(player, slot, event);
            }
        }
    }

    /**
     * Check if this is a VaultPack GUI
     */
    private boolean isVaultPackGUI(String title) {
        return title.contains("Storage")
            || title.contains("Backpacks")
            || title.contains("Ender")
            || title.contains("VaultPack");
    }

    /**
     * Handle clicks in the unified storage menu
     */
    private void handleStorageClick(Player player, int slot, InventoryClickEvent event) {
        MenuConfig menu = plugin.getMenuManager().getMenu("storage");
        if (menu == null) return;

        // Check if player is trying to place a backpack item into a slot
        org.bukkit.inventory.ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != org.bukkit.Material.AIR) {
            // Player has an item on cursor - check if it's a backpack being placed
            if (handleBackpackPlacement(player, slot, cursor, event)) {
                return; // Placement handled
            }
        }

        // Get ender chest display config
        org.bukkit.configuration.ConfigurationSection enderDisplay =
            menu.getConfig().getConfigurationSection("enderchest-display");
        if (enderDisplay != null && enderDisplay.getBoolean("enabled", true)) {
            java.util.List<Integer> enderSlots = enderDisplay.getIntegerList("slots");
            int enderIndex = enderSlots.indexOf(slot);
            if (enderIndex != -1) {
                // Clicked on an ender page
                int pageNumber = enderIndex + 1;
                com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

                // Check if page is locked
                if (!data.isEnderPageUnlocked(pageNumber)) {
                    // Try to unlock the page
                    handleEnderPageUnlock(player, pageNumber);
                    // Reopen storage menu after unlock attempt to show updated state
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            new com.vaultpack.gui.StorageMenuGUI(plugin).open(player);
                        }
                    }, 3L); // Small delay to let messages show
                } else {
                    // Page is unlocked, open it
                    plugin.getEnderChestManager().openEnderPage(player, pageNumber);
                    playSound(player, "BLOCK_ENDER_CHEST_OPEN");
                }
                return;
            }
        }

        // Get backpack display config
        org.bukkit.configuration.ConfigurationSection backpackDisplay =
            menu.getConfig().getConfigurationSection("backpack-display");
        if (backpackDisplay != null && backpackDisplay.getBoolean("enabled", true)) {
            java.util.List<Integer> backpackSlots = backpackDisplay.getIntegerList("slots");
            int backpackIndex = backpackSlots.indexOf(slot);
            if (backpackIndex != -1) {
                // Clicked on a backpack slot
                int slotNumber = backpackIndex + 1;
                com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

                // Check if slot is locked
                if (!data.isSlotUnlocked(slotNumber)) {
                    // Try to unlock the slot
                    plugin.getBackpackManager().unlockSlot(player, slotNumber);
                    // Reopen storage menu after unlock attempt to show updated state
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            new com.vaultpack.gui.StorageMenuGUI(plugin).open(player);
                        }
                    }, 3L); // Small delay to let messages show
                } else {
                    // Slot is unlocked
                    com.vaultpack.models.Backpack backpack = data.getBackpack(slotNumber);

                    if (backpack != null) {
                        // Backpack exists - check click type
                        if (event.isShiftClick()) {
                            // Shift + Any click = Remove backpack
                            handleBackpackRemoval(player, slotNumber, backpack, event);
                        } else {
                            // Normal click = Open backpack
                            plugin.getBackpackManager().openBackpack(player, slotNumber);
                            playSound(player, "BLOCK_CHEST_OPEN");
                        }
                    } else {
                        // No backpack in slot - do nothing (player can place one)
                    }
                }
                return;
            }
        }

        // Check for navigation buttons based on slot config
        MenuConfig.SlotConfig slotConfig = menu.getSlot(slot);
        if (slotConfig != null && slotConfig.clickAction != null) {
            executeAction(player, slotConfig.clickAction, slotConfig.menu);
            playSound(player, "UI_BUTTON_CLICK");
        }
    }

    /**
     * Handle clicks in the backpack selector menu
     */
    private void handleBackpackSelectorClick(Player player, int slot, InventoryClickEvent event) {
        // Get the GUI and let it handle the click
        com.vaultpack.gui.BackpackSelectorGUI backpackGUI = new com.vaultpack.gui.BackpackSelectorGUI(plugin);
        backpackGUI.handleClick(player, slot, event.isLeftClick(), event.isRightClick(), event.isShiftClick(), event.getCursor());
    }

    /**
     * Handle clicks in the ender chest menu
     */
    private void handleEnderChestClick(Player player, int slot, InventoryClickEvent event) {
        // Get the GUI and let it handle the click
        com.vaultpack.gui.EnderChestGUI enderGUI = new com.vaultpack.gui.EnderChestGUI(plugin);
        enderGUI.handleClick(player, slot, event.isLeftClick(), event.isRightClick());
    }

    /**
     * Handle backpack item placement into storage slot
     */
    private boolean handleBackpackPlacement(Player player, int slot, org.bukkit.inventory.ItemStack cursor, InventoryClickEvent event) {
        MenuConfig menu = plugin.getMenuManager().getMenu("storage");
        if (menu == null) return false;

        // Get backpack display config
        org.bukkit.configuration.ConfigurationSection backpackDisplay =
            menu.getConfig().getConfigurationSection("backpack-display");
        if (backpackDisplay == null) return false;

        java.util.List<Integer> backpackSlots = backpackDisplay.getIntegerList("slots");
        int backpackIndex = backpackSlots.indexOf(slot);

        if (backpackIndex == -1) return false; // Not a backpack slot

        // This is a backpack slot - check if item is a backpack
        if (!isBackpackItem(cursor)) {
            player.sendMessage(ChatColor.RED + "You can only place backpacks in these slots!");
            return true; // Prevent the click
        }

        int slotNumber = backpackIndex + 1;
        com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // Check if slot is unlocked
        if (!data.isSlotUnlocked(slotNumber)) {
            player.sendMessage(ChatColor.RED + "This slot is locked! Click to unlock it first.");
            return true;
        }

        // Check if slot already has a backpack
        if (data.hasBackpack(slotNumber)) {
            player.sendMessage(ChatColor.RED + "This slot already has a backpack! Remove it first.");
            return true;
        }

        // Place the backpack
        try {
            // Get backpack type from NBT
            org.bukkit.inventory.meta.ItemMeta meta = cursor.getItemMeta();
            if (meta == null) return false;

            org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "backpack_type");

            if (!container.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                player.sendMessage(ChatColor.RED + "This backpack item is invalid!");
                return true;
            }

            String backpackTypeId = container.get(key, org.bukkit.persistence.PersistentDataType.STRING);
            com.vaultpack.types.BackpackType backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);

            if (backpackType == null) {
                player.sendMessage(ChatColor.RED + "Unknown backpack type: " + backpackTypeId);
                return true;
            }

            // Create new backpack and place it
            com.vaultpack.models.BackpackTier tier = com.vaultpack.models.BackpackTier.valueOf(backpackType.getDefaultTier().getName().toUpperCase());
            com.vaultpack.models.Backpack backpack = new com.vaultpack.models.Backpack(
                player.getUniqueId(),
                slotNumber,
                tier,
                backpackTypeId
            );
            data.setBackpack(slotNumber, backpack);
            plugin.getDataManager().savePlayerData(player.getUniqueId());

            // Remove item from cursor
            event.setCursor(null);

            // Refresh menu
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    new com.vaultpack.gui.StorageMenuGUI(plugin).open(player);
                }
            }, 1L);

            player.sendMessage(ChatColor.GREEN + "Placed " + ChatColor.translateAlternateColorCodes('&', backpackType.getDisplayName()) + ChatColor.GREEN + " in slot #" + slotNumber + "!");
            playSound(player, "ENTITY_ITEM_PICKUP");

            return true;
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to place backpack: " + e.getMessage());
            plugin.getLogger().warning("Failed to place backpack: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Check if an item is a backpack
     */
    private boolean isBackpackItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "backpack_type");

        return container.has(key, org.bukkit.persistence.PersistentDataType.STRING);
    }

    /**
     * Handle backpack removal from storage slot
     */
    private void handleBackpackRemoval(Player player, int slotNumber, com.vaultpack.models.Backpack backpack, org.bukkit.event.inventory.InventoryClickEvent event) {
        // Check if backpack is empty
        if (!backpack.isEmpty()) {
            player.sendMessage(ChatColor.RED + "This backpack contains items! Empty it before removing.");
            player.sendMessage(ChatColor.YELLOW + "Tip: You can upgrade backpacks without removing them.");
            playSound(player, "ENTITY_VILLAGER_NO");
            return;
        }

        // Backpack is empty - remove it
        try {
            String backpackTypeId = backpack.getBackpackTypeId();
            com.vaultpack.types.BackpackType backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);

            if (backpackType == null) {
                player.sendMessage(ChatColor.RED + "Unknown backpack type!");
                return;
            }

            // Create backpack item
            org.bukkit.inventory.ItemStack backpackItem = plugin.getBackpackTypeManager().createBackpackItem(backpackType);

            // Remove backpack from data
            com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            data.setBackpack(slotNumber, null);
            plugin.getDataManager().savePlayerData(player.getUniqueId());

            // Give backpack item to player
            java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> overflow = player.getInventory().addItem(backpackItem);

            if (!overflow.isEmpty()) {
                // Inventory full - drop at player location
                player.getWorld().dropItem(player.getLocation(), backpackItem);
                player.sendMessage(ChatColor.YELLOW + "Your inventory is full! Backpack dropped at your feet.");
            }

            // Refresh menu
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    new com.vaultpack.gui.StorageMenuGUI(plugin).open(player);
                }
            }, 1L);

            player.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.translateAlternateColorCodes('&', backpackType.getDisplayName()) + ChatColor.GREEN + " from slot #" + slotNumber + "!");
            playSound(player, "ENTITY_ITEM_PICKUP");

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to remove backpack: " + e.getMessage());
            plugin.getLogger().warning("Failed to remove backpack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle ender page unlock attempt
     */
    private void handleEnderPageUnlock(Player player, int pageNumber) {
        plugin.getEnderChestManager().unlockEnderPage(player, pageNumber);
    }

    /**
     * Play a sound to the player
     */
    protected void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty()) return;

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
        }
    }

    /**
     * Execute a click action
     */
    protected void executeAction(Player player, String action, String menu) {
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
                plugin.getLogger().warning("Unknown action: " + action);
                break;
        }
    }

    /**
     * Open a menu by ID
     */
    private void openMenu(Player player, String menuId) {
        MenuConfig menu = plugin.getMenuManager().getMenu(menuId);
        if (menu == null) {
            player.sendMessage(ChatColor.RED + "Menu not found: " + menuId);
            return;
        }

        // Route to appropriate GUI handler
        switch (menuId) {
            case "storage":
                // Open storage menu
                player.closeInventory();
                player.performCommand("storage");
                break;

            case "backpack_selector":
                player.closeInventory();
                player.performCommand("backpack");
                break;

            case "enderchest":
                player.closeInventory();
                player.performCommand("enderchest");
                break;

            default:
                player.sendMessage(ChatColor.RED + "Menu not implemented: " + menuId);
                break;
        }
    }
}
