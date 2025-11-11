package com.vaultpack.models;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Backpack {

    private final UUID owner;
    private final int slotNumber;
    private BackpackTier tier;
    private String backpackTypeId; // ID of the backpack type (for texture)
    private Map<Integer, ItemStack> contents;
    private transient Inventory activeInventory;

    public Backpack(UUID owner, int slotNumber, BackpackTier tier) {
        this.owner = owner;
        this.slotNumber = slotNumber;
        this.tier = tier;
        this.contents = new HashMap<>();
    }

    public Backpack(UUID owner, int slotNumber, BackpackTier tier, String backpackTypeId) {
        this.owner = owner;
        this.slotNumber = slotNumber;
        this.tier = tier;
        this.backpackTypeId = backpackTypeId;
        this.contents = new HashMap<>();
    }

    public UUID getOwner() {
        return owner;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public BackpackTier getTier() {
        return tier;
    }

    public void setTier(BackpackTier tier) {
        this.tier = tier;
    }

    public String getBackpackTypeId() {
        return backpackTypeId;
    }

    public void setBackpackTypeId(String backpackTypeId) {
        this.backpackTypeId = backpackTypeId;
    }

    public int getSize() {
        return tier.getSize();
    }

    public Map<Integer, ItemStack> getContents() {
        return contents;
    }

    public void setContents(Map<Integer, ItemStack> contents) {
        this.contents = contents;
    }

    public Inventory getActiveInventory() {
        return activeInventory;
    }

    public void setActiveInventory(Inventory inventory) {
        this.activeInventory = inventory;
    }

    public int getUsedSlots() {
        int count = 0;
        for (ItemStack item : contents.values()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                count++;
            }
        }
        return count;
    }

    public double getFullnessPercent() {
        return (double) getUsedSlots() / (double) getSize() * 100.0;
    }

    public String getFullnessBar() {
        double percent = getFullnessPercent();

        if (percent == 0) return "&8[----------]";
        if (percent <= 10) return "&c[&a█&8---------]";
        if (percent <= 20) return "&c[&a██&8--------]";
        if (percent <= 30) return "&6[&a███&8-------]";
        if (percent <= 40) return "&6[&a████&8------]";
        if (percent <= 50) return "&e[&a█████&8-----]";
        if (percent <= 60) return "&e[&a██████&8----]";
        if (percent <= 70) return "&2[&a███████&8---]";
        if (percent <= 80) return "&2[&a████████&8--]";
        if (percent <= 90) return "&a[&a█████████&8-]";
        return "&a[&a██████████&a]";
    }

    public boolean isEmpty() {
        return getUsedSlots() == 0;
    }

    public boolean isFull() {
        return getUsedSlots() >= getSize();
    }

    public boolean canUpgrade() {
        return !tier.isMaxTier();
    }

    public BackpackTier getNextTier() {
        return tier.getNext();
    }

    public void upgrade() {
        if (canUpgrade()) {
            this.tier = tier.getNext();
        }
    }

    @Override
    public String toString() {
        return "Backpack{" +
                "owner=" + owner +
                ", slot=" + slotNumber +
                ", tier=" + tier +
                ", used=" + getUsedSlots() + "/" + getSize() +
                '}';
    }
}
