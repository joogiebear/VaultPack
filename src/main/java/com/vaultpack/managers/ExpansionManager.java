package com.vaultpack.managers;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.api.expansion.VaultPackExpansion;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages VaultPack expansions.
 * Allows registration and lifecycle management of expansion modules.
 *
 * @since 3.0.0
 */
public class ExpansionManager {

    private final VaultPackPlugin plugin;

    @Getter
    private final Map<String, VaultPackExpansion> expansions = new HashMap<>();

    public ExpansionManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register an expansion.
     *
     * @param expansion The expansion to register
     * @return true if registered successfully, false if already registered
     */
    public boolean registerExpansion(VaultPackExpansion expansion) {
        if (expansions.containsKey(expansion.getId())) {
            plugin.getLogger().warning("Expansion " + expansion.getId() + " is already registered!");
            return false;
        }

        expansions.put(expansion.getId(), expansion);
        expansion.onEnable(plugin);
        expansion.setEnabled(true);

        plugin.getLogger().info("Registered expansion: " + expansion.getName() + " v" + expansion.getVersion() + " by " + expansion.getAuthor());
        return true;
    }

    /**
     * Unregister an expansion.
     *
     * @param id The expansion ID
     * @return true if unregistered successfully
     */
    public boolean unregisterExpansion(String id) {
        VaultPackExpansion expansion = expansions.remove(id);

        if (expansion != null) {
            expansion.onDisable();
            expansion.setEnabled(false);
            plugin.getLogger().info("Unregistered expansion: " + expansion.getName());
            return true;
        }

        return false;
    }

    /**
     * Get an expansion by ID.
     *
     * @param id The expansion ID
     * @return Optional containing the expansion if found
     */
    public Optional<VaultPackExpansion> getExpansion(String id) {
        return Optional.ofNullable(expansions.get(id));
    }

    /**
     * Check if an expansion is registered.
     *
     * @param id The expansion ID
     * @return true if registered
     */
    public boolean isExpansionRegistered(String id) {
        return expansions.containsKey(id);
    }

    /**
     * Disable all expansions.
     * Called during plugin shutdown.
     */
    public void disableAll() {
        plugin.getLogger().info("Disabling " + expansions.size() + " expansion(s)...");

        for (VaultPackExpansion expansion : expansions.values()) {
            expansion.onDisable();
            expansion.setEnabled(false);
        }

        expansions.clear();
    }

    /**
     * Reload all expansions.
     * Disables and re-enables all registered expansions.
     */
    public void reloadAll() {
        plugin.getLogger().info("Reloading all expansions...");

        for (VaultPackExpansion expansion : expansions.values()) {
            expansion.onDisable();
            expansion.onEnable(plugin);
        }
    }
}
