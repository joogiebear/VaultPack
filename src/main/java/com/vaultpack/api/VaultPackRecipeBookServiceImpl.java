package com.vaultpack.api;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.types.BackpackType;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of the stable RecipeBook runtime service.
 */
public class VaultPackRecipeBookServiceImpl implements VaultPackRecipeBookService {

    private final VaultPackPlugin plugin;

    public VaultPackRecipeBookServiceImpl(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        return plugin.getBackpackTypeManager() != null;
    }

    @Override
    public Set<String> getBackpackTypeIds() {
        if (!isAvailable()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(plugin.getBackpackTypeManager().getAllBackpackTypes().keySet()));
    }

    @Override
    public boolean hasBackpackType(String id) {
        return id != null && isAvailable() && plugin.getBackpackTypeManager().hasBackpackType(id);
    }

    @Override
    public boolean hasRecipe(String id) {
        BackpackType type = getType(id);
        return type != null && type.hasRecipe();
    }

    @Override
    public List<String> getRecipe(String id) {
        BackpackType type = getType(id);
        if (type == null || !type.hasRecipe()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(type.getRecipe());
    }

    @Override
    public ItemStack createBackpackItem(String id) {
        BackpackType type = getType(id);
        if (type == null) {
            return null;
        }
        return plugin.getBackpackTypeManager().createBackpackItem(type).clone();
    }

    @Override
    public boolean isBackpackItem(ItemStack item, String id) {
        if (item == null || id == null || item.getItemMeta() == null) {
            return false;
        }

        String actualType = item.getItemMeta().getPersistentDataContainer().get(
            new NamespacedKey(plugin, "backpack_type"),
            PersistentDataType.STRING
        );
        return id.equals(actualType);
    }

    private BackpackType getType(String id) {
        if (id == null || !isAvailable()) {
            return null;
        }
        return plugin.getBackpackTypeManager().getBackpackType(id);
    }
}
