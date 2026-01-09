package com.vaultpack.gui.holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * InventoryHolder for the unified storage menu
 * Replaces fragile title-based matching with type-safe holder pattern
 */
public class StorageMenuInventoryHolder implements InventoryHolder {

    private Inventory inventory;

    /**
     * Create a new storage menu inventory holder
     */
    public StorageMenuInventoryHolder() {
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
