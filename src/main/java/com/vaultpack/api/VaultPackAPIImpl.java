package com.vaultpack.api;

import com.vaultpack.VaultPackPlugin;
import lombok.Getter;

import java.util.UUID;

/**
 * Main implementation of the VaultPack API.
 * Provides access to backpack and ender chest APIs.
 * This is the primary entry point for third-party plugins.
 *
 * @since 3.0.0
 */
public class VaultPackAPIImpl implements IVaultPackAPI {

    @Getter
    private final BackpackAPI backpackAPI;

    @Getter
    private final EnderChestAPI enderChestAPI;

    private final VaultPackPlugin plugin;

    public VaultPackAPIImpl(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.backpackAPI = new BackpackAPIImpl(plugin);
        this.enderChestAPI = new EnderChestAPIImpl(plugin);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean isDataLoaded(UUID playerId) {
        return plugin.getDataManager().isDataLoaded(playerId);
    }
}
