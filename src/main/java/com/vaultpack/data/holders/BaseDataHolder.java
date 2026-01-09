package com.vaultpack.data.holders;

import com.vaultpack.data.components.DataComponent;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * Base data holder that manages a collection of DataComponents.
 * Provides common functionality for all data holders.
 *
 * <p>Architecture:</p>
 * <ul>
 *   <li>Composable: Built from modular components</li>
 *   <li>Extensible: New components can be added dynamically</li>
 *   <li>Type-safe: Components retrieved by type</li>
 *   <li>Efficient: Only dirty components are saved</li>
 * </ul>
 */
public abstract class BaseDataHolder {

    @Getter
    private final UUID identifier;

    private final Map<String, DataComponent> components;

    protected BaseDataHolder(UUID identifier) {
        this.identifier = identifier;
        this.components = new LinkedHashMap<>();
    }

    /**
     * Register a component with this data holder.
     *
     * @param component The component to register
     */
    protected void registerComponent(DataComponent component) {
        components.put(component.getId(), component);
    }

    /**
     * Get a component by ID.
     *
     * @param id  The component ID
     * @param <T> The component type
     * @return The component, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends DataComponent> T getComponent(String id) {
        return (T) components.get(id);
    }

    /**
     * Get a component by class.
     *
     * @param componentClass The component class
     * @param <T>            The component type
     * @return The component, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends DataComponent> T getComponent(Class<T> componentClass) {
        for (DataComponent component : components.values()) {
            if (componentClass.isInstance(component)) {
                return (T) component;
            }
        }
        return null;
    }

    /**
     * Get all registered components.
     *
     * @return Collection of all components
     */
    public Collection<DataComponent> getComponents() {
        return Collections.unmodifiableCollection(components.values());
    }

    /**
     * Check if any component is dirty.
     *
     * @return true if any component has been modified
     */
    public boolean isDirty() {
        return components.values().stream().anyMatch(DataComponent::isDirty);
    }

    /**
     * Mark all components as clean.
     */
    public void markClean() {
        components.values().forEach(DataComponent::markClean);
    }

    /**
     * Load all components from a YAML configuration.
     *
     * @param yaml The YAML configuration
     */
    public void load(YamlConfiguration yaml) {
        for (DataComponent component : components.values()) {
            ConfigurationSection section = yaml.getConfigurationSection(component.getId());
            if (section != null) {
                component.load(section);
            }
        }
        markClean();
    }

    /**
     * Save all dirty components to a YAML configuration.
     *
     * @param yaml The YAML configuration
     */
    public void save(YamlConfiguration yaml) {
        for (DataComponent component : components.values()) {
            if (component.isDirty()) {
                ConfigurationSection section = yaml.createSection(component.getId());
                component.save(section);
                component.markClean();
            }
        }
    }

    /**
     * Save all components (regardless of dirty state) to a YAML configuration.
     *
     * @param yaml The YAML configuration
     */
    public void saveAll(YamlConfiguration yaml) {
        for (DataComponent component : components.values()) {
            ConfigurationSection section = yaml.createSection(component.getId());
            component.save(section);
            component.markClean();
        }
    }

    /**
     * Reset all components to default state.
     */
    public void reset() {
        components.values().forEach(DataComponent::reset);
        markClean();
    }

    /**
     * Validate all components.
     *
     * @return true if all components are valid
     */
    public boolean validate() {
        return components.values().stream().allMatch(DataComponent::validate);
    }
}
