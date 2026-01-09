package com.vaultpack.config;

import com.vaultpack.config.base.BaseConfig;
import com.vaultpack.config.base.IgnoreField;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * Configuration for all backpack types.
 * Manages loading, saving, and accessing backpack tier definitions.
 */
public class BackpacksConfig extends BaseConfig {

    @IgnoreField
    @Getter
    private final Map<String, BackpackType> backpackTypes = new LinkedHashMap<>();

    /**
     * Creates a new BackpacksConfig instance.
     *
     * @param file The backpacks.yml file
     */
    public BackpacksConfig(File file) {
        super(file);
    }

    @Override
    public void load() {
        super.load(); // Handle migrations
        loadBackpackTypes();
    }

    @Override
    public void save() {
        saveBackpackTypes();
        super.save(); // Save to file
    }

    /**
     * Load all backpack types from the backpacks section.
     */
    private void loadBackpackTypes() {
        backpackTypes.clear();

        YamlConfiguration yaml = getYaml();
        ConfigurationSection backpacksSection = yaml.getConfigurationSection("backpacks");

        if (backpacksSection == null) {
            getLogger().warning("No 'backpacks' section found in " + getFile().getName());
            return;
        }

        for (String id : backpacksSection.getKeys(false)) {
            ConfigurationSection section = backpacksSection.getConfigurationSection(id);
            if (section == null) continue;

            BackpackType type = new BackpackType(id);

            // Load basic properties
            type.setDisplayName(section.getString("display-name", "&7" + id));
            type.setRarity(section.getString("rarity", "&7Common"));
            type.setTier(section.getString("tier", id));
            type.setSize(section.getInt("size", 9));
            type.setRows(section.getInt("rows", 1));

            // Load item appearance
            String materialName = section.getString("material", "CHEST");
            try {
                type.setMaterial(Material.valueOf(materialName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid material '" + materialName + "' for backpack '" + id + "'. Using CHEST.");
                type.setMaterial(Material.CHEST);
            }

            type.setTexture(section.getString("texture"));
            type.setCustomModelData(section.getInt("custom-model-data", 0));
            type.setGlow(section.getBoolean("glow", false));

            // Load crafting
            type.setRecipe(section.getStringList("recipe"));
            type.setUpgradeFrom(section.getString("upgrade-from"));
            type.setCraftingPermission(section.getString("crafting-permission", "vaultpack.craft." + id));

            // Load lore
            type.setLore(section.getStringList("lore"));

            // Validate and add
            if (type.validate()) {
                backpackTypes.put(id, type);
            } else {
                getLogger().warning("Backpack '" + id + "' has validation errors. Check logs above.");
            }
        }

        getLogger().info("Loaded " + backpackTypes.size() + " backpack types");
    }

    /**
     * Save all backpack types to the backpacks section.
     */
    private void saveBackpackTypes() {
        YamlConfiguration yaml = getYaml();

        // Clear existing backpacks section
        yaml.set("backpacks", null);

        for (Map.Entry<String, BackpackType> entry : backpackTypes.entrySet()) {
            String id = entry.getKey();
            BackpackType type = entry.getValue();
            String path = "backpacks." + id;

            // Save basic properties
            yaml.set(path + ".display-name", type.getDisplayName());
            yaml.set(path + ".rarity", type.getRarity());
            yaml.set(path + ".tier", type.getTier());
            yaml.set(path + ".size", type.getSize());
            yaml.set(path + ".rows", type.getRows());

            // Save item appearance
            yaml.set(path + ".material", type.getMaterial().name());
            if (type.getTexture() != null) {
                yaml.set(path + ".texture", type.getTexture());
            }
            yaml.set(path + ".custom-model-data", type.getCustomModelData());
            yaml.set(path + ".glow", type.getGlow());

            // Save crafting
            if (type.getRecipe() != null && !type.getRecipe().isEmpty()) {
                yaml.set(path + ".recipe", type.getRecipe());
            }
            if (type.getUpgradeFrom() != null) {
                yaml.set(path + ".upgrade-from", type.getUpgradeFrom());
            }
            yaml.set(path + ".crafting-permission", type.getCraftingPermission());

            // Save lore
            if (type.getLore() != null && !type.getLore().isEmpty()) {
                yaml.set(path + ".lore", type.getLore());
            }
        }
    }

    /**
     * Get a backpack type by ID.
     *
     * @param id The backpack type ID
     * @return The BackpackType, or null if not found
     */
    public BackpackType getBackpackType(String id) {
        return backpackTypes.get(id);
    }

    /**
     * Check if a backpack type exists.
     *
     * @param id The backpack type ID
     * @return true if the type exists
     */
    public boolean hasBackpackType(String id) {
        return backpackTypes.containsKey(id);
    }

    /**
     * Get all backpack type IDs.
     *
     * @return Set of backpack type IDs
     */
    public Set<String> getBackpackTypeIds() {
        return backpackTypes.keySet();
    }

    /**
     * Get all backpack types ordered by size (smallest to largest).
     *
     * @return List of BackpackType ordered by size
     */
    public List<BackpackType> getBackpackTypesBySize() {
        List<BackpackType> types = new ArrayList<>(backpackTypes.values());
        types.sort(Comparator.comparingInt(BackpackType::getSize));
        return types;
    }

    /**
     * Get the next tier upgrade for a backpack type.
     *
     * @param currentId The current backpack type ID
     * @return The next tier BackpackType, or null if this is the highest tier
     */
    public BackpackType getNextTier(String currentId) {
        for (BackpackType type : backpackTypes.values()) {
            if (currentId.equalsIgnoreCase(type.getUpgradeFrom())) {
                return type;
            }
        }
        return null;
    }

    /**
     * Get the previous tier for a backpack type.
     *
     * @param currentId The current backpack type ID
     * @return The previous tier BackpackType, or null if this is the lowest tier
     */
    public BackpackType getPreviousTier(String currentId) {
        BackpackType current = backpackTypes.get(currentId);
        if (current == null || current.getUpgradeFrom() == null) {
            return null;
        }
        return backpackTypes.get(current.getUpgradeFrom());
    }

    @Override
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        return Arrays.asList(
            // Migration 1: Ensure all backpack types have required fields
            (yaml) -> {
                ConfigurationSection backpacks = yaml.getConfigurationSection("backpacks");
                if (backpacks != null) {
                    for (String id : backpacks.getKeys(false)) {
                        String path = "backpacks." + id;

                        // Ensure crafting-permission exists
                        if (!yaml.contains(path + ".crafting-permission")) {
                            yaml.set(path + ".crafting-permission", "vaultpack.craft." + id);
                        }

                        // Ensure custom-model-data exists
                        if (!yaml.contains(path + ".custom-model-data")) {
                            yaml.set(path + ".custom-model-data", 0);
                        }

                        // Ensure glow exists
                        if (!yaml.contains(path + ".glow")) {
                            yaml.set(path + ".glow", false);
                        }
                    }
                }

                yaml.set("config-version", 1);
            }
        );
    }

    @IgnoreField
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("VaultPack");

    private java.util.logging.Logger getLogger() {
        return logger;
    }
}
