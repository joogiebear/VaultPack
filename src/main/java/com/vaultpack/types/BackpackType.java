package com.vaultpack.types;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private final List<String> recipe; // Crafting recipe (9 normalized items)
    private final String craftingPermission; // Permission required to craft

    public BackpackType(String fallbackId, ConfigurationSection config) {
        this.id = normalizeId(config.getString("id", fallbackId));
        this.displayName = config.getString("display-name", id);

        ConfigurationSection itemSection = config.getConfigurationSection("item");
        ConfigurationSection storageSection = config.getConfigurationSection("storage");
        ConfigurationSection upgradeSection = config.getConfigurationSection("upgrade");
        ConfigurationSection recipeSection = config.getConfigurationSection("recipe");

        // Parse material. New item-file format uses item.material; legacy backpacks.yml uses material.
        String materialName = getNestedString(itemSection, config, "material", "CHEST");
        try {
            this.material = Material.valueOf(stripMinecraftNamespace(materialName).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid material: " + materialName + " in backpack type: " + id);
        }

        this.texture = getNestedString(itemSection, config, "texture", null);
        this.lore = config.getStringList("lore");
        this.customModelData = getNestedInt(itemSection, config, "custom-model-data", 0);
        this.glow = getNestedBoolean(itemSection, config, "glow", false);

        String upgradeValue = config.getString("upgrade-from", null);
        if (upgradeValue == null && upgradeSection != null) {
            upgradeValue = upgradeSection.getString("from", null);
        }
        this.upgradeFrom = normalizeNullable(upgradeValue);

        this.recipe = loadRecipe(config, recipeSection);
        String permission = config.getString("crafting-permission", null);
        if (permission == null && recipeSection != null) {
            permission = recipeSection.getString("permission", null);
        }
        this.craftingPermission = normalizeNullable(permission);

        // Parse default tier. New item-file format may nest storage rows/size; legacy uses top-level fields.
        String tierName = config.getString("tier", id);
        int tierSize = config.getInt("size", 0);
        if (storageSection != null) {
            tierSize = storageSection.getInt("size", tierSize);
            int rows = storageSection.getInt("rows", 0);
            if (tierSize <= 0 && rows > 0) {
                tierSize = rows * 9;
            }
        }
        if (tierSize <= 0) {
            int rows = config.getInt("rows", 1);
            tierSize = rows * 9;
        }
        this.defaultTier = new BackpackTier(tierName, tierSize);
    }

    private static String getNestedString(ConfigurationSection nested, ConfigurationSection root, String key, String fallback) {
        if (nested != null && nested.contains(key)) {
            return nested.getString(key, fallback);
        }
        return root.getString(key, fallback);
    }

    private static int getNestedInt(ConfigurationSection nested, ConfigurationSection root, String key, int fallback) {
        if (nested != null && nested.contains(key)) {
            return nested.getInt(key, fallback);
        }
        return root.getInt(key, fallback);
    }

    private static boolean getNestedBoolean(ConfigurationSection nested, ConfigurationSection root, String key, boolean fallback) {
        if (nested != null && nested.contains(key)) {
            return nested.getBoolean(key, fallback);
        }
        return root.getBoolean(key, fallback);
    }

    private static List<String> loadRecipe(ConfigurationSection root, ConfigurationSection recipeSection) {
        if (recipeSection != null) {
            if (!recipeSection.getBoolean("enabled", true)) {
                return new ArrayList<>();
            }

            List<String> slots = recipeSection.getStringList("slots");
            if (!slots.isEmpty()) {
                List<String> normalized = new ArrayList<>();
                for (String ingredient : slots) {
                    normalized.add(normalizeIngredient(ingredient));
                }
                return normalized;
            }

            List<String> pattern = recipeSection.getStringList("pattern");
            ConfigurationSection ingredients = recipeSection.getConfigurationSection("ingredients");
            if (!pattern.isEmpty() && ingredients != null) {
                return loadPatternRecipe(pattern, ingredients);
            }
        }

        List<String> legacyRecipe = root.getStringList("recipe");
        List<String> normalized = new ArrayList<>();
        for (String ingredient : legacyRecipe) {
            normalized.add(normalizeIngredient(ingredient));
        }
        return normalized;
    }

    private static List<String> loadPatternRecipe(List<String> pattern, ConfigurationSection ingredients) {
        List<String> normalized = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            String row = rowIndex < pattern.size() ? pattern.get(rowIndex) : "";
            for (int column = 0; column < 3; column++) {
                char symbol = column < row.length() ? row.charAt(column) : ' ';
                if (symbol == ' ') {
                    normalized.add("");
                    continue;
                }

                String ingredient = ingredients.getString(String.valueOf(symbol), "");
                normalized.add(normalizeIngredient(ingredient));
            }
        }
        return normalized;
    }

    public static String normalizeIngredient(String ingredient) {
        if (ingredient == null) {
            return "";
        }

        String value = ingredient.trim();
        if (value.isEmpty() || value.equals("\"\"") || value.equals("''")) {
            return "";
        }

        // Preserve the existing runtime format: "material amount" or "plugin:id amount".
        String[] spaceParts = value.split("\\s+");
        String idPart = spaceParts[0];
        String amountPart = spaceParts.length > 1 ? spaceParts[1] : null;

        String[] colonParts = idPart.split(":");
        if (colonParts.length >= 3 && isInteger(colonParts[colonParts.length - 1])) {
            amountPart = colonParts[colonParts.length - 1];
            idPart = String.join(":", java.util.Arrays.copyOf(colonParts, colonParts.length - 1));
        }

        idPart = stripMinecraftNamespace(idPart).toLowerCase(Locale.ROOT);

        if (amountPart == null || amountPart.isBlank() || "1".equals(amountPart)) {
            return idPart;
        }
        return idPart + " " + amountPart;
    }

    private static String stripMinecraftNamespace(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("minecraft:")) {
            return trimmed.substring("minecraft:".length());
        }
        return trimmed;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("null") || trimmed.equalsIgnoreCase("none")) {
            return null;
        }
        return trimmed;
    }

    private static String normalizeId(String value) {
        if (value == null || value.isBlank()) {
            return "backpack";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
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
            if (name == null || name.isEmpty()) {
                return "Backpack";
            }
            return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
        }

        public int getSize() {
            return size;
        }
    }
}
