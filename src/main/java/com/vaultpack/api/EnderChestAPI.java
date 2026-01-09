package com.vaultpack.api;

import com.vaultpack.models.EnderPage;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * API for ender chest operations.
 * Provides methods to manage player ender chest pages.
 *
 * <p>All methods are thread-safe.</p>
 *
 * @since 3.0.0
 */
public interface EnderChestAPI {

    /**
     * Get the number of unlocked ender chest pages for a player.
     *
     * @param playerId Player UUID
     * @return Number of unlocked pages
     */
    int getUnlockedPages(UUID playerId);

    /**
     * Get the number of unlocked ender chest pages for a player.
     *
     * @param player The player
     * @return Number of unlocked pages
     */
    default int getUnlockedPages(Player player) {
        return getUnlockedPages(player.getUniqueId());
    }

    /**
     * Unlock an ender chest page for a player.
     *
     * @param playerId Player UUID
     * @param page     Page number to unlock
     * @return true if successful
     */
    boolean unlockPage(UUID playerId, int page);

    /**
     * Unlock an ender chest page for a player.
     *
     * @param player The player
     * @param page   Page number to unlock
     * @return true if successful
     */
    default boolean unlockPage(Player player, int page) {
        return unlockPage(player.getUniqueId(), page);
    }

    /**
     * Check if a page is unlocked for a player.
     *
     * @param playerId Player UUID
     * @param page     Page number
     * @return true if unlocked
     */
    boolean isPageUnlocked(UUID playerId, int page);

    /**
     * Check if a page is unlocked for a player.
     *
     * @param player The player
     * @param page   Page number
     * @return true if unlocked
     */
    default boolean isPageUnlocked(Player player, int page) {
        return isPageUnlocked(player.getUniqueId(), page);
    }

    /**
     * Get an ender chest page.
     *
     * @param playerId Player UUID
     * @param page     Page number
     * @return Optional containing the ender page
     */
    Optional<EnderPage> getPage(UUID playerId, int page);

    /**
     * Get an ender chest page.
     *
     * @param player The player
     * @param page   Page number
     * @return Optional containing the ender page
     */
    default Optional<EnderPage> getPage(Player player, int page) {
        return getPage(player.getUniqueId(), page);
    }

    /**
     * Open an ender chest page for a player.
     *
     * @param player The player
     * @param page   Page number
     * @return true if successfully opened
     */
    boolean openEnderPage(Player player, int page);

    /**
     * Get total ender storage slots for a player.
     *
     * @param playerId Player UUID
     * @return Total slot count
     */
    int getTotalStorageSlots(UUID playerId);

    /**
     * Get total ender storage slots for a player.
     *
     * @param player The player
     * @return Total slot count
     */
    default int getTotalStorageSlots(Player player) {
        return getTotalStorageSlots(player.getUniqueId());
    }

    /**
     * Get total used ender slots for a player.
     *
     * @param playerId Player UUID
     * @return Total used slots
     */
    int getTotalUsedSlots(UUID playerId);

    /**
     * Get total used ender slots for a player.
     *
     * @param player The player
     * @return Total used slots
     */
    default int getTotalUsedSlots(Player player) {
        return getTotalUsedSlots(player.getUniqueId());
    }

    /**
     * Get the maximum number of ender chest pages allowed.
     *
     * @return Max pages (typically 9)
     */
    default int getMaxPages() {
        return 9;
    }
}
