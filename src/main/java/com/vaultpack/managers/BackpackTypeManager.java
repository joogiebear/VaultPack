package com.vaultpack.managers;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.types.BackpackType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackpackTypeManager {

    private final VaultPackPlugin plugin;
    private final Logger logger;
    private final Map<String, BackpackType> backpackTypes;

    public BackpackTypeManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.backpackTypes = new HashMap<>();
    }

    public void loadBackpackTypes() {
        backpackTypes.clear();

        // Save default backpacks.yml if it doesn't exist
        File backpacksFile = new File(plugin.getDataFolder(), "backpacks.yml");
        if (!backpacksFile.exists()) {
            plugin.saveResource("backpacks.yml", false);
            logger.info("Created default backpacks.yml");
        }

        // Load the backpacks.yml file
        YamlConfiguration config = YamlConfiguration.loadConfiguration(backpacksFile);

        // Get the backpacks section
        ConfigurationSection backpacksSection = config.getConfigurationSection("backpacks");
        if (backpacksSection == null) {
            logger.warning("No 'backpacks' section found in backpacks.yml!");
            return;
        }

        // Load each backpack type
        int loaded = 0;
        for (String id : backpacksSection.getKeys(false)) {
            try {
                ConfigurationSection typeSection = backpacksSection.getConfigurationSection(id);
                if (typeSection == null) {
                    logger.warning("Invalid backpack type section: " + id);
                    continue;
                }

                BackpackType type = new BackpackType(id, typeSection);
                backpackTypes.put(id, type);
                loaded++;
            } catch (Exception e) {
                logger.warning("Failed to load backpack type '" + id + "': " + e.getMessage());
            }
        }

        logger.info("Loaded " + loaded + " backpack types");

        // Register crafting recipes
        registerRecipes();
    }

    private void registerRecipes() {
        int registered = 0;

        for (BackpackType type : backpackTypes.values()) {
            if (!type.hasRecipe()) {
                continue;
            }

            try {
                List<String> recipeList = type.getRecipe();
                if (recipeList.size() != 9) {
                    logger.warning("Invalid recipe for " + type.getId() + ": must have exactly 9 items");
                    continue;
                }

                // Check if recipe contains custom items from other plugins (ecoitems, etc.)
                boolean hasCustomPluginItems = false;
                for (String ingredient : recipeList) {
                    String materialName = ingredient.trim().split(" ")[0];
                    if (materialName.contains(":") && !materialName.startsWith("vaultpack:")) {
                        hasCustomPluginItems = true;
                        break;
                    }
                }

                // Skip vanilla recipe registration for recipes with custom plugin items
                // They will be handled purely by CraftingListener
                if (hasCustomPluginItems) {
                    continue;
                }

                // Create the result item
                ItemStack result = createBackpackItem(type);

                // Create a namespaced key for the recipe
                NamespacedKey key = new NamespacedKey(plugin, type.getId());

                // Remove existing recipe if present (for reload support)
                Bukkit.removeRecipe(key);

                // Create shaped recipe
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                recipe.shape("ABC", "DEF", "GHI");

                // Parse and set recipe ingredients (just for display - CraftingListener validates amounts)
                char[] slots = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
                for (int i = 0; i < 9; i++) {
                    String ingredientStr = recipeList.get(i).trim();
                    setRecipeIngredient(recipe, slots[i], ingredientStr);
                }

                // Register the recipe
                Bukkit.addRecipe(recipe);
                registered++;
            } catch (Exception e) {
                logger.warning("Failed to register recipe for " + type.getId() + ": " + e.getMessage());
            }
        }

        if (registered > 0) {
            logger.info("Registered " + registered + " recipes");
        }
    }

    private void setRecipeIngredient(ShapedRecipe recipe, char slot, String ingredientStr) {
        // Parse ingredient string (format: "material amount" or "plugin:item_id" or "material")
        String[] parts = ingredientStr.split(" ");
        String materialName = parts[0];

        // Handle empty slots
        if (materialName.isEmpty() || materialName.equals("\"\"")) {
            return;
        }

        // Check if it's a custom item
        if (materialName.contains(":")) {
            String[] pluginParts = materialName.split(":");
            String pluginName = pluginParts[0].toLowerCase();

            if (pluginName.equals("vaultpack")) {
                String backpackId = pluginParts[1];
                BackpackType backpackType = backpackTypes.get(backpackId);

                if (backpackType != null) {
                    recipe.setIngredient(slot, backpackType.getMaterial());
                } else {
                    logger.warning("Unknown backpack type in recipe: " + backpackId);
                }
            } else {
                // For ecoitems or other plugins, just use a generic material
                // The CraftingListener will handle the actual validation
                recipe.setIngredient(slot, Material.PAPER); // Placeholder
            }
        } else {
            // Regular vanilla material
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                recipe.setIngredient(slot, material);
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown material in recipe: " + materialName);
            }
        }
    }

    public ItemStack createBackpackItem(BackpackType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(type.getDisplayName().replace("&", "§"));

            // Apply custom model data if present
            if (type.getCustomModelData() > 0) {
                meta.setCustomModelData(type.getCustomModelData());
            }

            // Apply texture for player heads
            if (type.getMaterial() == Material.PLAYER_HEAD && type.hasTexture() && meta instanceof SkullMeta) {
                SkullMeta skullMeta = (SkullMeta) meta;
                applySkullTexture(skullMeta, type.getTexture());
            }

            // Apply lore with placeholder replacement
            List<String> lore = type.getLore();
            for (int i = 0; i < lore.size(); i++) {
                lore.set(i, lore.get(i)
                    .replace("&", "§")
                    .replace("%tier%", type.getDefaultTier().getDisplayName())
                    .replace("%size%", String.valueOf(type.getDefaultTier().getSize()))
                    .replace("%used%", "0"));
            }
            meta.setLore(lore);

            // Store backpack type ID in NBT (required for placement detection)
            org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(plugin, "backpack_type");
            container.set(key, org.bukkit.persistence.PersistentDataType.STRING, type.getId());

            item.setItemMeta(meta);
        }

        return item;
    }

    private void applySkullTexture(SkullMeta skullMeta, String base64Texture) {
        try {
            // Decode the base64 texture to extract the URL
            String decoded = new String(Base64.getDecoder().decode(base64Texture), StandardCharsets.UTF_8);

            // Extract the texture URL from the JSON
            Pattern pattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(decoded);

            if (matcher.find()) {
                String textureUrl = matcher.group(1);

                // Create a PlayerProfile with the texture
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URL(textureUrl));
                profile.setTextures(textures);

                // Apply the profile to the skull
                skullMeta.setOwnerProfile(profile);
            } else {
                logger.warning("Could not extract texture URL from base64 texture");
            }
        } catch (MalformedURLException e) {
            logger.warning("Invalid texture URL: " + e.getMessage());
        } catch (Exception e) {
            logger.warning("Failed to apply skull texture: " + e.getMessage());
        }
    }

    public BackpackType getBackpackType(String id) {
        return backpackTypes.get(id);
    }

    public Map<String, BackpackType> getAllBackpackTypes() {
        return new HashMap<>(backpackTypes);
    }

    public boolean hasBackpackType(String id) {
        return backpackTypes.containsKey(id);
    }
}
