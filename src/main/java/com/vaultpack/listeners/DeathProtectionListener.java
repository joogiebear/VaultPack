package com.vaultpack.listeners;

import com.vaultpack.VaultPackPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * v2.0.0: Death protection listener
 * Ensures that items stored in backpacks and ender chest pages are NEVER lost on death
 * This is intentionally non-configurable to prevent accidental data loss
 */
public class DeathProtectionListener implements Listener {

    private final VaultPackPlugin plugin;

    public DeathProtectionListener(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Items in backpacks and ender pages are stored in memory/disk, not in player inventory
        // They are automatically protected by design - this listener exists as a safety check

        // Save player data immediately on death to ensure no data loss
        plugin.getDataManager().savePlayerData(event.getEntity().getUniqueId());

        // Log for debugging/audit purposes
        plugin.getLogger().fine("Player " + event.getEntity().getName() + " died - backpack data saved");
    }
}
