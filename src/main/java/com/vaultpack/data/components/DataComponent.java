package com.vaultpack.data.components;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Represents a modular component of player data.
 * Components can be added to DataHolders for composable data architecture.
 *
 * <p>Examples of components:</p>
 * <ul>
 *   <li>BackpackDataComponent - Manages backpack slots and items</li>
 *   <li>EnderChestDataComponent - Manages ender chest pages</li>
 *   <li>SettingsDataComponent - Manages player preferences</li>
 *   <li>StatisticsDataComponent - Tracks player statistics</li>
 * </ul>
 *
 * <p>Benefits:</p>
 * <ul>
 *   <li>Modular: Add/remove features without changing core data holder</li>
 *   <li>Reusable: Components can be shared across different data holders</li>
 *   <li>Testable: Each component can be tested independently</li>
 *   <li>Extensible: New components can be added without modifying existing code</li>
 * </ul>
 */
public interface DataComponent {

    /**
     * Get the unique identifier for this component.
     * Used for serialization and component lookup.
     *
     * @return Component ID (e.g., "backpacks", "ender-chests", "settings")
     */
    String getId();

    /**
     * Load this component's data from a configuration section.
     *
     * @param section The configuration section to load from
     */
    void load(ConfigurationSection section);

    /**
     * Save this component's data to a configuration section.
     *
     * @param section The configuration section to save to
     */
    void save(ConfigurationSection section);

    /**
     * Reset this component to default state.
     * Useful for player data resets or cleanup.
     */
    void reset();

    /**
     * Check if this component has been modified since last save.
     *
     * @return true if modified
     */
    boolean isDirty();

    /**
     * Mark this component as clean (not modified).
     * Called after successful save.
     */
    void markClean();

    /**
     * Mark this component as dirty (modified).
     * Called when data changes.
     */
    void markDirty();

    /**
     * Validate this component's data.
     *
     * @return true if data is valid
     */
    default boolean validate() {
        return true;
    }
}
