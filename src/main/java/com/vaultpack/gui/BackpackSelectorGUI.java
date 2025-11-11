package com.vaultpack.gui;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.config.MenuConfig;
import com.vaultpack.models.Backpack;
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
 * Handles the backpack selector menu (menus/backpack_selector.yml)
 * Shows all 18 backpack slots in a grid
 */
public class BackpackSelectorGUI {

    private final VaultPackPlugin plugin;
    private final GUIBuilder builder;

    public BackpackSelectorGUI(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.builder = new GUIBuilder(plugin);
    }

    /**
     * Open the backpack selector menu for a player
     */
    public void open(Player player) {
        // Fetch menu config dynamically to support reload
        MenuConfig menuConfig = plugin.getMenuManager().getMenu("backpack_selector");
        if (menuConfig == null) {
            player.sendMessage(ChatColor.RED + "Backpack menu not configured!");
            return;
        }

        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // Build placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("unlocked", String.valueOf(data.getUnlockedSlots()));
        placeholders.put("total_items", "0"); // TODO: Calculate
        placeholders.put("total_capacity", "0"); // TODO: Calculate

        // Build base menu
        Inventory inv = builder.buildMenu(menuConfig, player, placeholders);

        // Add all 18 backpack slots
        addBackpackSlots(inv, player, data, menuConfig);

        // Open for player
        player.openInventory(inv);

        // Play sound
        String openSound = menuConfig.getSound("open");
        if (openSound != null) {
            playSound(player, openSound);
        }
    }

    /**
     * Add all backpack slots to the menu
     */
    private void addBackpackSlots(Inventory inv, Player player, PlayerBackpackData data, MenuConfig menuConfig) {
        ConfigurationSection slotsSection = menuConfig.getConfig().getConfigurationSection("backpack-slots");
        if (slotsSection == null) return;

        ConfigurationSection lockedConfig = slotsSection.getConfigurationSection("locked");
        ConfigurationSection emptyConfig = slotsSection.getConfigurationSection("empty");
        ConfigurationSection activeConfig = slotsSection.getConfigurationSection("active");

        // Process all 18 slots
        for (int slotNumber = 1; slotNumber <= 18; slotNumber++) {
            String slotKey = "slot-" + slotNumber;
            ConfigurationSection slotSection = slotsSection.getConfigurationSection(slotKey);

            if (slotSection == null) continue;

            int guiSlot = slotSection.getInt("slot", -1);
            if (guiSlot == -1) continue;

            boolean unlocked = slotNumber <= data.getUnlockedSlots();
            Backpack backpack = data.getBackpack(slotNumber);

            ItemStack item;

            if (!unlocked) {
                // Locked slot
                item = createLockedSlotItem(player, slotNumber, lockedConfig);
            } else if (backpack == null) {
                // Empty slot
                item = createEmptySlotItem(player, slotNumber, emptyConfig);
            } else {
                // Active backpack
                item = createActiveSlotItem(player, slotNumber, backpack, activeConfig);
            }

            inv.setItem(guiSlot, item);
        }
    }

    /**
     * Create a locked slot item
     */
    private ItemStack createLockedSlotItem(Player player, int slotNumber, ConfigurationSection config) {
        String material = config.getString("item", "gray_dye");
        String name = config.getString("name", "&7Slot %slot% &c✖ LOCKED");
        List<String> lore = config.getStringList("lore");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("slot", String.valueOf(slotNumber));
        placeholders.put("cost", String.valueOf(plugin.getConfigManager().getSlotUnlockCost(slotNumber)));

        return builder.createDynamicItem(material, name, lore, player, placeholders);
    }

    /**
     * Create an empty slot item
     */
    private ItemStack createEmptySlotItem(Player player, int slotNumber, ConfigurationSection config) {
        String material = config.getString("item", "lime_dye");
        String name = config.getString("name", "&aSlot %slot% &7✓ EMPTY");
        List<String> lore = config.getStringList("lore");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("slot", String.valueOf(slotNumber));

        return builder.createDynamicItem(material, name, lore, player, placeholders);
    }

