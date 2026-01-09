package com.vaultpack.expansions;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.api.events.BackpackOpenEvent;
import com.vaultpack.api.events.BackpackSlotUnlockEvent;
import com.vaultpack.api.events.EnderPageOpenEvent;
import com.vaultpack.api.events.EnderPageUnlockEvent;
import com.vaultpack.api.expansion.VaultPackExpansion;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in expansion for logging player actions.
 * Demonstrates how to create custom expansions.
 *
 * @since 3.0.0
 */
public class LoggingExpansion implements VaultPackExpansion, Listener {

    private VaultPackPlugin plugin;
    private boolean enabled = false;

    @Override
    public @NotNull String getId() {
        return "logging";
    }

    @Override
    public @NotNull String getName() {
        return "Action Logging";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public @NotNull String getAuthor() {
        return "VaultPack Team";
    }

    @Override
    public void onEnable(@NotNull VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.enabled = true;

        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("[LoggingExpansion] Now logging player actions");
    }

    @Override
    public void onDisable() {
        this.enabled = false;
        plugin.getLogger().info("[LoggingExpansion] Stopped logging");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @EventHandler
    public void onBackpackOpen(BackpackOpenEvent event) {
        if (!enabled) return;

        plugin.getLogger().info(String.format("[LoggingExpansion] %s opened backpack in slot %d",
                event.getPlayer().getName(),
                event.getSlot()));
    }

    @EventHandler
    public void onBackpackSlotUnlock(BackpackSlotUnlockEvent event) {
        if (!enabled) return;

        plugin.getLogger().info(String.format("[LoggingExpansion] %s unlocked backpack slot %d (cost: %d)",
                event.getPlayer().getName(),
                event.getSlot(),
                event.getCost()));
    }

    @EventHandler
    public void onEnderPageOpen(EnderPageOpenEvent event) {
        if (!enabled) return;

        plugin.getLogger().info(String.format("[LoggingExpansion] %s opened ender chest page %d",
                event.getPlayer().getName(),
                event.getPage()));
    }

    @EventHandler
    public void onEnderPageUnlock(EnderPageUnlockEvent event) {
        if (!enabled) return;

        plugin.getLogger().info(String.format("[LoggingExpansion] %s unlocked ender chest page %d (cost: %d)",
                event.getPlayer().getName(),
                event.getPage(),
                event.getCost()));
    }
}
