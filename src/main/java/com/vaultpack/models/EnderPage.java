package com.vaultpack.models;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single ender chest page (45 slots)
 */
public class EnderPage {

    private final UUID ownerUUID;
    private final int pageNumber;
    private final Map<Integer, ItemStack> contents;
    private transient Inventory activeInventory;

    public EnderPage(UUID ownerUUID, int pageNumber) {
        this.ownerUUID = ownerUUID;
        this.pageNumber = pageNumber;
        this.contents = new HashMap<>();
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getSize() {
        return 45; // 5 rows x 9 columns
    }

    public Map<Integer, ItemStack> getContents() {
        return new HashMap<>(contents);
    }

    public void setContents(Map<Integer, ItemStack> contents) {
        this.contents.clear();
        if (contents != null) {
            this.contents.putAll(contents);
        }
    }

    public ItemStack getItem(int slot) {
        return contents.get(slot);
    }

    public void setItem(int slot, ItemStack item) {
        if (slot < 0 || slot >= getSize()) {
            return;
        }
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            contents.remove(slot);
        } else {
            contents.put(slot, item.clone());
        }
    }

    public Inventory getActiveInventory() {
        return activeInventory;
    }

    public void setActiveInventory(Inventory inventory) {
        this.activeInventory = inventory;
    }

    public int getUsedSlots() {
        return contents.size();
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public boolean isFull() {
        return contents.size() >= getSize();
    }

    /**
     * Get fullness percentage (0.0 to 1.0)
     */
    public double getFullness() {
        return (double) contents.size() / getSize();
    }

    /**
     * Get a visual fullness bar for display
     */
    public String getFullnessBar() {
        double fullness = getFullness();
        int filledBars = (int) (fullness * 10);
        int emptyBars = 10 - filledBars;

        StringBuilder bar = new StringBuilder();

        // Color based on fullness
        if (fullness < 0.5) {
            bar.append("&a"); // Green
        } else if (fullness < 0.8) {
            bar.append("&e"); // Yellow
        } else {
            bar.append("&c"); // Red
        }

        // Add filled bars
        for (int i = 0; i < filledBars; i++) {
            bar.append("█");
        }

        // Add empty bars
        bar.append("&7");
        for (int i = 0; i < emptyBars; i++) {
            bar.append("█");
        }

        return bar.toString();
    }

    /**
     * Clear all contents
     */
    public void clear() {
        contents.clear();
    }
}
