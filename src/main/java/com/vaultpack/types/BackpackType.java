package com.vaultpack.types;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class BackpackType {

    private final String id;
    private final String displayName;
    private final Material material;
    private final String texture; // Base64 texture for player heads
    private final List<String> lore;
    private final int customModelData;
    private final boolean glow;
    private final BackpackTier defaultTier;
    private final String upgradeFrom; // ID of backpack required to craft this one
    private final List<String> recipe; // Crafting recipe (9 items)
    private final String craftingPermission; // Permission required to craft

    public BackpackType(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display-name", id);

        // Parse material
        String materialName = config.getString("material", "CHEST");
        try {
            this.material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid material: " + materialName + " in backpack type: " + id);
        }

        this.texture = config.getString("texture", null);
        this.lore = config.getStringList("lore");
        this.customModelData = config.getInt("custom-model-data", 0);
        this.glow = config.getBoolean("glow", false);
        this.upgradeFrom = config.getString("upgrade-from", null);
        this.recipe = config.getStringList("recipe");
        this.craftingPermission = config.getString("crafting-permission", null);

        // Parse default tier
        String tierName = config.getString("tier", "small");
        int tierSize = config.getInt("size", 9);
        this.defaultTier = new BackpackTier(tierName, tierSize);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public List<String> getLore() {
        return new ArrayList<>(lore);
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean hasGlow() {
        return glow;
    }

    public BackpackTier getDefaultTier() {
        return defaultTier;
    }

    public String getTexture() {
        return texture;
    }

    public boolean hasTexture() {
        return texture != null && !texture.isEmpty();
    }

    public String getUpgradeFrom() {
        return upgradeFrom;
    }

    public boolean requiresUpgrade() {
        return upgradeFrom != null && !upgradeFrom.isEmpty();
    }

    public List<String> getRecipe() {
        return new ArrayList<>(recipe);
    }

    public boolean hasRecipe() {
        return recipe != null && !recipe.isEmpty();
    }

    public String getCraftingPermission() {
        return craftingPermission;
    }

    public boolean hasCraftingPermission() {
        return craftingPermission != null && !craftingPermission.isEmpty();
    }

    public static class BackpackTier {
        private final String name;
        private final int size;

        public BackpackTier(String name, int size) {
            this.name = name;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }

        public int getSize() {
            return size;
        }
    }
}
