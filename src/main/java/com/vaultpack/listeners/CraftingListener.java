package com.vaultpack.listeners;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.types.BackpackType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles custom recipe validation and item consumption for backpacks with amounts
 */
public class CraftingListener implements Listener {

    private final VaultPackPlugin plugin;
    private final Map<String, RecipeRequirements> recipeCache = new HashMap<>();
    private final boolean ecoItemsAvailable;

    public CraftingListener(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.ecoItemsAvailable = Bukkit.getPluginManager().getPlugin("EcoItems") != null;
        if (ecoItemsAvailable) {
            plugin.getLogger().info("EcoItems detected - custom item support enabled");
        }
        cacheRecipeRequirements();
    }

    private void cacheRecipeRequirements() {
        for (BackpackType type : plugin.getBackpackTypeManager().getAllBackpackTypes().values()) {
            if (type.hasRecipe()) {
                RecipeRequirements requirements = new RecipeRequirements();
                requirements.backpackType = type;
                requirements.parseRecipe(type.getRecipe());
                recipeCache.put(type.getId(), requirements);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        // FIRST: Try custom recipes (for EcoItems and exact matching)
        // Sort recipes by total required items (descending) to check most specific first
        java.util.List<Map.Entry<String, RecipeRequirements>> sortedRecipes = new java.util.ArrayList<>(recipeCache.entrySet());
        sortedRecipes.sort((e1, e2) -> {
            int total1 = e1.getValue().getTotalRequiredItems();
            int total2 = e2.getValue().getTotalRequiredItems();
            return Integer.compare(total2, total1); // Descending order (most items first)
        });

        for (Map.Entry<String, RecipeRequirements> entry : sortedRecipes) {
            RecipeRequirements requirements = entry.getValue();

            if (requirements.matchesPattern(matrix)) {
                BackpackType backpackType = requirements.backpackType;
                validateAndSetResult(event, backpackType, matrix);
                return;
            }
        }

        // SECOND: Fall back to vanilla recipe (for simple recipes without EcoItems)
        Recipe recipe = event.getRecipe();

        if (recipe != null && recipe.getResult() != null) {
            // Find the matching backpack type
            BackpackType backpackType = findBackpackTypeByResult(recipe.getResult());
            if (backpackType != null && backpackType.hasRecipe()) {
                validateAndSetResult(event, backpackType, matrix);
                return;
            }
        }
    }

    private void validateAndSetResult(PrepareItemCraftEvent event, BackpackType backpackType, ItemStack[] matrix) {
        CraftingInventory inventory = event.getInventory();

        // Check permission
        if (backpackType.hasCraftingPermission() && event.getView().getPlayer() instanceof Player) {
            Player player = (Player) event.getView().getPlayer();
            if (!player.hasPermission(backpackType.getCraftingPermission())) {
                inventory.setResult(null);
                return;
            }
        }

        // Validate the recipe with amounts (show preview)
        RecipeRequirements requirements = recipeCache.get(backpackType.getId());
        if (requirements == null) {
            inventory.setResult(null);
            return;
        }

        boolean hasItems = requirements.hasRequiredItems(matrix);

        if (hasItems) {
            // Set the result to show the preview
            ItemStack result = createBackpackResult(backpackType);
            inventory.setResult(result);
        } else {
            // Not enough items - cancel the preview
            inventory.setResult(null);
        }
    }

    private ItemStack createBackpackResult(BackpackType type) {
        // Create the backpack item using the type manager's method
        return plugin.getBackpackTypeManager().getAllBackpackTypes().get(type.getId()) != null ?
            createBackpackItemForType(type) : new ItemStack(Material.AIR);
    }

    private ItemStack createBackpackItemForType(BackpackType type) {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        // Handle player head with custom texture
        if (type.getMaterial() == Material.PLAYER_HEAD && type.hasTexture() && meta instanceof org.bukkit.inventory.meta.SkullMeta) {
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) meta;
            applySkullTexture(skullMeta, type.getTexture());
            meta = skullMeta;
        }

        // Set display name
        meta.setDisplayName(type.getDisplayName().replace("&", "§"));

        // Set lore (replace placeholders)
        java.util.List<String> lore = new java.util.ArrayList<>();
        for (String line : type.getLore()) {
            String formatted = line
                    .replace("%tier%", type.getDefaultTier().getDisplayName())
                    .replace("%size%", String.valueOf(type.getDefaultTier().getSize()))
                    .replace("%used%", "0")
                    .replace("&", "§");
            lore.add(formatted);
        }
        meta.setLore(lore);

        // Add custom model data
        if (type.getCustomModelData() > 0) {
            meta.setCustomModelData(type.getCustomModelData());
        }

        // Add glow effect
        if (type.hasGlow()) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        // Store backpack type ID in NBT
        org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(
                new org.bukkit.NamespacedKey(plugin, "backpack_type"),
                org.bukkit.persistence.PersistentDataType.STRING,
                type.getId()
        );

        item.setItemMeta(meta);
        return item;
    }

    private void applySkullTexture(org.bukkit.inventory.meta.SkullMeta skullMeta, String base64Texture) {
        try {
            // Decode the base64 texture to extract the URL
            String decoded = new String(java.util.Base64.getDecoder().decode(base64Texture), java.nio.charset.StandardCharsets.UTF_8);

            // Extract the texture URL from the JSON
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
            java.util.regex.Matcher matcher = pattern.matcher(decoded);

            if (matcher.find()) {
                String textureUrl = matcher.group(1);

                // Create a PlayerProfile with the texture
                org.bukkit.profile.PlayerProfile profile = Bukkit.createPlayerProfile(java.util.UUID.randomUUID());
                org.bukkit.profile.PlayerTextures textures = profile.getTextures();
                textures.setSkin(new java.net.URL(textureUrl));
                profile.setTextures(textures);

                // Apply the profile to the skull
                skullMeta.setOwnerProfile(profile);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply skull texture: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        // Check if this is a custom backpack recipe
        BackpackType backpackType = null;
        RecipeRequirements requirements = null;
        Recipe recipe = event.getRecipe();

        // FIRST: Try custom recipe discovery (for EcoItems and exact matching)
        // Sort recipes by total required items (descending) to check most specific first
        java.util.List<Map.Entry<String, RecipeRequirements>> sortedRecipes = new java.util.ArrayList<>(recipeCache.entrySet());
        sortedRecipes.sort((e1, e2) -> {
            int total1 = e1.getValue().getTotalRequiredItems();
            int total2 = e2.getValue().getTotalRequiredItems();
            return Integer.compare(total2, total1); // Descending order (most items first)
        });

        for (Map.Entry<String, RecipeRequirements> entry : sortedRecipes) {
            if (entry.getValue().matchesPattern(matrix)) {
                backpackType = entry.getValue().backpackType;
                requirements = entry.getValue();
                break;
            }
        }

        // SECOND: If no custom match, try vanilla recipe
        if (backpackType == null) {
            if (recipe != null && recipe.getResult() != null) {
                backpackType = findBackpackTypeByResult(recipe.getResult());
                if (backpackType != null && backpackType.hasRecipe()) {
                    requirements = recipeCache.get(backpackType.getId());
                }
            }
        }

        // If no backpack recipe found, let Bukkit handle it normally
        if (backpackType == null || requirements == null) {
            return;
        }

        // Validate items
        if (!requirements.hasRequiredItems(matrix)) {
            event.setCancelled(true);
            return;
        }

        // Check if this is a vanilla-registered recipe or custom-only recipe
        boolean hasVanillaRecipe = (recipe != null && recipe.getResult() != null &&
                                   findBackpackTypeByResult(recipe.getResult()) != null);

        // For recipes with custom items or high amounts, ensure the result is given
        // The vanilla recipe might have placeholders, so we need to set the correct result
        boolean hasCustomItems = hasCustomItemsInRecipe(requirements);
        boolean hasHighAmounts = hasHighAmounts(requirements);

        if (!hasVanillaRecipe || hasCustomItems || hasHighAmounts) {
            // Cancel vanilla crafting and give result manually
            event.setCancelled(true);

            // Create the backpack result
            ItemStack result = createBackpackResult(backpackType);

            // Give to player
            Player player = (Player) event.getWhoClicked();

            // Consume ingredients first
            for (int i = 0; i < 9; i++) {
                ItemStack slot = matrix[i];
                if (slot != null && slot.getType() != Material.AIR) {
                    int requiredAmount = requirements.amounts[i];
                    if (requiredAmount > 0) {
                        slot.setAmount(slot.getAmount() - requiredAmount);
                        if (slot.getAmount() <= 0) {
                            matrix[i] = null;
                        }
                    }
                }
            }

            // Update the crafting inventory
            inventory.setMatrix(matrix);

            // Give result on next tick (after event is processed)
            // Use player scheduler for Folia compatibility
            player.getScheduler().execute(plugin, () -> {
                if (event.isShiftClick()) {
                    // Shift-click - add to inventory
                    java.util.HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(result);
                    if (!overflow.isEmpty()) {
                        // Inventory full - drop item
                        player.getWorld().dropItem(player.getLocation(), result);
                    }
                } else {
                    // Normal click - put on cursor
                    player.setItemOnCursor(result);
                }
            }, null, 1L);
        }
    }

    private boolean hasCustomItemsInRecipe(RecipeRequirements requirements) {
        for (int i = 0; i < requirements.materials.length; i++) {
            String material = requirements.materials[i];
            if (material.contains(":") && !material.startsWith("vaultpack:")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasHighAmounts(RecipeRequirements requirements) {
        for (int amount : requirements.amounts) {
            if (amount > 1) {
                return true;
            }
        }
        return false;
    }

    private int calculateMaxCrafts(ItemStack[] matrix, RecipeRequirements requirements) {
        int maxCrafts = Integer.MAX_VALUE;

        for (int i = 0; i < 9; i++) {
            ItemStack slot = matrix[i];
            int requiredAmount = requirements.amounts[i];

            if (requiredAmount > 0 && slot != null) {
                int possible = slot.getAmount() / requiredAmount;
                maxCrafts = Math.min(maxCrafts, possible);
            }
        }

        return maxCrafts == Integer.MAX_VALUE ? 0 : maxCrafts;
    }

    private BackpackType findBackpackTypeByResult(ItemStack result) {
        for (BackpackType type : plugin.getBackpackTypeManager().getAllBackpackTypes().values()) {
            if (type.getMaterial() == result.getType() &&
                result.hasItemMeta() &&
                result.getItemMeta().hasDisplayName()) {

                String resultName = result.getItemMeta().getDisplayName();
                String typeName = type.getDisplayName().replace("&", "§");

                if (resultName.equals(typeName)) {
                    return type;
                }
            }
        }
        return null;
    }

    private static class RecipeRequirements {
        BackpackType backpackType;
        String[] materials = new String[9];
        int[] amounts = new int[9];

        int getTotalRequiredItems() {
            int total = 0;
            for (int amount : amounts) {
                total += amount;
            }
            return total;
        }

        void parseRecipe(List<String> recipeList) {
            for (int i = 0; i < 9 && i < recipeList.size(); i++) {
                String ingredientStr = recipeList.get(i).trim();

                if (ingredientStr.isEmpty() || ingredientStr.equals("\"\"")) {
                    materials[i] = "";
                    amounts[i] = 0;
                    continue;
                }

                String[] parts = ingredientStr.split(" ");
                materials[i] = parts[0];
                amounts[i] = 1;

                if (parts.length > 1) {
                    try {
                        amounts[i] = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        amounts[i] = 1;
                    }
                }
            }
        }

        boolean matchesPattern(ItemStack[] matrix) {
            // Quick pattern match - just check if the item types/custom items match (ignore amounts)
            for (int i = 0; i < 9; i++) {
                String requiredMaterial = materials[i];
                ItemStack slot = matrix[i];

                // Empty slot required
                if (requiredMaterial.isEmpty()) {
                    if (slot != null && slot.getType() != Material.AIR) {
                        return false;
                    }
                    continue;
                }

                // Slot should have something
                if (slot == null || slot.getType() == Material.AIR) {
                    return false;
                }

                // Check material type (ignore amount for pattern match)
                if (requiredMaterial.contains(":")) {
                    if (!validateCustomItem(requiredMaterial, slot)) {
                        return false;
                    }
                } else {
                    try {
                        Material required = Material.valueOf(requiredMaterial.toUpperCase());
                        if (slot.getType() != required) {
                            return false;
                        }
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                }
            }
            return true;
        }

        boolean hasRequiredItems(ItemStack[] matrix) {
            for (int i = 0; i < 9; i++) {
                String requiredMaterial = materials[i];
                int requiredAmount = amounts[i];

                ItemStack slot = matrix[i];

                // Empty slot required
                if (requiredMaterial.isEmpty()) {
                    if (slot != null && slot.getType() != Material.AIR) {
                        return false; // Slot should be empty
                    }
                    continue;
                }

                // Slot should have something
                if (slot == null || slot.getType() == Material.AIR) {
                    return false;
                }

                // Check amount
                if (slot.getAmount() < requiredAmount) {
                    return false;
                }

                // Check material type
                if (requiredMaterial.contains(":")) {
                    // Custom item validation
                    if (!validateCustomItem(requiredMaterial, slot)) {
                        return false;
                    }
                } else {
                    // Vanilla material
                    try {
                        Material required = Material.valueOf(requiredMaterial.toUpperCase());
                        if (slot.getType() != required) {
                            return false;
                        }
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                }
            }

            return true;
        }

        private boolean validateCustomItem(String requiredId, ItemStack item) {
            String[] parts = requiredId.split(":");
            String pluginName = parts[0].toLowerCase();

            if (pluginName.equals("ecoitems")) {
                // Use EcoItems API via reflection (to avoid hard dependency)
                try {
                    String itemId = parts[1];

                    // Get EcoItems class and INSTANCE field
                    Class<?> ecoItemsClass = Class.forName("com.willfp.ecoitems.items.EcoItems");

                    // Try to get INSTANCE field (singleton pattern)
                    Object instance;
                    try {
                        java.lang.reflect.Field instanceField = ecoItemsClass.getField("INSTANCE");
                        instance = instanceField.get(null);
                    } catch (NoSuchFieldException e) {
                        // Fallback: try static method
                        instance = null;
                    }

                    // Call getByID on instance or as static method
                    Method getByIDMethod = ecoItemsClass.getMethod("getByID", String.class);
                    Object ecoItem;

                    if (instance != null) {
                        ecoItem = getByIDMethod.invoke(instance, itemId);
                    } else {
                        ecoItem = getByIDMethod.invoke(null, itemId);
                    }

                    if (ecoItem == null) {
                        Bukkit.getLogger().warning("[VaultPack] EcoItem not found with ID: '" + itemId + "'");
                        Bukkit.getLogger().warning("[VaultPack] Make sure the 'id' field in the YML matches '" + itemId + "'");
                        return false;
                    }

                    // EcoItems uses a different pattern - get the EcoItem from the ItemStack and compare
                    // Try multiple possible API patterns

                    // Pattern 1: Try Items.getFromStack(itemStack) and compare
                    try {
                        Class<?> itemsClass = Class.forName("com.willfp.ecoitems.items.Items");
                        Method getFromStackMethod = itemsClass.getMethod("getFromStack", ItemStack.class);
                        Object itemFromStack = getFromStackMethod.invoke(null, item);

                        return itemFromStack != null && itemFromStack.equals(ecoItem);
                    } catch (Exception e1) {
                        // Pattern 2: Try comparing ItemStack from EcoItem
                        try {
                            Method getItemStackMethod = ecoItem.getClass().getMethod("getItemStack");
                            ItemStack ecoItemStack = (ItemStack) getItemStackMethod.invoke(ecoItem);

                            if (ecoItemStack != null) {
                                // Compare using ItemStack similarity (same material and meta)
                                return item.getType() == ecoItemStack.getType() &&
                                    item.hasItemMeta() && ecoItemStack.hasItemMeta() &&
                                    item.getItemMeta().getDisplayName().equals(ecoItemStack.getItemMeta().getDisplayName());
                            }
                        } catch (Exception e2) {
                            Bukkit.getLogger().warning("[VaultPack] Could not validate EcoItem '" + itemId + "'");
                            return false;
                        }
                    }

                    return false;
                } catch (ClassNotFoundException e) {
                    Bukkit.getLogger().warning("[VaultPack] EcoItems plugin not found! Make sure EcoItems is installed.");
                    return false;
                } catch (NoSuchMethodException e) {
                    Bukkit.getLogger().warning("[VaultPack] EcoItems API method not found. Your EcoItems version may be incompatible.");
                    Bukkit.getLogger().warning("[VaultPack] Error: " + e.getMessage());
                    return false;
                } catch (Exception e) {
                    // EcoItems not available or error - fail validation
                    Bukkit.getLogger().warning("[VaultPack] Failed to validate EcoItem '" + parts[1] + "': " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            } else if (pluginName.equals("vaultpack")) {
                // VaultPack backpack validation - check NBT data
                String requiredBackpackId = parts[1]; // e.g., "small", "medium", "large"

                if (!item.hasItemMeta()) {
                    Bukkit.getLogger().warning("[VaultPack] Backpack item has no metadata!");
                    return false;
                }

                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(
                    org.bukkit.Bukkit.getPluginManager().getPlugin("VaultPack"),
                    "backpack_type"
                );

                if (!container.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                    Bukkit.getLogger().warning("[VaultPack] Backpack item missing NBT data!");
                    return false;
                }

                String actualBackpackId = container.get(key, org.bukkit.persistence.PersistentDataType.STRING);

                if (!requiredBackpackId.equals(actualBackpackId)) {
                    return false;
                }

                return true;
            }

            // Unknown plugin prefix
            Bukkit.getLogger().warning("[VaultPack] Unknown custom item prefix: " + pluginName);
            return false;
        }
    }
}
