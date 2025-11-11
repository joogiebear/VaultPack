package com.vaultpack.models;

import org.bukkit.Material;

import java.util.List;

/**
 * v2.0.0: Represents a customizable GUI item from config
 */
public class GUIItem {
    private final boolean enabled;
    private final int slot;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final boolean glow;
    private final int customModelData;
    private final List<Integer> slots; // For items that appear in multiple slots (like borders)

    public GUIItem(boolean enabled, int slot, Material material, String name, List<String> lore,
                   boolean glow, int customModelData, List<Integer> slots) {
        this.enabled = enabled;
        this.slot = slot;
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.glow = glow;
        this.customModelData = customModelData;
        this.slots = slots;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSlot() {
        return slot;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }

    public boolean hasGlow() {
        return glow;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public boolean hasMultipleSlots() {
        return slots != null && !slots.isEmpty();
    }
}
