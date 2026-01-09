package com.vaultpack.gui;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.config.MenuConfig;
import com.vaultpack.models.Backpack;
import com.vaultpack.models.EnderPage;
import com.vaultpack.models.PlayerBackpackData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the unified storage menu (menus/storage.yml)
 * Shows both ender chest pages and backpack slots
 */
public class StorageMenuGUI {

    private final VaultPackPlugin plugin;
    private final GUIBuilder builder;

    public StorageMenuGUI(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.builder = new GUIBuilder(plugin);
    }

    /**
     * Open the unified storage menu for a player
     */
    public void open(Player player) {
        // Fetch menu config dynamically to support reload
        MenuConfig menuConfig = plugin.getMenuManager().getMenu("storage");
        if (menuConfig == null) {
            plugin.getMessageManager().send(player, "data-load-error");
            return;
        }

        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // Build placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("unlocked_ender_pages", String.valueOf(data.getUnlockedEnderPages()));
        placeholders.put("unlocked_backpack_slots", String.valueOf(data.getUnlockedSlots()));
        placeholders.put("total_items", "0"); // TODO: Calculate total items
        placeholders.put("ender_total_slots", String.valueOf(data.getUnlockedEnderPages() * 45));

        // Build base menu
        Inventory inv = builder.buildMenu(menuConfig, player, placeholders);

        // Add dynamic ender chest pages
        addEnderChestPages(inv, player, data, menuConfig);

        // Add dynamic backpack slots
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
     * Add ender chest pages to the display slots
     */
    private void addEnderChestPages(Inventory inv, Player player, PlayerBackpackData data, MenuConfig menuConfig) {
        ConfigurationSection enderDisplay = menuConfig.getConfig().getConfigurationSection("enderchest-display");
        if (enderDisplay == null) return;

        List<Integer> slots = enderDisplay.getIntegerList("slots");
        int maxVisible = enderDisplay.getInt("max-visible", 7);
        int totalPages = enderDisplay.getInt("total-pages", 9);

        // Show first N pages
        for (int i = 0; i < Math.min(slots.size(), totalPages); i++) {
            int pageNumber = i + 1;
            int slot = slots.get(i);

            ItemStack item = createEnderPageItem(player, data, pageNumber, enderDisplay);
            inv.setItem(slot, item);
        }
    }

    /**
     * Create an ender page display item
     */
    private ItemStack createEnderPageItem(Player player, PlayerBackpackData data, int pageNumber, ConfigurationSection config) {
        boolean unlocked = data.isEnderPageUnlocked(pageNumber);
        EnderPage page = data.getEnderPage(pageNumber);

        ConfigurationSection itemConfig;
        Material material;
        String nameTemplate;
        List<String> loreTemplate;

        if (!unlocked) {
            // Locked page
            itemConfig = config.getConfigurationSection("locked");
            String materialName = itemConfig != null ? itemConfig.getString("item", "gray_dye") : "gray_dye";
            material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) material = Material.GRAY_DYE;
            nameTemplate = itemConfig != null ? itemConfig.getString("name", "&7Page %page% &c✖") : "&7Page %page% &c✖";
            loreTemplate = itemConfig != null ? itemConfig.getStringList("lore") : new ArrayList<>();
        } else if (page == null || page.isEmpty()) {
            // Unlocked but empty
            itemConfig = config.getConfigurationSection("unlocked-empty");
            String materialName = itemConfig != null ? itemConfig.getString("item", "ender_pearl") : "ender_pearl";
            material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) material = Material.ENDER_PEARL;
            nameTemplate = itemConfig != null ? itemConfig.getString("name", "&5Page %page% &a✓") : "&5Page %page% &a✓";
            loreTemplate = itemConfig != null ? itemConfig.getStringList("lore") : new ArrayList<>();
        } else {
            // Active with items
            itemConfig = config.getConfigurationSection("unlocked-active");
            String materialName = itemConfig != null ? itemConfig.getString("item", "ender_eye") : "ender_eye";
            material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) material = Material.ENDER_EYE;
            nameTemplate = itemConfig != null ? itemConfig.getString("name", "&5Page %page% &a✓") : "&5Page %page% &a✓";
            loreTemplate = itemConfig != null ? itemConfig.getStringList("lore") : new ArrayList<>();
        }

        // Build placeholders
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("page", String.valueOf(pageNumber));
        placeholders.put("cost", String.valueOf(plugin.getConfigManager().getEnderPageUnlockCost(pageNumber)));

        if (page != null) {
            placeholders.put("used", String.valueOf(page.getUsedSlots()));
            placeholders.put("fill_bar", page.getFullnessBar());
        } else {
            placeholders.put("used", "0");
            placeholders.put("fill_bar", "----------");
        }

        return builder.createDynamicItem(material.name(), nameTemplate, loreTemplate, player, placeholders);
    }

    /**
     * Add backpack slots to the display
     */
    private void addBackpackSlots(Inventory inv, Player player, PlayerBackpackData data, MenuConfig menuConfig) {
        ConfigurationSection backpackDisplay = menuConfig.getConfig().getConfigurationSection("backpack-display");
        if (backpackDisplay == null) return;

        List<Integer> slots = backpackDisplay.getIntegerList("slots");
        int maxVisible = backpackDisplay.getInt("max-visible", 7);

        // Show first N backpack slots
        for (int i = 0; i < Math.min(slots.size(), data.getUnlockedSlots()); i++) {
            int slotNumber = i + 1;
            int guiSlot = slots.get(i);

            ItemStack item = createBackpackSlotItem(player, data, slotNumber, backpackDisplay);
            inv.setItem(guiSlot, item);
        }

        // Show locked slots
        for (int i = data.getUnlockedSlots(); i < Math.min(slots.size(), 18); i++) {
            int slotNumber = i + 1;
            int guiSlot = slots.get(i);

            ItemStack item = createLockedBackpackSlotItem(player, slotNumber, backpackDisplay);
            inv.setItem(guiSlot, item);
        }
    }

    /**
     * Create a backpack slot display item
     */
    private ItemStack createBackpackSlotItem(Player player, PlayerBackpackData data, int slotNumber, ConfigurationSection config) {
        Backpack backpack = data.getBackpack(slotNumber);

        ConfigurationSection itemConfig;
        Material material;
        String nameTemplate;
        List<String> loreTemplate;

        if (backpack == null) {
            // Empty slot
            itemConfig = config.getConfigurationSection("empty");
            String materialName = itemConfig != null ? itemConfig.getString("item", "lime_dye") : "lime_dye";
            material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) material = Material.LIME_DYE;
            nameTemplate = itemConfig != null ? itemConfig.getString("name", "&aSlot %slot% &7✓") : "&aSlot %slot% &7✓";
            loreTemplate = itemConfig != null ? itemConfig.getStringList("lore") : new ArrayList<>();

            // Build placeholders
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("slot", String.valueOf(slotNumber));
            placeholders.put("cost", String.valueOf(plugin.getConfigManager().getSlotUnlockCost(slotNumber)));

            return builder.createDynamicItem(material.name(), nameTemplate, loreTemplate, player, placeholders);
        } else {
            // Active backpack - use the actual backpack type's item
            String backpackTypeId = backpack.getBackpackTypeId();
            com.vaultpack.types.BackpackType backpackType = plugin.getBackpackTypeManager().getBackpackType(backpackTypeId);

            if (backpackType != null) {
                // Create item using the backpack type (with proper material/texture)
                ItemStack backpackItem = plugin.getBackpackTypeManager().createBackpackItem(backpackType);
                ItemMeta meta = backpackItem.getItemMeta();

                if (meta != null) {
                    // Update lore with current backpack stats
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    List<String> updatedLore = new ArrayList<>();

                    for (String line : lore) {
                        updatedLore.add(line
                            .replace("%tier%", backpack.getTier().getDisplayName())
                            .replace("%size%", String.valueOf(backpack.getSize()))
                            .replace("%used%", String.valueOf(backpack.getUsedSlots()))
                        );
                    }

                    // Add status lore (config-driven)
                    updatedLore.add("");
                    updatedLore.add(plugin.getMessageManager().getMessage("gui.storage-backpack-status.placed",
                        "%slot%", String.valueOf(slotNumber)));
                    updatedLore.add(plugin.getMessageManager().getMessage("gui.storage-backpack-status.items",
                        "%used%", String.valueOf(backpack.getUsedSlots()),
                        "%size%", String.valueOf(backpack.getSize())));
                    if (!backpack.isEmpty()) {
                        updatedLore.add(plugin.getMessageManager().getMessage("gui.storage-backpack-status.contains-items"));
                    } else {
                        updatedLore.add(plugin.getMessageManager().getMessage("gui.storage-backpack-status.click-to-remove"));
                    }

                    meta.setLore(updatedLore);
                    backpackItem.setItemMeta(meta);
                }

                return backpackItem;
            } else {
                // Fallback if backpack type not found
                itemConfig = config.getConfigurationSection("active");
                String materialName = itemConfig != null ? itemConfig.getString("item", "chest") : "chest";
                material = Material.getMaterial(materialName.toUpperCase());
                if (material == null) material = Material.CHEST;
                nameTemplate = itemConfig != null ? itemConfig.getString("name", "&6BP %slot% &8[%tier%]") : "&6BP %slot% &8[%tier%]";
                loreTemplate = itemConfig != null ? itemConfig.getStringList("lore") : new ArrayList<>();

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("slot", String.valueOf(slotNumber));
                placeholders.put("tier", backpack.getTier().name());
                placeholders.put("tier_display", backpack.getTier().getDisplayName());
                placeholders.put("size", String.valueOf(backpack.getSize()));
                placeholders.put("used", String.valueOf(backpack.getUsedSlots()));

                return builder.createDynamicItem(material.name(), nameTemplate, loreTemplate, player, placeholders);
            }
        }
    }

    /**
     * Create a locked backpack slot item
     */
    private ItemStack createLockedBackpackSlotItem(Player player, int slotNumber, ConfigurationSection config) {
        ConfigurationSection itemConfig = config.getConfigurationSection("locked");
        String materialName = itemConfig != null ? itemConfig.getString("item", "gray_dye") : "gray_dye";
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material == null) material = Material.GRAY_DYE;
        String nameTemplate = itemConfig != null ? itemConfig.getString("name", "&7Slot %slot% &c✖") : "&7Slot %slot% &c✖";
        List<String> loreTemplate = itemConfig != null ? itemConfig.getStringList("lore") : new ArrayList<>();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("slot", String.valueOf(slotNumber));
        placeholders.put("cost", String.valueOf(plugin.getConfigManager().getSlotUnlockCost(slotNumber)));

        return builder.createDynamicItem(material.name(), nameTemplate, loreTemplate, player, placeholders);
    }

    /**
     * Handle click in this menu
     */
    public void handleClick(Player player, int slot) {
        // TODO: Implement click handling
        // - Check if clicked on ender page
        // - Check if clicked on backpack slot
        // - Execute appropriate action
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
