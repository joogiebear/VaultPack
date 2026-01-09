package com.vaultpack.config;

import lombok.Data;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single backpack type configuration.
 * This is a data class that holds all properties for a backpack tier.
 */
@Data
public class BackpackType {
    private String id;
    private String displayName;
    private String rarity;
    private String tier;
    private Integer size;
    private Integer rows;

    // Item appearance
    private Material material;
    private String texture;
    private Integer customModelData;
    private Boolean glow;

    // Crafting
    private List<String> recipe;
    private String upgradeFrom;
    private String craftingPermission;

    // Display
    private List<String> lore;

    /**
     * Creates a new BackpackType with default values.
     *
     * @param id The backpack type ID (e.g., "small", "medium")
     */
    public BackpackType(String id) {
        this.id = id;
        this.displayName = "&7Backpack";
        this.rarity = "&7Common";
        this.tier = id;
        this.size = 9;
        this.rows = 1;
        this.material = Material.CHEST;
        this.texture = null;
        this.customModelData = 0;
        this.glow = false;
        this.recipe = new ArrayList<>();
        this.upgradeFrom = null;
        this.craftingPermission = "vaultpack.craft." + id;
        this.lore = new ArrayList<>();
    }

    /**
     * Check if this backpack type has a custom player head texture.
     *
     * @return true if texture is set and material is PLAYER_HEAD
     */
    public boolean hasCustomTexture() {
        return texture != null && !texture.isEmpty() && material == Material.PLAYER_HEAD;
    }

    /**
     * Check if this backpack type is an upgrade from another type.
     *
     * @return true if upgradeFrom is set
     */
    public boolean isUpgrade() {
        return upgradeFrom != null && !upgradeFrom.isEmpty();
    }

    /**
     * Check if this backpack type has a crafting recipe.
     *
     * @return true if recipe is not empty
     */
    public boolean hasCraftingRecipe() {
        return recipe != null && !recipe.isEmpty();
    }

    /**
     * Get the recipe as a 3x3 grid (9 elements).
     * Missing elements are filled with empty strings.
     *
     * @return Recipe grid with 9 elements
     */
    public List<String> getRecipeGrid() {
        List<String> grid = new ArrayList<>(recipe);

        // Ensure exactly 9 elements
        while (grid.size() < 9) {
            grid.add("");
        }

        // Trim to 9 if too many
        if (grid.size() > 9) {
            grid = grid.subList(0, 9);
        }

        return grid;
    }

    /**
     * Validates this backpack type configuration.
     *
     * @return true if valid, false if there are issues
     */
    public boolean validate() {
        boolean valid = true;

        // Validate size and rows match
        if (size != rows * 9) {
            System.err.println("Backpack " + id + ": size (" + size + ") doesn't match rows (" + rows + " * 9)");
            valid = false;
        }

        // Validate rows is between 1-6
        if (rows < 1 || rows > 6) {
            System.err.println("Backpack " + id + ": rows must be between 1-6, got " + rows);
            valid = false;
        }

        // Validate material
        if (material == null) {
            System.err.println("Backpack " + id + ": material cannot be null");
            valid = false;
        }

        // Validate recipe has 9 elements if present
        if (recipe != null && !recipe.isEmpty() && recipe.size() != 9) {
            System.err.println("Backpack " + id + ": recipe must have exactly 9 elements, got " + recipe.size());
            valid = false;
        }

        return valid;
    }
}
