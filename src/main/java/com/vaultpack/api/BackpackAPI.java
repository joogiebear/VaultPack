package com.vaultpack.api;

import com.vaultpack.models.Backpack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * API for backpack operations.
 * Provides methods to manage player backpacks.
 *
 * <p>All methods are thread-safe.</p>
 *
 * @since 3.0.0
 */
public interface BackpackAPI {

    /**
     * Get the number of unlocked backpack slots for a player.
     *
     * @param playerId Player UUID
     * @return Number of unlocked slots
     */
    int getUnlockedSlots(UUID playerId);

    /**
     * Get the number of unlocked backpack slots for a player.
     *
     * @param player The player
     * @return Number of unlocked slots
     */
    default int getUnlockedSlots(Player player) {
        return getUnlockedSlots(player.getUniqueId());
    }

    /**
     * Unlock a backpack slot for a player.
     *
     * @param playerId Player UUID
     * @param slot     Slot number to unlock
     * @return true if successful
     */
    boolean unlockSlot(UUID playerId, int slot);

    /**
     * Unlock a backpack slot for a player.
     *
     * @param player The player
     * @param slot   Slot number to unlock
     * @return true if successful
     */
    default boolean unlockSlot(Player player, int slot) {
        return unlockSlot(player.getUniqueId(), slot);
    }

    /**
     * Check if a slot is unlocked for a player.
     *
     * @param playerId Player UUID
     * @param slot     Slot number
     * @return true if unlocked
     */
    boolean isSlotUnlocked(UUID playerId, int slot);

    /**
     * Check if a slot is unlocked for a player.
     *
     * @param player The player
     * @param slot   Slot number
     * @return true if unlocked
     */
    default boolean isSlotUnlocked(Player player, int slot) {
        return isSlotUnlocked(player.getUniqueId(), slot);
    }

    /**
     * Check if a backpack exists in a slot.
     *
     * @param playerId Player UUID
     * @param slot     Slot number
     * @return true if backpack exists
     */
    boolean hasBackpack(UUID playerId, int slot);

    /**
     * Check if a backpack exists in a slot.
     *
     * @param player The player
     * @param slot   Slot number
     * @return true if backpack exists
     */
    default boolean hasBackpack(Player player, int slot) {
        return hasBackpack(player.getUniqueId(), slot);
    }

    /**
     * Get a backpack from a slot.
     *
     * @param playerId Player UUID
     * @param slot     Slot number
     * @return Optional containing the backpack, or empty if none exists
     */
    Optional<Backpack> getBackpack(UUID playerId, int slot);

    /**
     * Get a backpack from a slot.
     *
     * @param player The player
     * @param slot   Slot number
     * @return Optional containing the backpack, or empty if none exists
     */
    default Optional<Backpack> getBackpack(Player player, int slot) {
        return getBackpack(player.getUniqueId(), slot);
    }

    /**
     * Open a backpack for a player.
     *
     * @param player The player
     * @param slot   Slot number
     * @return true if successfully opened
     */
    boolean openBackpack(Player player, int slot);

    /**
     * Get the number of active backpacks for a player.
     *
     * @param playerId Player UUID
     * @return Number of active backpacks
     */
    int getActiveBackpackCount(UUID playerId);

    /**
     * Get the number of active backpacks for a player.
     *
     * @param player The player
     * @return Number of active backpacks
     */
    default int getActiveBackpackCount(Player player) {
        return getActiveBackpackCount(player.getUniqueId());
    }

    /**
     * Get total storage slots across all backpacks.
     *
     * @param playerId Player UUID
     * @return Total slot count
     */
    int getTotalStorageSlots(UUID playerId);

    /**
     * Get total storage slots across all backpacks.
     *
     * @param player The player
     * @return Total slot count
     */
    default int getTotalStorageSlots(Player player) {
        return getTotalStorageSlots(player.getUniqueId());
    }

    /**
     * Get total used slots across all backpacks.
     *
     * @param playerId Player UUID
     * @return Total used slots
     */
    int getTotalUsedSlots(UUID playerId);

    /**
     * Get total used slots across all backpacks.
     *
     * @param player The player
     * @return Total used slots
     */
    default int getTotalUsedSlots(Player player) {
        return getTotalUsedSlots(player.getUniqueId());
    }

    /**
     * Add an item to a player's backpack.
     * Attempts to add to the specified slot, or any available slot if slot is 0.
     *
     * @param playerId Player UUID
     * @param slot     Slot number (0 for any available slot)
     * @param item     Item to add
     * @return true if item was added successfully
     */
    boolean addItem(UUID playerId, int slot, ItemStack item);

    /**
     * Add an item to a player's backpack.
     * Attempts to add to the specified slot, or any available slot if slot is 0.
     *
     * @param player The player
     * @param slot   Slot number (0 for any available slot)
     * @param item   Item to add
     * @return true if item was added successfully
     */
    default boolean addItem(Player player, int slot, ItemStack item) {
        return addItem(player.getUniqueId(), slot, item);
    }

    /**
     * Get the maximum number of backpack slots allowed.
     *
     * @return Max slots
     */
    int getMaxBackpackSlots();
}
