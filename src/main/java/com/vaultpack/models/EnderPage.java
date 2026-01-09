package com.vaultpack.models;

import org.bukkit.configuration.ConfigurationSection;
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

    /**
     * Serialize this ender page to a configuration section.
     *
     * @param section The configuration section to serialize to
     */
    public void serialize(ConfigurationSection section) {
        section.set("owner", ownerUUID.toString());
        section.set("page", pageNumber);

        // Serialize contents
        if (!contents.isEmpty()) {
            ConfigurationSection contentsSection = section.createSection("contents");
            for (Map.Entry<Integer, ItemStack> entry : contents.entrySet()) {
                if (entry.getValue() != null) {
                    contentsSection.set(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
    }

    /**
     * Deserialize an ender page from a configuration section.
     *
     * @param section The configuration section to deserialize from
     * @return The deserialized ender page
     */
    public static EnderPage deserialize(ConfigurationSection section) {
        UUID owner = UUID.fromString(section.getString("owner"));
        int page = section.getInt("page");

        EnderPage enderPage = new EnderPage(owner, page);

        // Deserialize contents
        ConfigurationSection contentsSection = section.getConfigurationSection("contents");
        if (contentsSection != null) {
            Map<Integer, ItemStack> contents = new HashMap<>();
            for (String key : contentsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = contentsSection.getItemStack(key);
                    if (item != null) {
                        contents.put(slot, item);
                    }
                } catch (NumberFormatException ignored) {
                    // Skip invalid slot numbers
                }
            }
            enderPage.setContents(contents);
        }

        return enderPage;
    }
}
