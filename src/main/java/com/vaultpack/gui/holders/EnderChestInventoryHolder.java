package com.vaultpack.gui.holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * InventoryHolder for ender chest page inventories
 * Replaces fragile title-based matching with type-safe holder pattern
 */
public class EnderChestInventoryHolder implements InventoryHolder {

    private final int pageNumber;
    private Inventory inventory;

    /**
     * Create a new ender chest inventory holder
     *
     * @param pageNumber The ender chest page number (1-9)
     */
    public EnderChestInventoryHolder(int pageNumber) {
        if (pageNumber < 1 || pageNumber > 9) {
            throw new IllegalArgumentException("Page number must be between 1 and 9, got: " + pageNumber);
        }
        this.pageNumber = pageNumber;
    }

    /**
     * Get the ender chest page number
     *
     * @return The page number (1-9)
     */
    public int getPageNumber() {
        return pageNumber;
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
