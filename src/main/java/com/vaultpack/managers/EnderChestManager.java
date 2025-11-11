package com.vaultpack.managers;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.models.EnderPage;
import com.vaultpack.models.PlayerBackpackData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages ender chest pages and functionality
 * v2.0.0: VaultPack now controls ALL ender chest interactions
 */
public class EnderChestManager {

    private final VaultPackPlugin plugin;
    private final Map<UUID, Integer> openEnderPages; // Player UUID -> Page number

    public EnderChestManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.openEnderPages = new HashMap<>();
    }

    /**
     * Open a specific ender page for a player
     */
    public void openEnderPage(Player player, int pageNumber) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        // Check if page is unlocked
        if (!data.isEnderPageUnlocked(pageNumber)) {
            player.sendMessage(ChatColor.RED + "This ender chest page is locked!");
            return;
        }

        // Get or create the page
        EnderPage page = data.getEnderPage(pageNumber);
        if (page == null) {
            page = new EnderPage(player.getUniqueId(), pageNumber);
            data.setEnderPage(pageNumber, page);
        }

        // Create inventory with extra row for navigation header (6 rows = 54 slots)
        String title = ChatColor.translateAlternateColorCodes('&',
                "&5Ender Chest &7- &dPage " + pageNumber);

        Inventory inventory = Bukkit.createInventory(null, 54, title);

        // Add navigation header (row 1, slots 0-8)
        addNavigationHeader(inventory, player, pageNumber, data);

        // Load contents (starting from row 2, slot 9)
        Map<Integer, ItemStack> contents = page.getContents();
        for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue();

            if (slot >= 0 && slot < 45 && item != null) {
                // Offset by 9 to skip navigation header
                inventory.setItem(slot + 9, item.clone());
            }
        }

        // Open inventory
        player.openInventory(inventory);
        page.setActiveInventory(inventory);
        openEnderPages.put(player.getUniqueId(), pageNumber);

        // Send action bar feedback
        com.vaultpack.utils.ActionBarUtil.sendInfo(player,
            "Ender Chest - Page " + pageNumber + " &7(" + page.getUsedSlots() + "/45)");
    }

    /**
     * Close an ender page and save contents
     */
    public void closeEnderPage(Player player) {
        Integer pageNumber = openEnderPages.remove(player.getUniqueId());

        if (pageNumber != null) {
            saveEnderPageContents(player, pageNumber);
        }
    }

    /**
     * Save ender page contents to data
     */
    private void saveEnderPageContents(Player player, int pageNumber) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        EnderPage page = data.getEnderPage(pageNumber);

        if (page != null && page.getActiveInventory() != null) {
            Inventory inventory = page.getActiveInventory();
            Map<Integer, ItemStack> contents = new HashMap<>();

            // Read from slot i+9 (offset by navigation header) and save to slot i
            for (int i = 0; i < 45; i++) {
                ItemStack item = inventory.getItem(i + 9); // +9 to skip navigation header
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    contents.put(i, item.clone());
                }
            }

            page.setContents(contents);
            page.setActiveInventory(null);

            // Save to file
            plugin.getDataManager().savePlayerData(player.getUniqueId());
        }
    }

    /**
     * Check if a player has an ender page open
     */
    public boolean isEnderPageOpen(Player player) {
        return openEnderPages.containsKey(player.getUniqueId());
    }

    /**
     * Get the page number currently open for a player
     */
    public Integer getOpenEnderPageNumber(Player player) {
        return openEnderPages.get(player.getUniqueId());
    }

    /**
     * Create an ender page for a player
     */
    public void createEnderPage(Player player, int pageNumber) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        EnderPage page = new EnderPage(player.getUniqueId(), pageNumber);
        data.setEnderPage(pageNumber, page);

        plugin.getDataManager().savePlayerData(player.getUniqueId());
    }

    /**
     * Unlock an ender page for a player
     */
    public void unlockEnderPage(Player player, int pageNumber) {
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (data.isEnderPageUnlocked(pageNumber)) {
            player.sendMessage(ChatColor.RED + "This ender chest page is already unlocked!");
            return;
        }

        // Check permission first
        if (plugin.getConfigManager().usePermissions()) {
            String permission = "vaultpack.enderchest.page." + pageNumber;
            if (player.hasPermission(permission)) {
                data.unlockEnderPage(pageNumber);
                plugin.getDataManager().savePlayerData(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Ender Chest Page " + pageNumber + " unlocked!");
                return;
            }
        }

        // Check economy cost (if configured)
        if (plugin.getConfigManager().useEconomy() && plugin.isVaultEnabled()) {
            int cost = plugin.getConfigManager().getEnderPageUnlockCost(pageNumber);

            if (!plugin.getEconomyManager().hasMoney(player, cost)) {
                player.sendMessage(ChatColor.RED + "You don't have enough money! Need: $" + cost);
                return;
            }

            plugin.getEconomyManager().takeMoney(player, cost);
            data.unlockEnderPage(pageNumber);
            plugin.getDataManager().savePlayerData(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Ender Chest Page " + pageNumber + " unlocked for $" + cost + "!");
        } else {
            player.sendMessage(ChatColor.RED + "You don't have permission to unlock this page!");
        }
    }

    /**
     * Add navigation header to ender page inventory
     */
    private void addNavigationHeader(Inventory inventory, Player player, int currentPage, PlayerBackpackData data) {
        // Player head textures
        String firstTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQ3MDdkYjQ2YTVhY2JmZWJmNjEyMzk1MzZkMjU2NDgxMzRiYjQzYjY1YzE2NzE2YmEzMjljNmRiZjQxMiJ9fX0=";
        String previousTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTVkYTQ4NDcyNzI1ODIyNjViZGFjYTM2NzIzN2M5NjEyMmIxMzlmNGU1OTdmYmM2NjY3ZDNmYjc1ZmVhN2NmNiJ9fX0=";
        String nextTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjUyN2ViYWU5ZjE1MzE1NGE3ZWQ0OWM4OGMwMmI1YTlhOWNhN2NiMTYxOGQ5OTE0YTNkOWRmOGNjYjNjODQifX19";
        String lastTexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWI3MThhYmUxMDI0NzYyYzFjNTIyNWM5YjNjZjk0M2EwMTRmNTRkZDhkNGQ0NjRhMmQ2MWYwOThjMDdkOWUifX19";

        // Slot 0: Close button
        inventory.setItem(0, createButton(org.bukkit.Material.BARRIER, "&c&lClose",
            "&7Click to close this page"));

        // Slot 1: Back to all pages
        inventory.setItem(1, createButton(org.bukkit.Material.ENDER_CHEST, "&e&lAll Pages",
            "&7Return to storage menu"));

        // Find first and last ender pages
        int firstPage = -1;
        int lastPage = -1;
        for (int i = 1; i <= 9; i++) {
            if (data.isEnderPageUnlocked(i)) {
                if (firstPage == -1) firstPage = i;
                lastPage = i;
            }
        }

        // Slot 5: First page
        if (firstPage != -1 && firstPage != currentPage) {
            inventory.setItem(5, createButtonWithTexture(firstTexture, "&a&lFirst Page",
                "&7Jump to page " + firstPage));
        } else {
            inventory.setItem(5, createButton(org.bukkit.Material.GRAY_DYE, "&7First Page",
                "&cAlready at first or only page"));
        }

        // Slot 6: Previous page
        int previousPage = findPreviousPage(data, currentPage);
        if (previousPage != -1) {
            inventory.setItem(6, createButtonWithTexture(previousTexture, "&e&lPrevious",
                "&7Go to page " + previousPage));
        } else {
            inventory.setItem(6, createButton(org.bukkit.Material.GRAY_DYE, "&7Previous",
                "&cNo previous page"));
        }

        // Slot 7: Next page
        int nextPage = findNextPage(data, currentPage);
        if (nextPage != -1) {
            inventory.setItem(7, createButtonWithTexture(nextTexture, "&e&lNext",
                "&7Go to page " + nextPage));
        } else {
            inventory.setItem(7, createButton(org.bukkit.Material.GRAY_DYE, "&7Next",
                "&cNo next page"));
        }

        // Slot 8: Last page
        if (lastPage != -1 && lastPage != currentPage) {
            inventory.setItem(8, createButtonWithTexture(lastTexture, "&a&lLast Page",
                "&7Jump to page " + lastPage));
        } else {
            inventory.setItem(8, createButton(org.bukkit.Material.GRAY_DYE, "&7Last Page",
                "&cAlready at last or only page"));
        }
    }

    private ItemStack createButton(org.bukkit.Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            java.util.List<String> loreList = new java.util.ArrayList<>();
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

            java.util.List<String> loreList = new java.util.ArrayList<>();
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

    private int findPreviousPage(PlayerBackpackData data, int currentPage) {
        for (int i = currentPage - 1; i >= 1; i--) {
            if (data.isEnderPageUnlocked(i)) {
                return i;
            }
        }
        return -1;
    }

    private int findNextPage(PlayerBackpackData data, int currentPage) {
        for (int i = currentPage + 1; i <= 9; i++) {
            if (data.isEnderPageUnlocked(i)) {
                return i;
            }
        }
        return -1;
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

    /**
     * Close all open ender pages
     */
    public void closeAllEnderPages() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isEnderPageOpen(player)) {
                player.closeInventory();
            }
        }
        openEnderPages.clear();
    }

    /**
     * Search for an item across all ender pages
     */
    public Map<Integer, Integer> searchEnderPages(Player player, org.bukkit.Material material) {
        Map<Integer, Integer> results = new HashMap<>(); // Page -> Count
        PlayerBackpackData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        for (int i = 1; i <= 9; i++) {
            if (!data.isEnderPageUnlocked(i)) continue;

            EnderPage page = data.getEnderPage(i);
            if (page == null) continue;

            int count = 0;
            for (ItemStack item : page.getContents().values()) {
                if (item != null && item.getType() == material) {
                    count += item.getAmount();
                }
            }

            if (count > 0) {
                results.put(i, count);
            }
        }

        return results;
    }
}