    /**
     * Create an active backpack slot item
     */
    private ItemStack createActiveSlotItem(Player player, int slotNumber, Backpack backpack, ConfigurationSection config) {
        // Get the backpack type and create the actual backpack item with texture
        String backpackTypeId = backpack.getBackpackTypeId();
        com.vaultpack.types.BackpackType backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);

        if (backpackType == null) {
            // Fallback to chest if type not found
            String material = Material.CHEST.name();
            String name = config.getString("name", "&6Backpack %slot% &8[%tier%]");
            List<String> lore = config.getStringList("lore");

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("slot", String.valueOf(slotNumber));
            placeholders.put("tier", backpack.getTier().name());
            placeholders.put("tier_display", backpack.getTier().getDisplayName());
            placeholders.put("size", String.valueOf(backpack.getSize()));
            placeholders.put("rows", String.valueOf(backpack.getSize() / 9));
            placeholders.put("used", String.valueOf(backpack.getUsedSlots()));

            return builder.createDynamicItem(material, name, lore, player, placeholders);
        }

        // Create the base backpack item with proper material and texture
        ItemStack backpackItem = plugin.getBackpackTypeManager().createBackpackItem(backpackType);
        org.bukkit.inventory.meta.ItemMeta meta = backpackItem.getItemMeta();

        if (meta != null) {
            // Update lore with current backpack stats
            java.util.List<String> updatedLore = new java.util.ArrayList<>();

            // Get lore from config or use default from backpack type
            List<String> configLore = config.getStringList("lore");
            List<String> baseLore = configLore.isEmpty() ? backpackType.getLore() : configLore;

            for (String line : baseLore) {
                String updated = ChatColor.translateAlternateColorCodes('&', line)
                    .replace("%slot%", String.valueOf(slotNumber))
                    .replace("%tier%", backpack.getTier().name())
                    .replace("%tier_display%", backpack.getTier().getDisplayName())
                    .replace("%size%", String.valueOf(backpack.getSize()))
                    .replace("%rows%", String.valueOf(backpack.getSize() / 9))
                    .replace("%used%", String.valueOf(backpack.getUsedSlots()));

                // Calculate fill percentage
                int fillPercent = backpack.getSize() > 0 ? (backpack.getUsedSlots() * 100) / backpack.getSize() : 0;
                updated = updated
                    .replace("%fill_percent%", String.valueOf(fillPercent))
                    .replace("%fill_bar%", backpack.getFullnessBar());

                updatedLore.add(updated);
            }

            // Add status information
            updatedLore.add("");
            updatedLore.add(ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Placed in slot #" + slotNumber);
            if (!backpack.isEmpty()) {
                updatedLore.add(ChatColor.RED + "" + ChatColor.BOLD + "Contains items - Cannot remove!");
            } else {
                updatedLore.add(ChatColor.YELLOW + "Right-click to remove (empty)");
            }

            meta.setLore(updatedLore);
            backpackItem.setItemMeta(meta);
        }

        return backpackItem;
    }

