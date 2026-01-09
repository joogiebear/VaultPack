package com.vaultpack.data.holders;

import com.vaultpack.data.components.BackpackDataComponent;
import com.vaultpack.data.components.EnderChestDataComponent;
import com.vaultpack.models.Backpack;
import com.vaultpack.models.EnderPage;

import java.util.UUID;

/**
 * Data holder for player-specific data.
 * Combines backpack and ender chest components for complete player storage.
 *
 * <p>This is the main data holder for VaultPack player data.</p>
 * <p>Uses composition pattern with DataComponents for modularity.</p>
 */
public class PlayerDataHolder extends BaseDataHolder {

    private final BackpackDataComponent backpackComponent;
    private final EnderChestDataComponent enderChestComponent;

    public PlayerDataHolder(UUID playerUUID) {
        super(playerUUID);

        // Register components
        this.backpackComponent = new BackpackDataComponent();
        this.enderChestComponent = new EnderChestDataComponent();

        registerComponent(backpackComponent);
        registerComponent(enderChestComponent);
    }

    // ============================================
    //         Backpack Convenience Methods
    // ============================================

    /**
     * Get the backpack data component.
     *
     * @return The backpack component
     */
    public BackpackDataComponent getBackpackData() {
        return backpackComponent;
    }

    /**
     * Check if a backpack slot is unlocked.
     *
     * @param slot The slot number
     * @return true if unlocked
     */
    public boolean isSlotUnlocked(int slot) {
        return backpackComponent.isSlotUnlocked(slot);
    }

    /**
     * Unlock a backpack slot.
     *
     * @param slot The slot number
     */
    public void unlockSlot(int slot) {
        backpackComponent.unlockSlot(slot);
    }

    /**
     * Get number of unlocked backpack slots.
     *
     * @return Unlocked slot count
     */
    public int getUnlockedSlots() {
        return backpackComponent.getUnlockedSlots();
    }

    /**
     * Set number of unlocked backpack slots.
     *
     * @param count The slot count
     */
    public void setUnlockedSlots(int count) {
        backpackComponent.setUnlockedSlots(count);
    }

    /**
     * Check if a backpack exists in a slot.
     *
     * @param slot The slot number
     * @return true if exists
     */
    public boolean hasBackpack(int slot) {
        return backpackComponent.hasBackpack(slot);
    }

    /**
     * Get a backpack from a slot.
     *
     * @param slot The slot number
     * @return The backpack, or null
     */
    public Backpack getBackpack(int slot) {
        return backpackComponent.getBackpack(slot);
    }

    /**
     * Set a backpack in a slot.
     *
     * @param slot     The slot number
     * @param backpack The backpack
     */
    public void setBackpack(int slot, Backpack backpack) {
        backpackComponent.setBackpack(slot, backpack);
    }

    /**
     * Remove a backpack from a slot.
     *
     * @param slot The slot number
     */
    public void removeBackpack(int slot) {
        backpackComponent.removeBackpack(slot);
    }

    /**
     * Get the number of active backpacks.
     *
     * @return Active backpack count
     */
    public int getActiveBackpackCount() {
        return backpackComponent.getActiveBackpackCount();
    }

    /**
     * Get total storage slots across all backpacks.
     *
     * @return Total slot count
     */
    public int getTotalStorageSlots() {
        return backpackComponent.getTotalStorageSlots();
    }

    /**
     * Get total used slots across all backpacks.
     *
     * @return Total used slots
     */
    public int getTotalUsedSlots() {
        return backpackComponent.getTotalUsedSlots();
    }

    // ============================================
    //       Ender Chest Convenience Methods
    // ============================================

    /**
     * Get the ender chest data component.
     *
     * @return The ender chest component
     */
    public EnderChestDataComponent getEnderChestData() {
        return enderChestComponent;
    }

    /**
     * Check if an ender page is unlocked.
     *
     * @param page The page number
     * @return true if unlocked
     */
    public boolean isEnderPageUnlocked(int page) {
        return enderChestComponent.isPageUnlocked(page);
    }

    /**
     * Unlock an ender page.
     *
     * @param page The page number
     */
    public void unlockEnderPage(int page) {
        enderChestComponent.unlockPage(page);
    }

    /**
     * Get number of unlocked ender pages.
     *
     * @return Unlocked page count
     */
    public int getUnlockedEnderPages() {
        return enderChestComponent.getUnlockedPages();
    }

    /**
     * Set number of unlocked ender pages.
     *
     * @param count The page count
     */
    public void setUnlockedEnderPages(int count) {
        enderChestComponent.setUnlockedPages(count);
    }

    /**
     * Get an ender page.
     *
     * @param page The page number
     * @return The ender page
     */
    public EnderPage getEnderPage(int page) {
        return enderChestComponent.getPage(page);
    }

    /**
     * Check if an ender page exists.
     *
     * @param page The page number
     * @return true if exists
     */
    public boolean hasEnderPage(int page) {
        return enderChestComponent.hasPage(page);
    }

    /**
     * Set an ender page.
     *
     * @param page      The page number
     * @param enderPage The ender page
     */
    public void setEnderPage(int page, EnderPage enderPage) {
        enderChestComponent.setPage(page, enderPage);
    }

    /**
     * Get total ender storage slots.
     *
     * @return Total slot count
     */
    public int getTotalEnderStorageSlots() {
        return enderChestComponent.getTotalStorageSlots();
    }

    /**
     * Get total used ender slots.
     *
     * @return Total used slots
     */
    public int getTotalUsedEnderSlots() {
        return enderChestComponent.getTotalUsedSlots();
    }
}
