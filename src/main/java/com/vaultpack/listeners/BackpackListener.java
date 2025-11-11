package com.vaultpack.listeners;

import com.vaultpack.VaultPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;

public class BackpackListener implements Listener {

    private final VaultPackPlugin plugin;

    public BackpackListener(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // Check if they're closing a backpack
        if (plugin.getBackpackManager().isBackpackOpen(player)) {
            String title = event.getView().getTitle();

            // Check if it's a backpack inventory (starts with "Backpack #")
            if (title.contains("Backpack #")) {
                plugin.getBackpackManager().closeBackpack(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if clicking in backpack inventory
        if (title.contains("Backpack #") && plugin.getBackpackManager().isBackpackOpen(player)) {
            // Check if clicked in the top inventory (backpack GUI)
            if (event.getClickedInventory() != null &&
                event.getClickedInventory().equals(event.getView().getTopInventory())) {

                int slot = event.getSlot();

                // Handle navigation header clicks (slots 0-8)
                if (slot >= 0 && slot <= 8) {
                    event.setCancelled(true); // Cancel all navigation button clicks
                    handleNavigationClick(player, slot);
                    return;
                }
            }

            // Check for blacklisted items being added to backpack
            org.bukkit.inventory.ItemStack clickedItem = event.getCurrentItem();
            org.bukkit.inventory.ItemStack cursorItem = event.getCursor();

            // Check the item being moved into the backpack
            org.bukkit.inventory.ItemStack itemToCheck = null;

            // If clicking in the backpack inventory (top) with an item on cursor
            if (event.getClickedInventory() != null &&
                event.getClickedInventory().equals(event.getView().getTopInventory()) &&
                cursorItem != null && cursorItem.getType() != org.bukkit.Material.AIR) {
                itemToCheck = cursorItem;
            }
            // If shift-clicking from player inventory to backpack
            else if (event.isShiftClick() &&
                     event.getClickedInventory() != null &&
                     event.getClickedInventory().equals(event.getView().getBottomInventory()) &&
                     clickedItem != null && clickedItem.getType() != org.bukkit.Material.AIR) {
                itemToCheck = clickedItem;
            }

            // Check if the item is blacklisted
            if (itemToCheck != null && plugin.getConfigManager().isBlacklisted(itemToCheck.getType())) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getBlacklistMessage());
                return;
            }

            // Allow normal interaction in backpack
            return;
        }

        // Check if clicking in unified storage GUI (v2.0.0)
        String unifiedTitle = ChatColor.translateAlternateColorCodes('&', "&6&lVaultPack Storage");
        if (title.equals(unifiedTitle)) {
            // Handle unified GUI clicks
            handleUnifiedGUIClick(event);
            return;
        }

        // Check if clicking in legacy backpack menu (compatibility)
        String menuTitle = ChatColor.translateAlternateColorCodes('&', "&8Backpacks");
        if (title.equals(menuTitle)) {
            // Check if clicking in the top inventory (GUI) or bottom inventory (player inventory)
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                // Clicking in the GUI - cancel the event and handle custom logic
                event.setCancelled(true);

                if (event.getCurrentItem() == null || event.getCurrentItem().getType() == org.bukkit.Material.AIR) {
                    return;
                }

                // Get clicked slot number from display name
                String displayName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            // Close button
            if (displayName.contains("Close")) {
                player.closeInventory();
                return;
            }

            // Info button - do nothing
            if (displayName.contains("Info")) {
                return;
            }

            // Extract slot number from display name
            // Handles both "Backpack Slot #X" and "Backpack #X"
            int slotNumber = -1;
            if (displayName.contains("#")) {
                try {
                    String[] parts = displayName.split("#");
                    if (parts.length > 1) {
                        // Get number part (everything before the first space or end of string)
                        String afterHash = parts[1].trim();
                        String numberPart = afterHash.split(" ")[0];
                        slotNumber = Integer.parseInt(numberPart);
                    }
                } catch (Exception ignored) {
                }
            }

            if (slotNumber == -1) {
                return;
            }

                // Handle different click types
                if (event.isLeftClick()) {
                    handleLeftClick(player, slotNumber);
                } else if (event.isRightClick()) {
                    handleRightClick(player, slotNumber);
                } else if (event.isShiftClick()) {
                    handleShiftClick(player, slotNumber);
                }
            } else {
                // Clicking in player's own inventory - allow it but prevent shift-clicking items into GUI
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleLeftClick(Player player, int slotNumber) {
        com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (!data.isSlotUnlocked(slotNumber)) {
            // Try to unlock slot
            plugin.getBackpackManager().unlockSlot(player, slotNumber);
            player.closeInventory();
            // Reopen menu to show updated state
            plugin.getBackpackManager().openUnifiedStorageGUI(player);
        } else if (!data.hasBackpack(slotNumber)) {
            // Try to place backpack from cursor (clicked item)
            handleBackpackPlacement(player, slotNumber, data);
        } else {
            // Backpack already exists - check if player is holding a larger backpack to swap
            org.bukkit.inventory.ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && cursor.getType() != org.bukkit.Material.AIR) {
                // Check if it's a backpack item
                if (cursor.hasItemMeta() && cursor.getItemMeta().getPersistentDataContainer()
                        .has(new org.bukkit.NamespacedKey(plugin, "backpack_type"),
                             org.bukkit.persistence.PersistentDataType.STRING)) {

                    // Try to swap/upgrade
                    handleBackpackSwap(player, slotNumber, data);
                    return;
                }
            }

            // No item on cursor, just open the backpack
            player.closeInventory();
            plugin.getBackpackManager().openBackpack(player, slotNumber);
        }
    }

    private void handleRightClick(Player player, int slotNumber) {
        // Check if this is a confirmation (pending removal exists)
        boolean wasConfirmation = plugin.getBackpackManager().hasPendingRemoval(player, slotNumber);

        plugin.getBackpackManager().removeBackpack(player, slotNumber);

        // Only close/reopen menu if it was a confirmation (actual removal happened)
        if (wasConfirmation) {
            player.closeInventory();
        }
    }

    private void handleShiftClick(Player player, int slotNumber) {
        plugin.getBackpackManager().upgradeBackpack(player, slotNumber);
        player.closeInventory();
        plugin.getBackpackManager().openUnifiedStorageGUI(player);
    }

    private com.vaultpack.models.BackpackTier getTierFromSize(int size) {
        // Map size to tier
        switch (size) {
            case 9:
                return com.vaultpack.models.BackpackTier.SMALL;
            case 18:
                return com.vaultpack.models.BackpackTier.MEDIUM;
            case 27:
                return com.vaultpack.models.BackpackTier.LARGE;
            case 36:
                return com.vaultpack.models.BackpackTier.GREATER;
            case 45:
                return com.vaultpack.models.BackpackTier.JUMBO;
            default:
                return com.vaultpack.models.BackpackTier.SMALL;
        }
    }

    private void handleBackpackPlacement(Player player, int slotNumber, com.vaultpack.models.PlayerBackpackData data) {
        org.bukkit.inventory.ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != org.bukkit.Material.AIR) {
            // Check if it's a backpack item by checking NBT tag
            if (cursor.hasItemMeta() && cursor.getItemMeta().getPersistentDataContainer()
                    .has(new org.bukkit.NamespacedKey(plugin, "backpack_type"),
                         org.bukkit.persistence.PersistentDataType.STRING)) {

                // Get backpack type ID from NBT
                String backpackTypeId = cursor.getItemMeta().getPersistentDataContainer()
                        .get(new org.bukkit.NamespacedKey(plugin, "backpack_type"),
                             org.bukkit.persistence.PersistentDataType.STRING);

                // Get the backpack type
                com.vaultpack.types.BackpackType backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);

                if (backpackType != null) {
                    // Get tier from backpack type
                    com.vaultpack.models.BackpackTier tier = getTierFromSize(backpackType.getDefaultTier().getSize());

                    // Create the backpack with type ID
                    plugin.getBackpackManager().createBackpack(player, slotNumber, tier, backpackTypeId);

                    // Remove item from cursor
                    cursor.setAmount(cursor.getAmount() - 1);
                    player.setItemOnCursor(cursor);

                    player.sendMessage(ChatColor.GREEN + "Backpack placed in slot #" + slotNumber + "!");
                    player.closeInventory();
                    plugin.getBackpackManager().openUnifiedStorageGUI(player);
                } else {
                    player.sendMessage(ChatColor.RED + "Invalid backpack type!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "You must click with a backpack item to place it!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "You must click with a backpack item to place it!");
        }
    }

    private void handleBackpackSwap(Player player, int slotNumber, com.vaultpack.models.PlayerBackpackData data) {
        org.bukkit.inventory.ItemStack cursor = player.getItemOnCursor();

        // Get backpack type ID from NBT
        String backpackTypeId = cursor.getItemMeta().getPersistentDataContainer()
                .get(new org.bukkit.NamespacedKey(plugin, "backpack_type"),
                     org.bukkit.persistence.PersistentDataType.STRING);

        // Get the backpack type
        com.vaultpack.types.BackpackType backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);

        if (backpackType == null) {
            player.sendMessage(ChatColor.RED + "Invalid backpack type!");
            return;
        }

        // Get new tier
        com.vaultpack.models.BackpackTier newTier = getTierFromSize(backpackType.getDefaultTier().getSize());
        int newSize = backpackType.getDefaultTier().getSize();

        // Get existing backpack
        com.vaultpack.models.Backpack existingBackpack = data.getBackpack(slotNumber);
        int oldSize = existingBackpack.getSize();

        // Check if new backpack is larger
        if (newSize <= oldSize) {
            player.sendMessage(ChatColor.RED + "You can only swap with a LARGER backpack!");
            player.sendMessage(ChatColor.YELLOW + "Current: " + oldSize + " slots | New: " + newSize + " slots");
            return;
        }

        // Get existing items
        java.util.Map<Integer, org.bukkit.inventory.ItemStack> existingItems = existingBackpack.getContents();

        // Create new backpack with same items and new type ID
        com.vaultpack.models.Backpack newBackpack = new com.vaultpack.models.Backpack(
                player.getUniqueId(), slotNumber, newTier, backpackTypeId);
        newBackpack.setContents(existingItems);

        // Replace backpack
        data.setBackpack(slotNumber, newBackpack);
        plugin.getDataManager().savePlayerData(player.getUniqueId());

        // Remove item from cursor
        cursor.setAmount(cursor.getAmount() - 1);
        player.setItemOnCursor(cursor);

        player.sendMessage(ChatColor.GREEN + "Backpack upgraded from " + oldSize + " to " + newSize + " slots!");
        player.sendMessage(ChatColor.YELLOW + "All items have been preserved!");
        player.closeInventory();
        plugin.getBackpackManager().openUnifiedStorageGUI(player);
    }

    /**
     * v2.0.0: Handle clicks in the unified storage GUI
     */
    private void handleUnifiedGUIClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        // Cancel all clicks in GUI
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            event.setCancelled(true);

            // Get the raw slot number (0-53 in a 54-slot GUI)
            int rawSlot = event.getRawSlot();

            // Check if this is a backpack slot (GUI slots 28-45, which is 0-indexed 27-44)
            if (rawSlot >= 27 && rawSlot <= 44) {
                int slotNumber = rawSlot - 26; // Convert to backpack slot 1-18
                com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

                if (event.isLeftClick()) {
                    handleLeftClick(player, slotNumber);
                } else if (event.isRightClick()) {
                    handleRightClick(player, slotNumber);
                } else if (event.isShiftClick()) {
                    handleShiftClick(player, slotNumber);
                }
                return;
            }

            // Check if this is an ender page slot (GUI slots 10-18, which is 0-indexed 9-17)
            if (rawSlot >= 9 && rawSlot <= 17) {
                int pageNumber = rawSlot - 8; // Convert to page 1-9
                handleEnderPageClick(player, pageNumber);
                return;
            }

            // For other items (buttons, icons), check the display name
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == org.bukkit.Material.AIR) {
                return;
            }

