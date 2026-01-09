package com.vaultpack.data.components;

import com.vaultpack.models.Backpack;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Data component for managing player backpack slots and data.
 * Handles backpack storage, slot unlocking, and serialization.
 */
public class BackpackDataComponent extends BaseDataComponent {

    @Getter
    private int unlockedSlots;

    @Getter
    private final Map<Integer, Backpack> backpacks;

    public BackpackDataComponent() {
        super("backpacks");
        this.unlockedSlots = 1; // Default 1 unlocked slot
        this.backpacks = new HashMap<>();
    }

    /**
     * Check if a slot is unlocked.
     *
     * @param slot The slot number
     * @return true if unlocked
     */
    public boolean isSlotUnlocked(int slot) {
        return slot <= unlockedSlots;
    }

    /**
     * Unlock a slot.
     *
     * @param slot The slot number to unlock
     */
    public void unlockSlot(int slot) {
        if (slot > unlockedSlots) {
            unlockedSlots = slot;
            markDirty();
        }
    }

    /**
     * Set unlocked slots count.
     *
     * @param count The number of unlocked slots
     */
    public void setUnlockedSlots(int count) {
        if (this.unlockedSlots != count) {
            this.unlockedSlots = count;
            markDirty();
        }
    }

    /**
     * Check if a backpack exists in a slot.
     *
     * @param slot The slot number
     * @return true if a backpack exists
     */
    public boolean hasBackpack(int slot) {
        return backpacks.containsKey(slot);
    }

    /**
     * Get a backpack from a slot.
     *
     * @param slot The slot number
     * @return The backpack, or null if none exists
     */
    public Backpack getBackpack(int slot) {
        return backpacks.get(slot);
    }

    /**
     * Set a backpack in a slot.
     *
     * @param slot     The slot number
     * @param backpack The backpack to set
     */
    public void setBackpack(int slot, Backpack backpack) {
        backpacks.put(slot, backpack);
        markDirty();
    }

    /**
     * Remove a backpack from a slot.
     *
     * @param slot The slot number
     */
    public void removeBackpack(int slot) {
        if (backpacks.remove(slot) != null) {
            markDirty();
        }
    }

    /**
     * Get the number of active backpacks.
     *
     * @return Active backpack count
     */
    public int getActiveBackpackCount() {
        return backpacks.size();
    }

    /**
     * Get total storage slots across all backpacks.
     *
     * @return Total slot count
     */
    public int getTotalStorageSlots() {
        return backpacks.values().stream()
            .mapToInt(Backpack::getSize)
            .sum();
    }

    /**
     * Get total used slots across all backpacks.
     *
     * @return Total used slots
     */
    public int getTotalUsedSlots() {
        return backpacks.values().stream()
            .mapToInt(Backpack::getUsedSlots)
            .sum();
    }

    @Override
    public void load(ConfigurationSection section) {
        unlockedSlots = section.getInt("unlocked-slots", 1);

        ConfigurationSection backpacksSection = section.getConfigurationSection("slots");
        if (backpacksSection != null) {
            for (String slotKey : backpacksSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotKey);
                    ConfigurationSection backpackSection = backpacksSection.getConfigurationSection(slotKey);

                    if (backpackSection != null) {
                        Backpack backpack = Backpack.deserialize(backpackSection);
                        backpacks.put(slot, backpack);
                    }
                } catch (NumberFormatException e) {
                    // Invalid slot number, skip
                }
            }
        }
    }

    @Override
    public void save(ConfigurationSection section) {
        section.set("unlocked-slots", unlockedSlots);

        // Clear existing backpacks section
        section.set("slots", null);

        if (!backpacks.isEmpty()) {
            ConfigurationSection backpacksSection = section.createSection("slots");

            for (Map.Entry<Integer, Backpack> entry : backpacks.entrySet()) {
                ConfigurationSection backpackSection = backpacksSection.createSection(String.valueOf(entry.getKey()));
                entry.getValue().serialize(backpackSection);
            }
        }
    }

    @Override
    public void reset() {
        unlockedSlots = 1;
        backpacks.clear();
        markDirty();
    }

    @Override
    public boolean validate() {
        // Validate unlocked slots is positive
        if (unlockedSlots < 0) {
            return false;
        }

        // Validate all backpacks
        for (Backpack backpack : backpacks.values()) {
            if (backpack == null) {
                return false;
            }
        }

        return true;
    }
}
