package com.vaultpack.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerBackpackData {

    private final UUID playerId;
    private int unlockedSlots;
    private Map<Integer, Backpack> backpacks;

    // v2.0.0: Ender Chest System
    private int unlockedEnderPages;
    private Map<Integer, EnderPage> enderPages;

    public PlayerBackpackData(UUID playerId) {
        this.playerId = playerId;
        this.unlockedSlots = 1; // Default: 1 unlocked slot
        this.backpacks = new HashMap<>();
        this.unlockedEnderPages = 1; // Default: 1 unlocked ender page
        this.enderPages = new HashMap<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getUnlockedSlots() {
        return unlockedSlots;
    }

    public void setUnlockedSlots(int slots) {
        this.unlockedSlots = Math.max(1, slots);
    }

    public void unlockSlot(int slotNumber) {
        if (slotNumber > unlockedSlots) {
            unlockedSlots = slotNumber;
        }
    }

    public boolean isSlotUnlocked(int slotNumber) {
        return slotNumber <= unlockedSlots;
    }

    public Map<Integer, Backpack> getBackpacks() {
        return backpacks;
    }

    public Backpack getBackpack(int slotNumber) {
        return backpacks.get(slotNumber);
    }

    public void setBackpack(int slotNumber, Backpack backpack) {
        if (backpack == null) {
            backpacks.remove(slotNumber);
        } else {
            backpacks.put(slotNumber, backpack);
        }
    }

    public boolean hasBackpack(int slotNumber) {
        return backpacks.containsKey(slotNumber);
    }

    public void removeBackpack(int slotNumber) {
        backpacks.remove(slotNumber);
    }

    public int getActiveBackpackCount() {
        return backpacks.size();
    }

    public int getTotalStorageSlots() {
        int total = 0;
        for (Backpack backpack : backpacks.values()) {
            total += backpack.getSize();
        }
        return total;
    }

    // ========== Ender Chest Methods (v2.0.0) ==========

    public int getUnlockedEnderPages() {
        return unlockedEnderPages;
    }

    public void setUnlockedEnderPages(int pages) {
        this.unlockedEnderPages = Math.max(1, Math.min(9, pages));
    }

    public void unlockEnderPage(int pageNumber) {
        if (pageNumber > unlockedEnderPages && pageNumber <= 9) {
            unlockedEnderPages = pageNumber;
        }
    }

    public boolean isEnderPageUnlocked(int pageNumber) {
        return pageNumber <= unlockedEnderPages && pageNumber >= 1;
    }

    public Map<Integer, EnderPage> getEnderPages() {
        return enderPages;
    }

    public EnderPage getEnderPage(int pageNumber) {
        return enderPages.get(pageNumber);
    }

    public void setEnderPage(int pageNumber, EnderPage page) {
        if (page == null) {
            enderPages.remove(pageNumber);
        } else {
            enderPages.put(pageNumber, page);
        }
    }

    public boolean hasEnderPage(int pageNumber) {
        return enderPages.containsKey(pageNumber);
    }

    public void removeEnderPage(int pageNumber) {
        enderPages.remove(pageNumber);
    }

    public int getActiveEnderPageCount() {
        return enderPages.size();
    }

    public int getTotalEnderStorageSlots() {
        return enderPages.size() * 45; // Each page is 45 slots
    }

    /**
     * v2.0.0: Get total used slots across all backpacks
     */
    public int getTotalUsedSlots() {
        int total = 0;
        for (Backpack backpack : backpacks.values()) {
            total += backpack.getUsedSlots();
        }
        return total;
    }

    /**
     * v2.0.0: Get total used slots across all ender pages
     */
    public int getTotalUsedEnderSlots() {
        int total = 0;
        for (EnderPage enderPage : enderPages.values()) {
            total += enderPage.getUsedSlots();
        }
        return total;
    }

    @Override
    public String toString() {
        return "PlayerBackpackData{" +
                "playerId=" + playerId +
                ", unlockedSlots=" + unlockedSlots +
                ", activeBackpacks=" + getActiveBackpackCount() +
                ", totalStorage=" + getTotalStorageSlots() +
                ", unlockedEnderPages=" + unlockedEnderPages +
                ", activeEnderPages=" + getActiveEnderPageCount() +
                ", totalEnderStorage=" + getTotalEnderStorageSlots() +
                '}';
    }
}