            String displayName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            // Close button
            if (displayName.contains("Close")) {
                player.closeInventory();
                return;
            }

            // Back button
            if (displayName.contains("Back")) {
                player.closeInventory();
                // TODO: Return to previous menu if opened from menu plugin
                return;
            }

            // Search button
            if (displayName.contains("Search")) {
                player.sendMessage(ChatColor.YELLOW + "Search feature coming soon!");
                // TODO: Implement search functionality
                return;
            }

            // Info button - do nothing
            if (displayName.contains("Info")) {
                return;
            }

            // Ender Chest icon - do nothing
            if (displayName.contains("Ender Chest")) {
                return;
            }

            // Backpacks icon - do nothing
            if (displayName.contains("Backpacks")) {
                return;
            }
        } else {
            // Clicking in player inventory - prevent shift-click
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handle ender page clicks
     */
    private void handleEnderPageClick(Player player, int pageNumber) {
        com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (!data.isEnderPageUnlocked(pageNumber)) {
            // Try to unlock
            plugin.getEnderChestManager().unlockEnderPage(player, pageNumber);
            player.closeInventory();
            // Reopen to show updated state
            plugin.getBackpackManager().openUnifiedStorageGUI(player);
        } else {
            // Open the ender page
            player.closeInventory();
            plugin.getEnderChestManager().openEnderPage(player, pageNumber);
        }
    }

    /**
     * Handle navigation button clicks in backpack header
     */
    private void handleNavigationClick(Player player, int slot) {
        Integer currentSlot = plugin.getBackpackManager().getOpenBackpackSlot(player);
        if (currentSlot == null) return;

        com.vaultpack.models.PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        switch (slot) {
            case 0: // Close button
                player.closeInventory();
                break;

            case 1: // Back to all backpacks
                player.closeInventory();
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        new com.vaultpack.gui.StorageMenuGUI(plugin).open(player);
                    }
                }, 1L);
                break;

            case 5: // First backpack
                int firstSlot = findFirstBackpack(data);
                if (firstSlot != -1 && firstSlot != currentSlot) {
                    player.closeInventory();
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getBackpackManager().openBackpack(player, firstSlot);
                        }
                    }, 1L);
                } else {
                    com.vaultpack.utils.ActionBarUtil.sendWarning(player, "Already at the first backpack!");
                }
                break;

            case 6: // Previous backpack
                int previousSlot = findPreviousBackpack(data, currentSlot);
                if (previousSlot != -1) {
                    player.closeInventory();
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getBackpackManager().openBackpack(player, previousSlot);
                        }
                    }, 1L);
                } else {
                    com.vaultpack.utils.ActionBarUtil.sendWarning(player, "No previous backpack!");
                }
                break;

            case 7: // Next backpack
                int nextSlot = findNextBackpack(data, currentSlot);
                if (nextSlot != -1) {
                    player.closeInventory();
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getBackpackManager().openBackpack(player, nextSlot);
                        }
                    }, 1L);
                } else {
                    com.vaultpack.utils.ActionBarUtil.sendWarning(player, "No next backpack!");
                }
                break;

            case 8: // Last backpack
                int lastSlot = findLastBackpack(data);
                if (lastSlot != -1 && lastSlot != currentSlot) {
                    player.closeInventory();
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getBackpackManager().openBackpack(player, lastSlot);
                        }
                    }, 1L);
                } else {
                    com.vaultpack.utils.ActionBarUtil.sendWarning(player, "Already at the last backpack!");
                }
                break;
        }
    }

    /**
     * Find the first backpack slot that is unlocked and has a backpack
     */
    private int findFirstBackpack(com.vaultpack.models.PlayerBackpackData data) {
        for (int i = 1; i <= 18; i++) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the last backpack slot that is unlocked and has a backpack
     */
    private int findLastBackpack(com.vaultpack.models.PlayerBackpackData data) {
        for (int i = 18; i >= 1; i--) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the previous backpack slot before the current one
     */
    private int findPreviousBackpack(com.vaultpack.models.PlayerBackpackData data, int currentSlot) {
        for (int i = currentSlot - 1; i >= 1; i--) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the next backpack slot after the current one
     */
    private int findNextBackpack(com.vaultpack.models.PlayerBackpackData data, int currentSlot) {
        for (int i = currentSlot + 1; i <= 18; i++) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Extract number from display name (e.g., "Ender Page 3" -> 3)
     */
    private int extractNumber(String text, String prefix) {
        try {
            if (text.contains("#")) {
                String[] parts = text.split("#");
                if (parts.length > 1) {
                    String afterHash = parts[1].trim();
                    String numberPart = afterHash.split(" ")[0];
                    return Integer.parseInt(numberPart);
                }
            } else {
                // Try to find number after prefix
                String[] words = text.split(" ");
                for (int i = 0; i < words.length; i++) {
                    if (words[i].contains(prefix) && i + 1 < words.length) {
                        try {
                            return Integer.parseInt(words[i + 1]);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
}