    /**
     * Handle click in this menu
     */
    public void handleClick(Player player, int slot, boolean leftClick, boolean rightClick, boolean shiftClick, org.bukkit.inventory.ItemStack cursorItem) {
        // Fetch menu config dynamically to support reload
        MenuConfig menuConfig = plugin.getMenuManager().getMenu("backpack_selector");
        if (menuConfig == null) return;

        // Get the slot number from the GUI slot
        int slotNumber = getSlotNumberFromGUISlot(slot, menuConfig);
        if (slotNumber == -1) return;

        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        boolean unlocked = slotNumber <= data.getUnlockedSlots();
        Backpack backpack = data.getBackpack(slotNumber);

        if (!unlocked) {
            // Try to unlock the slot
            handleUnlockClick(player, slotNumber);
        } else if (backpack == null) {
            // Empty slot - try to place backpack from cursor
            if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                handlePlaceBackpack(player, slotNumber, cursorItem);
            } else {
                player.sendMessage(ChatColor.YELLOW + "Hold a backpack item and click to place it in slot " + slotNumber);
            }
        } else {
            // Active backpack
            if (leftClick) {
                // Open backpack
                plugin.getBackpackManager().openBackpack(player, slotNumber);
                playSound(player, menuConfig.getSound("click"));
            } else if (rightClick) {
                // Remove backpack
                handleRemoveBackpack(player, slotNumber);
            } else if (shiftClick) {
                // Upgrade backpack
                handleUpgradeBackpack(player, slotNumber);
            }
        }
    }

    /**
     * Map GUI slot to backpack slot number
     */
    private int getSlotNumberFromGUISlot(int guiSlot, MenuConfig menuConfig) {
        ConfigurationSection slotsSection = menuConfig.getConfig().getConfigurationSection("backpack-slots");
        if (slotsSection == null) return -1;

        for (int slotNumber = 1; slotNumber <= 18; slotNumber++) {
            String slotKey = "slot-" + slotNumber;
            ConfigurationSection slotSection = slotsSection.getConfigurationSection(slotKey);
            if (slotSection != null && slotSection.getInt("slot", -1) == guiSlot) {
                return slotNumber;
            }
        }

        return -1;
    }

    /**
     * Handle unlock slot click
     */
    private void handleUnlockClick(Player player, int slotNumber) {
        plugin.getBackpackManager().unlockSlot(player, slotNumber);
        // Refresh menu
        open(player);
    }

    /**
     * Handle place backpack from cursor
     */
    private void handlePlaceBackpack(Player player, int slotNumber, ItemStack cursorItem) {
        // Check if item is a backpack
        if (!isBackpackItem(cursorItem)) {
            player.sendMessage(ChatColor.RED + "You can only place backpack items in these slots!");
            return;
        }

        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // Check if slot already has a backpack
        if (data.hasBackpack(slotNumber)) {
            player.sendMessage(ChatColor.RED + "This slot already has a backpack! Remove it first.");
            return;
        }

        // Place the backpack
        try {
            // Get backpack type from NBT
            org.bukkit.inventory.meta.ItemMeta meta = cursorItem.getItemMeta();
            if (meta == null) return;

            org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "backpack_type");

            if (!container.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                player.sendMessage(ChatColor.RED + "This backpack item is invalid!");
                return;
            }

            String backpackTypeId = container.get(key, org.bukkit.persistence.PersistentDataType.STRING);
            com.vaultpack.types.BackpackType backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);

            if (backpackType == null) {
                player.sendMessage(ChatColor.RED + "Unknown backpack type: " + backpackTypeId);
                return;
            }

            // Create new backpack and place it
            com.vaultpack.models.BackpackTier tier = com.vaultpack.models.BackpackTier.valueOf(backpackType.getDefaultTier().getName().toUpperCase());
            Backpack backpack = new Backpack(
                player.getUniqueId(),
                slotNumber,
                tier,
                backpackTypeId
            );
            data.setBackpack(slotNumber, backpack);
            plugin.getDataManager().savePlayerData(player.getUniqueId());

            // Remove item from cursor
            player.setItemOnCursor(null);

            // Refresh menu
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    open(player);
                }
            }, 1L);

            player.sendMessage(ChatColor.GREEN + "Placed " + ChatColor.translateAlternateColorCodes('&', backpackType.getDisplayName()) + ChatColor.GREEN + " in slot #" + slotNumber + "!");
            playSound(player, "ENTITY_ITEM_PICKUP");

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to place backpack: " + e.getMessage());
            plugin.getLogger().warning("Failed to place backpack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if an item is a backpack
     */
    private boolean isBackpackItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "backpack_type");

        return container.has(key, org.bukkit.persistence.PersistentDataType.STRING);
    }

    /**
     * Handle remove backpack
     */
    private void handleRemoveBackpack(Player player, int slotNumber) {
        plugin.getBackpackManager().removeBackpack(player, slotNumber);
        // Refresh menu
        open(player);
    }

    /**
     * Handle upgrade backpack
     */
    private void handleUpgradeBackpack(Player player, int slotNumber) {
        plugin.getBackpackManager().upgradeBackpack(player, slotNumber);
        // Refresh menu
        open(player);
    }

    /**
     * Play sound
     */
    private void playSound(Player player, String soundName) {
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            // Ignore invalid sound
        }
    }
}
