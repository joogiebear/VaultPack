package com.vaultpack.api;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Main API interface for VaultPack.
 * Provides access to all plugin features for third-party plugins.
 *
 * <p>Usage:</p>
 * <pre>
 * IVaultPackAPI api = VaultPackAPI.getInstance();
 * BackpackAPI backpackAPI = api.getBackpackAPI();
 * EnderChestAPI enderAPI = api.getEnderChestAPI();
 * </pre>
 *
 * <p>This API is thread-safe and can be called from async tasks.</p>
 *
 * @since 3.0.0
 */
public interface IVaultPackAPI {

    /**
     * Get the backpack API for backpack operations.
     *
     * @return BackpackAPI instance
     */
    BackpackAPI getBackpackAPI();

    /**
     * Get the ender chest API for ender chest operations.
     *
     * @return EnderChestAPI instance
     */
    EnderChestAPI getEnderChestAPI();

    /**
     * Get the plugin version.
     *
     * @return Plugin version string (e.g., "3.0.0")
     */
    String getVersion();

    /**
     * Check if a player has VaultPack data loaded.
     *
     * @param playerId Player UUID
     * @return true if data is loaded
     */
    boolean isDataLoaded(UUID playerId);

    /**
     * Check if a player has VaultPack data loaded.
     *
     * @param player The player
     * @return true if data is loaded
     */
    default boolean isDataLoaded(Player player) {
        return isDataLoaded(player.getUniqueId());
    }

    /**
     * Get API version.
     * Used for compatibility checking.
     *
     * @return API version (e.g., 1)
     */
    default int getAPIVersion() {
        return 1;
    }
}
