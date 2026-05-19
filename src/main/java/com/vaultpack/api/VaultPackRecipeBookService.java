package com.vaultpack.api;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Stable runtime service for recipe-browser plugins such as RecipeBook.
 *
 * <p>Consumers should discover this through Bukkit's ServicesManager instead of
 * compiling against the full VaultPack plugin jar. The interface intentionally
 * exposes only recipe/display operations needed by external recipe browsers.</p>
 */
public interface VaultPackRecipeBookService {

    /**
     * Check whether VaultPack's backpack type system is ready for lookups.
     *
     * @return true when backpack types can be queried
     */
    boolean isAvailable();

    /**
     * Get all configured backpack type ids.
     *
     * @return immutable set of backpack type ids
     */
    Set<String> getBackpackTypeIds();

    /**
     * Check whether a backpack type exists.
     *
     * @param id backpack type id
     * @return true if the type exists
     */
    boolean hasBackpackType(String id);

    /**
     * Check whether a backpack type has a configured recipe.
     *
     * @param id backpack type id
     * @return true if a recipe exists
     */
    boolean hasRecipe(String id);

    /**
     * Get a backpack recipe as nine ingredient strings from backpacks.yml.
     *
     * @param id backpack type id
     * @return immutable list of recipe ingredient strings, or an empty list
     */
    List<String> getRecipe(String id);

    /**
     * Create the display/result item for a backpack type.
     *
     * @param id backpack type id
     * @return cloned backpack item, or null if the type is missing
     */
    ItemStack createBackpackItem(String id);

    /**
     * Check whether an ItemStack is a VaultPack backpack of the requested type.
     *
     * @param item item to check
     * @param id backpack type id
     * @return true if the item is a matching VaultPack backpack
     */
    boolean isBackpackItem(ItemStack item, String id);
}
