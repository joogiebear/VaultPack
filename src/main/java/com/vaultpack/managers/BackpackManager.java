package com.vaultpack.managers;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.models.Backpack;
import com.vaultpack.models.BackpackTier;
import com.vaultpack.models.GUIItem;
import com.vaultpack.models.PlayerBackpackData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BackpackManager {

    private final VaultPackPlugin plugin;
    private final Map<UUID, Integer> openBackpacks; // Player UUID -> Slot number
    private final Map<UUID, RemovalConfirmation> pendingRemovals; // Player UUID -> Removal confirmation

    public BackpackManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.openBackpacks = new HashMap<>();
        this.pendingRemovals = new HashMap<>();
    }

    private static class RemovalConfirmation {
        int slotNumber;
        long timestamp;

        RemovalConfirmation(int slotNumber) {
            this.slotNumber = slotNumber;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 10000; // 10 seconds
        }
    }

    public void openBackpack(Player player, int slotNumber) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // Check if slot is unlocked
        if (!data.isSlotUnlocked(slotNumber)) {
            player.sendMessage(ChatColor.RED + "This slot is locked!");
            return;
        }

        // Check if backpack exists in slot
        Backpack backpack = data.getBackpack(slotNumber);
        if (backpack == null) {
            player.sendMessage(ChatColor.RED + "No backpack in this slot!");
            return;
        }

        // Create inventory with extra row for navigation header
        int backpackRows = (int) Math.ceil(backpack.getSize() / 9.0);
        int totalRows = backpackRows + 1; // +1 for navigation header
        String title = ChatColor.translateAlternateColorCodes('&',
                "&8Backpack #" + slotNumber + " &7[" + backpack.getTier().getDisplayName() + "]");

        Inventory inventory = Bukkit.createInventory(null, totalRows * 9, title);

        // Add navigation header (row 1, slots 0-8)
        addNavigationHeader(inventory, player, slotNumber, data);

        // Load backpack contents (starting from row 2, slot 9)
        Map<Integer, ItemStack> contents = backpack.getContents();
        for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
            int backpackSlot = entry.getKey();
            ItemStack item = entry.getValue();

            if (backpackSlot >= 0 && backpackSlot < backpack.getSize() && item != null) {
                // Offset by 9 to skip navigation header
                inventory.setItem(backpackSlot + 9, item.clone());
            }
        }

        // Open inventory
        player.openInventory(inventory);
        backpack.setActiveInventory(inventory);
        openBackpacks.put(player.getUniqueId(), slotNumber);

        player.sendMessage(ChatColor.GREEN + "Opened backpack #" + slotNumber);
    }

    private void addNavigationHeader(Inventory inventory, Player player, int currentSlot, PlayerBackpackData data) {
        // Player head textures
        String firstTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQ3MDdkYjQ2YTVhY2JmZWJmNjEyMzk1MzZkMjU2NDgxMzRiYjQzYjY1YzE2NzE2YmEzMjljNmRiZjQxMiJ9fX0=";
        String previousTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTVkYTQ4NDcyNzI1ODIyNjViZGFjYTM2NzIzN2M5NjEyMmIxMzlmNGU1OTdmYmM2NjY3ZDNmYjc1ZmVhN2NmNiJ9fX0=";
        String nextTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjUyN2ViYWU5ZjE1MzE1NGE3ZWQ0OWM4OGMwMmI1YTlhOWNhN2NiMTYxOGQ5OTE0YTNkOWRmOGNjYjNjODQifX19";
        String lastTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWI3MThhYmUxMDI0NzYyYzFjNTIyNWM5YjNjZjk0M2EwMTRmNTRkZDhkNGQ0NjRhMmQ2MWYwOThjMDdkOWUifX19";

        // Slot 0: Close button
        inventory.setItem(0, createButton(org.bukkit.Material.BARRIER, "&c&lClose",
            "&7Click to close this backpack"));

        // Slot 1: Back to all backpacks
        inventory.setItem(1, createButton(org.bukkit.Material.CHEST, "&e&lAll Backpacks",
            "&7Return to backpack menu"));

        // Find first and last backpack slots
        int firstSlot = -1;
        int lastSlot = -1;
        for (int i = 1; i <= 18; i++) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                if (firstSlot == -1) firstSlot = i;
                lastSlot = i;
            }
        }

        // Slot 5: First backpack
        if (firstSlot != -1 && firstSlot != currentSlot) {
            inventory.setItem(5, createButtonWithTexture(firstTexture, "&a&lFirst Backpack",
                "&7Jump to backpack #" + firstSlot));
        } else {
            inventory.setItem(5, createButton(org.bukkit.Material.GRAY_DYE, "&7First Backpack",
                "&cAlready at first or only backpack"));
        }

        // Slot 6: Previous backpack
        int previousSlot = findPreviousBackpack(data, currentSlot);
        if (previousSlot != -1) {
            inventory.setItem(6, createButtonWithTexture(previousTexture, "&e&lPrevious",
                "&7Go to backpack #" + previousSlot));
        } else {
            inventory.setItem(6, createButton(org.bukkit.Material.GRAY_DYE, "&7Previous",
                "&cNo previous backpack"));
        }

        // Slot 7: Next backpack
        int nextSlot = findNextBackpack(data, currentSlot);
        if (nextSlot != -1) {
            inventory.setItem(7, createButtonWithTexture(nextTexture, "&e&lNext",
                "&7Go to backpack #" + nextSlot));
        } else {
            inventory.setItem(7, createButton(org.bukkit.Material.GRAY_DYE, "&7Next",
                "&cNo next backpack"));
        }

        // Slot 8: Last backpack
        if (lastSlot != -1 && lastSlot != currentSlot) {
            inventory.setItem(8, createButtonWithTexture(lastTexture, "&a&lLast Backpack",
                "&7Jump to backpack #" + lastSlot));
        } else {
            inventory.setItem(8, createButton(org.bukkit.Material.GRAY_DYE, "&7Last Backpack",
                "&cAlready at last or only backpack"));
        }
    }

    private ItemStack createButton(org.bukkit.Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createButtonWithTexture(String texture, String name, String... lore) {
        ItemStack item = new ItemStack(org.bukkit.Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(loreList);

            // Apply custom texture
            applyTexture(meta, texture);

            item.setItemMeta(meta);
        }

        return item;
    }

    private int findPreviousBackpack(PlayerBackpackData data, int currentSlot) {
        for (int i = currentSlot - 1; i >= 1; i--) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                return i;
            }
        }
        return -1;
    }

    private int findNextBackpack(PlayerBackpackData data, int currentSlot) {
        for (int i = currentSlot + 1; i <= 18; i++) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                return i;
            }
        }
        return -1;
    }

    public void closeBackpack(Player player) {
        Integer slotNumber = openBackpacks.remove(player.getUniqueId());

        if (slotNumber != null) {
            saveBackpackContents(player, slotNumber);
        }
    }

    private void saveBackpackContents(Player player, int slotNumber) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        Backpack backpack = data.getBackpack(slotNumber);

        if (backpack != null && backpack.getActiveInventory() != null) {
            Inventory inventory = backpack.getActiveInventory();
            Map<Integer, ItemStack> contents = new HashMap<>();

            // Read from slot i+9 (offset by navigation header) and save to slot i
            for (int i = 0; i < backpack.getSize(); i++) {
                ItemStack item = inventory.getItem(i + 9); // +9 to skip navigation header
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    contents.put(i, item.clone());
                }
            }

            backpack.setContents(contents);
            backpack.setActiveInventory(null);

            // Save to file
            plugin.getDataManager().savePlayerData(player.getUniqueId());
        }
    }

    public boolean isBackpackOpen(Player player) {
        return openBackpacks.containsKey(player.getUniqueId());
    }

    public Integer getOpenBackpackSlot(Player player) {
        return openBackpacks.get(player.getUniqueId());
    }

    public void createBackpack(Player player, int slotNumber, BackpackTier tier) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        Backpack backpack = new Backpack(player.getUniqueId(), slotNumber, tier);
        data.setBackpack(slotNumber, backpack);

        plugin.getDataManager().savePlayerData(player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "Backpack placed in slot #" + slotNumber + "!");
    }

    public void createBackpack(Player player, int slotNumber, BackpackTier tier, String backpackTypeId) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        Backpack backpack = new Backpack(player.getUniqueId(), slotNumber, tier, backpackTypeId);
        data.setBackpack(slotNumber, backpack);

        plugin.getDataManager().savePlayerData(player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "Backpack placed in slot #" + slotNumber + "!");
    }

    public void removeBackpack(Player player, int slotNumber) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        Backpack backpack = data.getBackpack(slotNumber);

        if (backpack == null) {
            player.sendMessage(ChatColor.RED + "No backpack in this slot!");
            return;
        }

        // Check for pending confirmation
        RemovalConfirmation pending = pendingRemovals.get(player.getUniqueId());
        if (pending != null && !pending.isExpired() && pending.slotNumber == slotNumber) {
            // Confirmed - proceed with removal
            pendingRemovals.remove(player.getUniqueId());

            // Close if open
            if (isBackpackOpen(player) && getOpenBackpackSlot(player) == slotNumber) {
                player.closeInventory();
            }

            // Drop all items on the ground
            Map<Integer, ItemStack> contents = backpack.getContents();
            int itemCount = 0;
            for (ItemStack item : contents.values()) {
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                    itemCount++;
                }
            }

            // Remove backpack
            data.removeBackpack(slotNumber);
            plugin.getDataManager().savePlayerData(player.getUniqueId());

            player.sendMessage(ChatColor.GREEN + "Backpack removed from slot #" + slotNumber + "!");
            if (itemCount > 0) {
                player.sendMessage(ChatColor.YELLOW + "" + itemCount + " item(s) dropped on the ground!");
            }
        } else {
            // First click - ask for confirmation
            pendingRemovals.put(player.getUniqueId(), new RemovalConfirmation(slotNumber));

            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "⚠ WARNING ⚠");
            player.sendMessage(ChatColor.YELLOW + "Right-click again to confirm backpack removal!");
            player.sendMessage(ChatColor.GRAY + "All items will be dropped on the ground.");
            player.sendMessage(ChatColor.GRAY + "This action cannot be undone!");
            player.sendMessage(ChatColor.DARK_GRAY + "(Confirmation expires in 10 seconds)");
        }
    }

    public void upgradeBackpack(Player player, int slotNumber) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        Backpack backpack = data.getBackpack(slotNumber);

        if (backpack == null) {
            player.sendMessage(ChatColor.RED + "No backpack in this slot!");
            return;
        }

        if (!backpack.canUpgrade()) {
            player.sendMessage(ChatColor.RED + "This backpack is already at maximum size!");
            return;
        }

        BackpackTier oldTier = backpack.getTier();
        BackpackTier newTier = backpack.getNextTier();

        // Check cost
        int cost = plugin.getConfigManager().getUpgradeCost(oldTier, newTier);
        if (cost > 0 && plugin.isVaultEnabled()) {
            if (!plugin.getEconomyManager().hasMoney(player, cost)) {
                player.sendMessage(ChatColor.RED + "You don't have enough money! Need: $" + cost);
                return;
            }

            plugin.getEconomyManager().takeMoney(player, cost);
        }

        // Upgrade
        backpack.upgrade();
        plugin.getDataManager().savePlayerData(player.getUniqueId());

        player.sendMessage(ChatColor.GREEN + "Backpack upgraded from " + oldTier.getDisplayName() +
                " to " + newTier.getDisplayName() + "!");
    }

    public void unlockSlot(Player player, int slotNumber) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (data.isSlotUnlocked(slotNumber)) {
            player.sendMessage(ChatColor.RED + "This slot is already unlocked!");
            return;
        }

        // Check permission first
        if (plugin.getConfigManager().usePermissions()) {
            String permission = plugin.getConfigManager().getSlotPermission(slotNumber);
            if (player.hasPermission(permission)) {
                data.unlockSlot(slotNumber);
                plugin.getDataManager().savePlayerData(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Slot #" + slotNumber + " unlocked!");
                return;
            }
        }

        // Check economy cost
        if (plugin.getConfigManager().useEconomy() && plugin.isVaultEnabled()) {
            int cost = plugin.getConfigManager().getSlotUnlockCost(slotNumber);

            if (!plugin.getEconomyManager().hasMoney(player, cost)) {
                player.sendMessage(ChatColor.RED + "You don't have enough money! Need: $" + cost);
                return;
            }

            plugin.getEconomyManager().takeMoney(player, cost);
            data.unlockSlot(slotNumber);
            plugin.getDataManager().savePlayerData(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Slot #" + slotNumber + " unlocked for $" + cost + "!");
        } else {
            player.sendMessage(ChatColor.RED + "You don't have permission to unlock this slot!");
        }
    }

    /**
     * v2.0.0: Create an ItemStack from a GUI item config with placeholder replacement
     */
    private ItemStack createCustomGUIItem(GUIItem guiItem, Player player, PlayerBackpackData data) {
        ItemStack item = new ItemStack(guiItem.getMaterial());
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        // Set display name with placeholders
        String name = ChatColor.translateAlternateColorCodes('&', guiItem.getName());
        name = replacePlaceholders(name, player, data);
        meta.setDisplayName(name);

        // Set lore with placeholders
        List<String> lore = new ArrayList<>();
        for (String line : guiItem.getLore()) {
            String formatted = ChatColor.translateAlternateColorCodes('&', line);
            formatted = replacePlaceholders(formatted, player, data);
            lore.add(formatted);
        }
        meta.setLore(lore);

        // Add glow effect
        if (guiItem.hasGlow()) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Set custom model data
        if (guiItem.getCustomModelData() > 0) {
            meta.setCustomModelData(guiItem.getCustomModelData());
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * v2.0.0: Replace placeholders in GUI text
     */
    private String replacePlaceholders(String text, Player player, PlayerBackpackData data) {
        return text
                .replace("%player%", player.getName())
                .replace("%backpack_used%", String.valueOf(data.getTotalUsedSlots()))
                .replace("%backpack_total%", String.valueOf(data.getTotalStorageSlots()))
                .replace("%ender_used%", String.valueOf(data.getTotalUsedEnderSlots()))
                .replace("%ender_total%", String.valueOf(data.getTotalEnderStorageSlots()))
                .replace("%unlocked_backpacks%", String.valueOf(data.getUnlockedSlots()))
                .replace("%unlocked_ender%", String.valueOf(data.getUnlockedEnderPages()));
    }

    /**
     * v1.0.0: Open the unified storage GUI (both backpacks and ender chest)
     * Now uses the new menu system from menus/storage.yml
     */
    public void openUnifiedStorageGUI(Player player) {
        // TODO: Implement with new MenuManager and GUIBuilder
        // For now, just open the main backpack menu
        openBackpackMenu(player);
    }

    /**
     * Create an ender page slot item for the GUI
     */
    private ItemStack createEnderPageSlotItem(Player player, PlayerBackpackData data, int pageNumber) {
        boolean unlocked = data.isEnderPageUnlocked(pageNumber);
        boolean hasPage = data.hasEnderPage(pageNumber);

        ItemStack item;
        java.util.List<String> lore = new java.util.ArrayList<>();

        if (!unlocked) {
            // Locked page
            item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.RED_STAINED_GLASS_PANE);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Ender Page " + pageNumber + " " + ChatColor.RED + "[LOCKED]");

            int cost = plugin.getConfigManager().getEnderPageUnlockCost(pageNumber);
            String perm = plugin.getConfigManager().getEnderPagePermission(pageNumber);

            lore.add(ChatColor.GRAY + "This page is locked.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Economy unlock: " + ChatColor.GREEN + "$" + cost);
            lore.add(ChatColor.YELLOW + "Permission: " + ChatColor.WHITE + perm);
            lore.add("");
            lore.add(ChatColor.GRAY + "Click to unlock!");

            meta.setLore(lore);
            item.setItemMeta(meta);
        } else if (!hasPage) {
            // Empty page
            item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PURPLE_STAINED_GLASS_PANE);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Ender Page " + pageNumber + " " + ChatColor.GRAY + "[EMPTY]");

            lore.add(ChatColor.GRAY + "This page is unlocked!");
            lore.add(ChatColor.GRAY + "Capacity: " + ChatColor.AQUA + "45 slots");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to open");

            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            // Active page
            com.vaultpack.models.EnderPage page = data.getEnderPage(pageNumber);
            item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ENDER_EYE);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Ender Page " + pageNumber);

            lore.add("");
            lore.add(ChatColor.GRAY + "Capacity: " + ChatColor.AQUA + "45 slots");
            lore.add(ChatColor.GRAY + "Used: " + ChatColor.GREEN + page.getUsedSlots() + ChatColor.GRAY + "/" + ChatColor.AQUA + "45");
            lore.add(ChatColor.GRAY + "Fullness: " + ChatColor.translateAlternateColorCodes('&', page.getFullnessBar()));
            lore.add("");
            lore.add(ChatColor.YELLOW + "» " + ChatColor.GRAY + "Click to open");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Legacy method - redirects to unified GUI
     */
    public void openBackpackMenu(Player player) {
        openUnifiedStorageGUI(player);
    }

    /**
     * Old backpack menu (kept for compatibility)
     */
    public void openLegacyBackpackMenu(Player player) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // Create main backpack selection GUI
        int rows = 6;
        String title = ChatColor.translateAlternateColorCodes('&', "&8Backpacks");
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);

        // Fill with border
        ItemStack border = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);

        // Top and bottom borders
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        // Side borders
        for (int row = 1; row < 5; row++) {
            inventory.setItem(row * 9, border);
            inventory.setItem(row * 9 + 8, border);
        }

        // Add backpack slots (18 slots in positions)
        int[][] slotPositions = {
            {10, 11, 12, 13, 14, 15},  // Row 2
            {19, 20, 21, 22, 23, 24},  // Row 3
            {28, 29, 30, 31, 32, 33}   // Row 4
        };

        int slotNumber = 1;
        for (int[] row : slotPositions) {
            for (int pos : row) {
                if (slotNumber <= 18) {
                    inventory.setItem(pos, createSlotItem(player, data, slotNumber));
                    slotNumber++;
                }
            }
        }

        // Info button
        ItemStack info = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOOK);
        org.bukkit.inventory.meta.ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Backpack Info");
        java.util.List<String> infoLore = new java.util.ArrayList<>();
        infoLore.add(ChatColor.GRAY + "Total Slots: " + ChatColor.YELLOW + data.getUnlockedSlots());
        infoLore.add(ChatColor.GRAY + "Active Backpacks: " + ChatColor.GREEN + data.getActiveBackpackCount());
        infoLore.add(ChatColor.GRAY + "Total Storage: " + ChatColor.YELLOW + data.getTotalStorageSlots() + " slots");
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "Backpacks provide extra");
        infoLore.add(ChatColor.GRAY + "storage that you can access");
        infoLore.add(ChatColor.GRAY + "anywhere, anytime!");
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inventory.setItem(49, info);

        // Close button
        ItemStack close = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Close");
        java.util.List<String> closeLore = new java.util.ArrayList<>();
        closeLore.add(ChatColor.GRAY + "Click to close");
        closeMeta.setLore(closeLore);
        close.setItemMeta(closeMeta);
        inventory.setItem(53, close);

        player.openInventory(inventory);
    }

    private ItemStack createSlotItem(Player player, PlayerBackpackData data, int slotNumber) {
        boolean unlocked = data.isSlotUnlocked(slotNumber);
        boolean hasBackpack = data.hasBackpack(slotNumber);

        ItemStack item;
        java.util.List<String> lore = new java.util.ArrayList<>();

        if (!unlocked) {
            // Locked slot - gray dye
            item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_DYE);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GRAY + "Backpack Slot #" + slotNumber + " " + ChatColor.RED + "[LOCKED]");

            int cost = plugin.getConfigManager().getSlotUnlockCost(slotNumber);
            String perm = plugin.getConfigManager().getSlotPermission(slotNumber);

            lore.add(ChatColor.GRAY + "This slot is locked.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Economy unlock: " + ChatColor.GREEN + "$" + cost);
            lore.add(ChatColor.YELLOW + "Permission: " + ChatColor.WHITE + perm);
            lore.add("");
            lore.add(ChatColor.GRAY + "Purchase upgrades or gain");
            lore.add(ChatColor.GRAY + "permissions to unlock!");

            meta.setLore(lore);
            item.setItemMeta(meta);
        } else if (!hasBackpack) {
            // Empty slot - lime dye
            item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.LIME_DYE);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "Backpack Slot #" + slotNumber + " " + ChatColor.GRAY + "[EMPTY]");

            lore.add(ChatColor.GRAY + "This slot is unlocked!");
            lore.add("");
            lore.add(ChatColor.GRAY + "Hold a backpack item and");
            lore.add(ChatColor.GRAY + "click here to place it.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Left-Click " + ChatColor.GRAY + "to place backpack");

            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            // Active backpack - use custom texture if available
            Backpack backpack = data.getBackpack(slotNumber);

            // Try to get backpack type for custom texture
            String backpackTypeId = backpack.getBackpackTypeId();
            com.vaultpack.types.BackpackType backpackType = null;
            if (backpackTypeId != null) {
                backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);
            }

            // Use custom texture if available, otherwise default to chest
            if (backpackType != null && backpackType.getMaterial() == org.bukkit.Material.PLAYER_HEAD && backpackType.hasTexture()) {
                item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
                org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
                applyTexture(skullMeta, backpackType.getTexture());
                skullMeta.setDisplayName(ChatColor.GOLD + "Backpack #" + slotNumber);
                item.setItemMeta(skullMeta);
            } else {
                item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHEST);
            }

            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta.getDisplayName() == null || meta.getDisplayName().isEmpty()) {
                meta.setDisplayName(ChatColor.GOLD + "Backpack #" + slotNumber);
            }

            // Get rarity from backpack type if available
            String rarity = ChatColor.GRAY + "Unknown";
            if (backpackType != null) {
                BackpackTier tier = backpack.getTier();
                rarity = ChatColor.translateAlternateColorCodes('&', tier.getColorCode()) + tier.getDisplayName();

                // Add rarity label based on tier
                if (tier == BackpackTier.SMALL) {
                    rarity = ChatColor.GREEN + "Uncommon";
                } else if (tier == BackpackTier.MEDIUM) {
                    rarity = ChatColor.BLUE + "Rare";
                } else if (tier == BackpackTier.LARGE || tier == BackpackTier.GREATER) {
                    rarity = ChatColor.DARK_PURPLE + "Epic";
                } else if (tier == BackpackTier.JUMBO) {
                    rarity = ChatColor.GOLD + "" + ChatColor.BOLD + "Legendary";
                }
            }

            lore.add("");
            lore.add(ChatColor.GRAY + "Rarity: " + rarity);
            lore.add(ChatColor.GRAY + "Tier: " + ChatColor.translateAlternateColorCodes('&', backpack.getTier().getColorCode()) + backpack.getTier().getDisplayName());
            lore.add(ChatColor.GRAY + "Capacity: " + ChatColor.YELLOW + backpack.getSize() + " slots");
            lore.add(ChatColor.GRAY + "Used: " + ChatColor.GREEN + backpack.getUsedSlots() + ChatColor.GRAY + "/" + ChatColor.YELLOW + backpack.getSize());
            lore.add(ChatColor.GRAY + "Fullness: " + ChatColor.translateAlternateColorCodes('&', backpack.getFullnessBar()));
            lore.add("");
            lore.add(ChatColor.YELLOW + "» " + ChatColor.GRAY + "Left-Click to open");
            lore.add(ChatColor.YELLOW + "» " + ChatColor.GRAY + "Right-Click twice to remove");
            lore.add(ChatColor.YELLOW + "» " + ChatColor.GRAY + "Shift-Click to upgrade tier");
            lore.add(ChatColor.YELLOW + "» " + ChatColor.GRAY + "Click with larger to swap");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public void closeAllBackpacks() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isBackpackOpen(player)) {
                player.closeInventory();
            }
        }
        openBackpacks.clear();
    }

    public boolean hasPendingRemoval(Player player, int slotNumber) {
        RemovalConfirmation pending = pendingRemovals.get(player.getUniqueId());
        return pending != null && !pending.isExpired() && pending.slotNumber == slotNumber;
    }

    private void applyTexture(org.bukkit.inventory.meta.SkullMeta skullMeta, String texture) {
        try {
            // Use Bukkit's profile API if available (Paper 1.18.2+)
            org.bukkit.profile.PlayerProfile profile = org.bukkit.Bukkit.createPlayerProfile(java.util.UUID.randomUUID());
            org.bukkit.profile.PlayerTextures textures = profile.getTextures();

            // Decode the base64 texture to get the URL
            String decoded = new String(java.util.Base64.getDecoder().decode(texture));
            String url = decoded.substring(decoded.indexOf("\"url\":\"") + 7, decoded.lastIndexOf("\""));

            textures.setSkin(new java.net.URL(url));
            profile.setTextures(textures);
            skullMeta.setOwnerProfile(profile);
        } catch (Exception e) {
            // Fallback to reflection method for older versions
            try {
                Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

                Object profile = gameProfileClass.getConstructor(java.util.UUID.class, String.class)
                        .newInstance(java.util.UUID.randomUUID(), null);
                Object properties = gameProfileClass.getMethod("getProperties").invoke(profile);
                Object property = propertyClass.getConstructor(String.class, String.class)
                        .newInstance("textures", texture);

                properties.getClass().getMethod("put", Object.class, Object.class)
                        .invoke(properties, "textures", property);

                java.lang.reflect.Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(skullMeta, profile);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to apply texture: " + ex.getMessage());
            }
        }
    }
}
