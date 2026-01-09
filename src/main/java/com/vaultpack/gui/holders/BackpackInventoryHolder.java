package com.vaultpack.gui.holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * InventoryHolder for backpack inventories
 * Replaces fragile title-based matching with type-safe holder pattern
 */
public class BackpackInventoryHolder implements InventoryHolder {

    private final int slotNumber;
    private Inventory inventory;

    /**
     * Create a new backpack inventory holder
     *
     * @param slotNumber The backpack slot number (1-18)
     */
    public BackpackInventoryHolder(int slotNumber) {
        if (slotNumber < 1 || slotNumber > 18) {
            throw new IllegalArgumentException("Slot number must be between 1 and 18, got: " + slotNumber);
        }
        this.slotNumber = slotNumber;
    }

    /**
     * Get the backpack slot number
     *
     * @return The slot number (1-18)
     */
    public int getSlotNumber() {
        return slotNumber;
    }

    @Nullable
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Set the inventory for this holder
     *
     * @param inventory The inventory
     */
    public void setInventory(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }
}
