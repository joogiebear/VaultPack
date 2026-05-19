package com.vaultpack.managers;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.types.BackpackType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackpackTypeManager {

    private static final String[] DEFAULT_BACKPACK_FILES = {
        "small.yml",
        "medium.yml",
        "large.yml",
        "greater.yml",
        "jumbo.yml"
    };

    private final VaultPackPlugin plugin;
    private final Logger logger;
    private final Map<String, BackpackType> backpackTypes;

    public BackpackTypeManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.backpackTypes = new LinkedHashMap<>();
    }

    public void loadBackpackTypes() {
        backpackTypes.clear();
        ensureDefaultBackpackFiles();

        int legacyLoaded = loadLegacyBackpacksFile();
        int fileLoaded = loadBackpackDirectory();

        logger.info("Loaded " + backpackTypes.size() + " backpack types (" + legacyLoaded + " legacy, " + fileLoaded + " item files)");

        // Register crafting recipes
        registerRecipes();
    }

    private void ensureDefaultBackpackFiles() {
        File folder = getBackpacksFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            logger.warning("Could not create backpacks folder: " + folder.getPath());
            return;
        }

        File[] ymlFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (ymlFiles != null && ymlFiles.length > 0) {
            return;
        }

        if (hasLegacyBackpacksFile()) {
            logger.info("Using legacy backpacks.yml because backpacks/ is empty. Run /vaultpack migrate to split it into item files.");
            return;
        }

        for (String fileName : DEFAULT_BACKPACK_FILES) {
            String resourcePath = "backpacks/" + fileName;
            File target = new File(folder, fileName);
            if (!target.exists()) {
                try {
                    plugin.saveResource(resourcePath, false);
                    logger.info("Created default " + resourcePath);
                } catch (IllegalArgumentException e) {
                    logger.warning("Default resource missing from jar: " + resourcePath);
                }
            }
        }
    }

    private boolean hasLegacyBackpacksFile() {
        File backpacksFile = new File(plugin.getDataFolder(), "backpacks.yml");
        if (!backpacksFile.exists()) {
            return false;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(backpacksFile);
        ConfigurationSection backpacksSection = config.getConfigurationSection("backpacks");
        return backpacksSection != null && !backpacksSection.getKeys(false).isEmpty();
    }

    private int loadLegacyBackpacksFile() {
        File backpacksFile = new File(plugin.getDataFolder(), "backpacks.yml");
        if (!backpacksFile.exists()) {
            return 0;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(backpacksFile);
        ConfigurationSection backpacksSection = config.getConfigurationSection("backpacks");
        if (backpacksSection == null) {
            logger.warning("Legacy backpacks.yml exists but has no 'backpacks' section; ignoring it.");
            return 0;
        }

        int loaded = 0;
        for (String id : backpacksSection.getKeys(false)) {
            ConfigurationSection typeSection = backpacksSection.getConfigurationSection(id);
            if (typeSection == null) {
                logger.warning("Invalid legacy backpack type section: " + id);
                continue;
            }

            if (loadBackpackType(id, typeSection, backpacksFile.getName(), false)) {
                loaded++;
            }
        }
        return loaded;
    }

    private int loadBackpackDirectory() {
        File folder = getBackpacksFolder();
        List<File> files = new ArrayList<>();
        collectYamlFiles(folder, files);

        int loaded = 0;
        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            String fallbackId = stripYamlExtension(file.getName());
            ConfigurationSection section = config;
            if (config.isConfigurationSection("backpack")) {
                section = config.getConfigurationSection("backpack");
            }
            if (section == null) {
                logger.warning("Skipping invalid backpack file " + relativePath(file) + ": missing backpack section");
                continue;
            }

            String id = section.getString("id", fallbackId);
            if (loadBackpackType(id, section, relativePath(file), true)) {
                loaded++;
            }
        }
        return loaded;
    }

    private boolean loadBackpackType(String fallbackId, ConfigurationSection typeSection, String source, boolean overrideExisting) {
        try {
            BackpackType type = new BackpackType(fallbackId, typeSection);
            if (backpackTypes.containsKey(type.getId())) {
                if (!overrideExisting) {
                    logger.warning("Duplicate legacy backpack type '" + type.getId() + "' in " + source + "; keeping first definition.");
                    return false;
                }
                logger.info("Backpack item file " + source + " overrides legacy backpack type '" + type.getId() + "'.");
            }

            List<String> validationErrors = validateBackpackType(type, source);
            if (!validationErrors.isEmpty()) {
                for (String error : validationErrors) {
                    logger.warning(error);
                }
                return false;
            }

            backpackTypes.put(type.getId(), type);
            return true;
        } catch (Exception e) {
            logger.warning("Failed to load backpack type from " + source + ": " + e.getMessage());
            return false;
        }
    }

    private List<String> validateBackpackType(BackpackType type, String source) {
        List<String> errors = new ArrayList<>();
        if (type.getId() == null || type.getId().isBlank()) {
            errors.add(source + ": backpack id cannot be blank");
        }
        if (type.getMaterial() == null) {
            errors.add(source + ": material cannot be null for backpack '" + type.getId() + "'");
        }
        int size = type.getDefaultTier().getSize();
        if (size <= 0 || size % 9 != 0) {
            errors.add(source + ": backpack '" + type.getId() + "' size must be a positive multiple of 9 (found " + size + ")");
        }
        if (size > 54) {
            errors.add(source + ": backpack '" + type.getId() + "' size cannot be larger than 54 slots (found " + size + ")");
        }
        if (type.hasRecipe() && type.getRecipe().size() != 9) {
            errors.add(source + ": backpack '" + type.getId() + "' recipe must have exactly 9 slots");
        }
        return errors;
    }

    public List<String> validateBackpackDefinitions() {
        List<String> messages = new ArrayList<>();
        if (backpackTypes.isEmpty()) {
            messages.add("No backpack types are loaded.");
            return messages;
        }

        for (BackpackType type : backpackTypes.values()) {
            List<String> errors = validateBackpackType(type, type.getId());
            if (errors.isEmpty()) {
                messages.add("OK: " + type.getId() + " (" + type.getDefaultTier().getSize() + " slots)");
            } else {
                messages.addAll(errors);
            }
        }
        return messages;
    }

    public boolean migrateLegacyBackpacksToItemFiles() {
        File legacyFile = new File(plugin.getDataFolder(), "backpacks.yml");
        if (!legacyFile.exists()) {
            return false;
        }

        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(legacyFile);
        ConfigurationSection backpacks = legacy.getConfigurationSection("backpacks");
        if (backpacks == null) {
            return false;
        }

        File folder = getBackpacksFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            return false;
        }

        for (String id : backpacks.getKeys(false)) {
            ConfigurationSection source = backpacks.getConfigurationSection(id);
            if (source == null) {
                continue;
            }

            File target = new File(folder, id.toLowerCase() + ".yml");
            if (target.exists()) {
                continue;
            }

            YamlConfiguration output = new YamlConfiguration();
            output.set("id", id.toLowerCase());
            output.set("display-name", source.getString("display-name", "&7" + id));
            output.set("rarity", source.getString("rarity", "&7Common"));
            output.set("tier", source.getString("tier", id));
            output.set("storage.size", source.getInt("size", source.getInt("rows", 1) * 9));
            output.set("storage.rows", source.getInt("rows", Math.max(1, source.getInt("size", 9) / 9)));
            output.set("item.material", source.getString("material", "CHEST"));
            output.set("item.texture", source.getString("texture", null));
            output.set("item.custom-model-data", source.getInt("custom-model-data", 0));
            output.set("item.glow", source.getBoolean("glow", false));
            output.set("recipe.enabled", !source.getStringList("recipe").isEmpty());
            output.set("recipe.permission", source.getString("crafting-permission", "vaultpack.craft." + id));
            output.set("recipe.slots", source.getStringList("recipe"));
            output.set("upgrade.from", source.getString("upgrade-from", null));
            output.set("lore", source.getStringList("lore"));

            try {
                output.save(target);
            } catch (IOException e) {
                logger.warning("Failed to migrate backpack '" + id + "' to " + target.getName() + ": " + e.getMessage());
                return false;
            }
        }

        File backup = new File(plugin.getDataFolder(), "backpacks.yml.bak");
        if (!backup.exists() && !legacyFile.renameTo(backup)) {
            logger.warning("Migrated backpack files, but could not rename backpacks.yml to backpacks.yml.bak");
        }
        return true;
    }

    private void collectYamlFiles(File folder, List<File> files) {
        File[] children = folder.listFiles();
        if (children == null) {
            return;
        }

        java.util.Arrays.sort(children, java.util.Comparator.comparing(File::getName));
        for (File child : children) {
            if (child.isDirectory()) {
                collectYamlFiles(child, files);
            } else if (child.getName().toLowerCase().endsWith(".yml")) {
                files.add(child);
            }
        }
    }

    private File getBackpacksFolder() {
        return new File(plugin.getDataFolder(), "backpacks");
    }

    private String relativePath(File file) {
        String base = plugin.getDataFolder().getAbsolutePath();
        String absolute = file.getAbsolutePath();
        if (absolute.startsWith(base)) {
            return absolute.substring(base.length() + 1);
        }
        return file.getName();
    }

    private String stripYamlExtension(String fileName) {
        if (fileName.toLowerCase().endsWith(".yml")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
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

                // Skip vanilla recipe registration for recipes with custom plugin items.
                // They will be handled purely by CraftingListener.
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
        // Parse normalized ingredient string (format: "material amount", "plugin:item_id amount", or "material")
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
                // For ecoitems or other plugins, just use a generic material.
                // The CraftingListener will handle the actual validation.
                recipe.setIngredient(slot, Material.PAPER);
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
                meta = skullMeta;
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

            if (type.hasGlow()) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

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

    public boolean hasBackpackType(String id) {
        return backpackTypes.containsKey(id);
    }

    public Map<String, BackpackType> getAllBackpackTypes() {
        return new HashMap<>(backpackTypes);
    }
}
