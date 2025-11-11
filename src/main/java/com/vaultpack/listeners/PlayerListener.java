package com.vaultpack.listeners;

import com.vaultpack.VaultPackPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final VaultPackPlugin plugin;

    public PlayerListener(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load player data when they join
        plugin.getDataManager().getPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save and unload player data when they quit
        plugin.getBackpackManager().closeBackpack(event.getPlayer());
        plugin.getDataManager().unloadPlayerData(event.getPlayer().getUniqueId());
    }
}
