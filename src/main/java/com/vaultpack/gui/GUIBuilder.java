package com.vaultpack.gui;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.config.MenuConfig;
import com.vaultpack.models.GUIItem;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds GUI inventories from MenuConfig
 * Supports EcoMenus-style slot-based layouts
 */
public class GUIBuilder {

    private final VaultPackPlugin plugin;

    public GUIBuilder(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Build a basic menu inventory from config
     * @param menu The menu configuration
     * @param player The player (for placeholders)
     * @param placeholders Additional placeholders to replace
     * @return The built inventory
     */
    public Inventory buildMenu(MenuConfig menu, Player player, Map<String, String> placeholders) {
        // Create inventory
        String title = processPlaceholders(menu.getTitle(), player, placeholders);
        Inventory inv = Bukkit.createInventory(null, menu.getRows() * 9, title);

        // First, fill mask items
        fillMask(inv, menu, player, placeholders);

        // Then, add custom slots on top
        fillCustomSlots(inv, menu, player, placeholders);

        return inv;
    }

    /**
     * Fill the mask pattern
     */
    private void fillMask(Inventory inv, MenuConfig menu, Player player, Map<String, String> placeholders) {
        for (int slot = 0; slot < inv.getSize(); slot++) {
            String material = menu.getMaskMaterial(slot);
            if (material != null && !material.equals("0")) {
                ItemStack item = createItem(material, " ", new ArrayList<>(), false, null);
                if (item != null) {
                    inv.setItem(slot, item);
                }
            }
        }
    }

    /**
     * Fill custom slots (buttons, decorations, etc.)
     */
    private void fillCustomSlots(Inventory inv, MenuConfig menu, Player player, Map<String, String> placeholders) {
        for (Map.Entry<Integer, MenuConfig.SlotConfig> entry : menu.getCustomSlots().entrySet()) {
            int slot = entry.getKey();
            MenuConfig.SlotConfig config = entry.getValue();

            // Process placeholders
            String name = processPlaceholders(config.name, player, placeholders);
            List<String> lore = processPlaceholdersList(config.lore, player, placeholders);

            // Create item
            ItemStack item = createItem(config.material, name, lore, config.glow, config.head);
            if (item != null) {
                // Apply player head if needed
                if (config.head != null && !config.head.isEmpty()) {
                    applyPlayerHead(item, config.head, player);
                }

                inv.setItem(slot, item);
            }
        }
    }

    /**
     * Create an ItemStack from material string
     */
    private ItemStack createItem(String materialStr, String name, List<String> lore, boolean glow, String head) {
        Material material;

        // Handle player_head specially
        if (materialStr.equalsIgnoreCase("player_head")) {
            material = Material.PLAYER_HEAD;
        } else {
            try {
                material = Material.valueOf(materialStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material: " + materialStr);
                return null;
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Set name
        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        }

        // Set lore
        if (lore != null && !lore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
        }

        // Add glow effect (enchantment glint)
        if (glow) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Apply player head texture
     */
    private void applyPlayerHead(ItemStack item, String head, Player player) {
        if (!(item.getItemMeta() instanceof SkullMeta)) return;

        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // Replace %player% with actual player name
        if (head.equals("%player%")) {
            meta.setOwningPlayer(player);
        } else {
            // Try to set as player name
            meta.setOwner(head);
        }

        item.setItemMeta(meta);
    }

    /**
     * Process placeholders in a string
     */
    private String processPlaceholders(String text, Player player, Map<String, String> customPlaceholders) {
        if (text == null) return "";

        // Apply custom placeholders first
        if (customPlaceholders != null) {
            for (Map.Entry<String, String> entry : customPlaceholders.entrySet()) {
                text = text.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        // Apply PlaceholderAPI if available
        if (plugin.isPlaceholderAPIEnabled() && player != null) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        // Apply color codes
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Process placeholders in a list of strings
     */
    private List<String> processPlaceholdersList(List<String> list, Player player, Map<String, String> customPlaceholders) {
        List<String> result = new ArrayList<>();
        for (String line : list) {
            result.add(processPlaceholders(line, player, customPlaceholders));
        }
        return result;
    }

    /**
     * Create an ItemStack for dynamic content (backpack/ender page slots)
     */
    public ItemStack createDynamicItem(String material, String name, List<String> lore, Player player, Map<String, String> placeholders) {
        String processedName = processPlaceholders(name, player, placeholders);
        List<String> processedLore = processPlaceholdersList(lore, player, placeholders);

        return createItem(material, processedName, processedLore, false, null);
    }
}
