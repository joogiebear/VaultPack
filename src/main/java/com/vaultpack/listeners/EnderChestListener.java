package com.vaultpack.listeners;

import com.vaultpack.VaultPackPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * v2.0.0: Overrides vanilla ender chest functionality
 * When players open an ender chest, they see VaultPack's unified storage GUI
 */
public class EnderChestListener implements Listener {

    private final VaultPackPlugin plugin;

    public EnderChestListener(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Intercept ender chest opens and redirect to VaultPack GUI
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnderChestOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) {
            return;
        }

        Player player = event.getPlayer();

        // Cancel vanilla ender chest open
        event.setCancelled(true);

        // Open VaultPack unified storage GUI instead
        plugin.getBackpackManager().openUnifiedStorageGUI(player);
    }

    /**
     * Handle clicks in ender chest pages
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if clicking in ender chest inventory
        if (title.contains("Ender Chest") && plugin.getEnderChestManager().isEnderPageOpen(player)) {
            // Check if clicked in the top inventory (ender page GUI)
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

            // Allow normal interaction in ender page
            return;
        }
    }

    /**
     * Handle navigation button clicks in ender page header
     */
    private void handleNavigationClick(Player player, int slot) {
        Integer currentPage = plugin.getEnderChestManager().getOpenEnderPageNumber(player);
        if (currentPage == null) return;

        com.vaultpack.data.holders.PlayerDataHolder data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        switch (slot) {
            case 0: // Close button
                playSound(player, "BLOCK_ENDER_CHEST_CLOSE");
                player.closeInventory();
                break;

            case 1: // Back to all pages
                playSound(player, "UI_BUTTON_CLICK");
                player.closeInventory();
                // Folia-compatible: Use player's EntityScheduler
                player.getScheduler().runDelayed(plugin, task -> {
                    if (player.isOnline()) {
                        new com.vaultpack.gui.StorageMenuGUI(plugin).open(player);
                    }
                }, null, 1L);
                break;

            case 5: // First page
                int firstPage = findFirstPage(data);
                if (firstPage != -1 && firstPage != currentPage) {
                    playSound(player, "UI_BUTTON_CLICK");
                    player.closeInventory();
                    // Folia-compatible: Use player's EntityScheduler
                    player.getScheduler().runDelayed(plugin, task -> {
                        if (player.isOnline()) {
                            plugin.getEnderChestManager().openEnderPage(player, firstPage);
                        }
                    }, null, 1L);
                } else {
                    playSound(player, "ENTITY_VILLAGER_NO");
                    com.vaultpack.utils.ActionBarUtil.sendWarning(player, "Already at the first page!");
                }
                break;

            case 6: // Previous page
                int previousPage = findPreviousPage(data, currentPage);
                if (previousPage != -1) {
                    playSound(player, "UI_BUTTON_CLICK");
                    player.closeInventory();
                    // Folia-compatible: Use player's EntityScheduler
                    player.getScheduler().runDelayed(plugin, task -> {
                        if (player.isOnline()) {
                            plugin.getEnderChestManager().openEnderPage(player, previousPage);
                        }
                    }, null, 1L);
                } else {
                    playSound(player, "ENTITY_VILLAGER_NO");
                    com.vaultpack.utils.ActionBarUtil.sendWarning(player, "No previous page!");
                }
                break;

            case 7: // Next page
                int nextPage = findNextPage(data, currentPage);
                if (nextPage != -1) {
                    playSound(player, "UI_BUTTON_CLICK");
                    player.closeInventory();
                    // Folia-compatible: Use player's EntityScheduler
                    player.getScheduler().runDelayed(plugin, task -> {
                        if (player.isOnline()) {
                            plugin.getEnderChestManager().openEnderPage(player, nextPage);
                        }
                    }, null, 1L);
                } else {
                    playSound(player, "ENTITY_VILLAGER_NO");
                    com.vaultpack.utils.ActionBarUtil.sendWarning(player, "No next page!");
                }
                break;

            case 8: // Last page
                int lastPage = findLastPage(data);
                if (lastPage != -1 && lastPage != currentPage) {
                    playSound(player, "UI_BUTTON_CLICK");
                    player.closeInventory();
                    // Folia-compatible: Use player's EntityScheduler
                    player.getScheduler().runDelayed(plugin, task -> {
                        if (player.isOnline()) {
                            plugin.getEnderChestManager().openEnderPage(player, lastPage);
                        }
                    }, null, 1L);
                } else {
                    playSound(player, "ENTITY_VILLAGER_NO");
                    com.vaultpack.utils.ActionBarUtil.sendWarning(player, "Already at the last page!");
                }
                break;
        }
    }

    /**
     * Play a simple UI sound for ender page navigation feedback.
     */
    private void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty()) return;

        try {
            String convertedSound = soundName.toLowerCase().replace('_', '.');
            player.playSound(player.getLocation(), convertedSound,
                org.bukkit.SoundCategory.MASTER, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
        }
    }

    /**
     * Find the first unlocked ender page
     */
    private int findFirstPage(com.vaultpack.data.holders.PlayerDataHolder data) {
        for (int i = 1; i <= 9; i++) {
            if (data.isEnderPageUnlocked(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the last unlocked ender page
     */
    private int findLastPage(com.vaultpack.data.holders.PlayerDataHolder data) {
        for (int i = 9; i >= 1; i--) {
            if (data.isEnderPageUnlocked(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the previous unlocked ender page before the current one
     */
    private int findPreviousPage(com.vaultpack.data.holders.PlayerDataHolder data, int currentPage) {
        for (int i = currentPage - 1; i >= 1; i--) {
            if (data.isEnderPageUnlocked(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the next unlocked ender page after the current one
     */
    private int findNextPage(com.vaultpack.data.holders.PlayerDataHolder data, int currentPage) {
        for (int i = currentPage + 1; i <= 9; i++) {
            if (data.isEnderPageUnlocked(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Handle closing of ender chest pages
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // Check if they're closing an ender page
        if (plugin.getEnderChestManager().isEnderPageOpen(player)) {
            String title = event.getView().getTitle();

            // Check if it's an ender chest inventory
            if (title.contains("Ender Chest")) {
                plugin.getEnderChestManager().closeEnderPage(player);
            }
        }
    }
}
