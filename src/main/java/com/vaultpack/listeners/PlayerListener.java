package com.vaultpack.listeners;

import com.vaultpack.VaultPackPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Phase 1: Enhanced player listener with async data loading
 */
public class PlayerListener implements Listener {

    private final VaultPackPlugin plugin;

    public PlayerListener(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Phase 1: Async data loading on join
     * Uses MONITOR priority to run after other plugins
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Load player data asynchronously
        plugin.getDataManager().loadPlayerDataAsync(uuid).thenAccept(data -> {
            // Switch back to player's region scheduler when data is ready
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    // Data loaded successfully
                    plugin.getLogger().fine("Data loaded for " + player.getName());

                    // Send welcome message with stats if configured
                    if (plugin.getConfig().getBoolean("performance.show-cache-stats-on-join", false)) {
                        if (player.hasPermission("vaultpack.admin")) {
                            player.sendMessage("§7[VaultPack] §a" + plugin.getDataManager().getCacheStats());
                        }
                    }
                }
            }, null);
        }).exceptionally(ex -> {
            // Handle loading errors
            plugin.getLogger().severe("Failed to load data for " + player.getName() + ": " + ex.getMessage());
            ex.printStackTrace();

            // Send error message to player
            player.getScheduler().run(plugin, task -> {
                if (player.isOnline()) {
                    player.sendMessage("§c§l[VaultPack] §cFailed to load your data! Please contact an administrator.");
                }
            }, null);

            return null;
        });
    }

    /**
     * Phase 1: Enhanced quit handler with delayed unload
     * Saves immediately but delays cache removal
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Close any open backpacks
        if (plugin.getBackpackManager().isBackpackOpen(player)) {
            plugin.getBackpackManager().closeBackpack(player);
        }

        // Save and schedule delayed unload (5 minutes by default)
        plugin.getDataManager().unloadPlayerData(uuid);

        plugin.getLogger().fine("Scheduled delayed unload for " + player.getName());
    }
}
