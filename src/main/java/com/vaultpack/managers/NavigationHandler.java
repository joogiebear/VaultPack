package com.vaultpack.managers;

import com.vaultpack.models.PlayerBackpackData;

/**
 * Phase 3: Handles backpack navigation logic
 * Extracted from BackpackManager for better separation of concerns
 */
public class NavigationHandler {

    /**
     * Find the first backpack slot that is unlocked and has a backpack
     */
    public static int findFirstBackpack(PlayerBackpackData data) {
        for (int i = 1; i <= 18; i++) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the last backpack slot that is unlocked and has a backpack
     */
    public static int findLastBackpack(PlayerBackpackData data) {
        for (int i = 18; i >= 1; i--) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the previous backpack slot before the current one
     */
    public static int findPreviousBackpack(PlayerBackpackData data, int currentSlot) {
        for (int i = currentSlot - 1; i >= 1; i--) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find the next backpack slot after the current one
     */
    public static int findNextBackpack(PlayerBackpackData data, int currentSlot) {
        for (int i = currentSlot + 1; i <= 18; i++) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Check if there is a previous backpack
     */
    public static boolean hasPreviousBackpack(PlayerBackpackData data, int currentSlot) {
        return findPreviousBackpack(data, currentSlot) != -1;
    }

    /**
     * Check if there is a next backpack
     */
    public static boolean hasNextBackpack(PlayerBackpackData data, int currentSlot) {
        return findNextBackpack(data, currentSlot) != -1;
    }

    /**
     * Get the total number of backpacks the player has
     */
    public static int getTotalBackpacks(PlayerBackpackData data) {
        int count = 0;
        for (int i = 1; i <= 18; i++) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get the position of current backpack in the sequence (e.g., 3 of 5)
     */
    public static int getBackpackPosition(PlayerBackpackData data, int currentSlot) {
        int position = 0;
        for (int i = 1; i <= currentSlot; i++) {
            if (data.isSlotUnlocked(i) && data.hasBackpack(i)) {
                position++;
            }
        }
        return position;
    }

    /**
     * Check if a slot is the first backpack
     */
    public static boolean isFirstBackpack(PlayerBackpackData data, int slotNumber) {
        return findFirstBackpack(data) == slotNumber;
    }

    /**
     * Check if a slot is the last backpack
     */
    public static boolean isLastBackpack(PlayerBackpackData data, int slotNumber) {
        return findLastBackpack(data) == slotNumber;
    }
}
