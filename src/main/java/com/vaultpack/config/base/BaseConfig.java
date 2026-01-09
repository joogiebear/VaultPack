package com.vaultpack.config.base;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Base configuration class with automatic field serialization and migration support.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Automatic camelCase to kebab-case field name conversion</li>
 *   <li>Reflection-based field loading/saving</li>
 *   <li>Version-based migration system</li>
 *   <li>Type-safe configuration values</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class MyConfig extends BaseConfig {
 *     private String serverName = "Default Server";  // → server-name in YAML
 *     private Integer maxPlayers = 100;              // → max-players in YAML
 *
 *     @IgnoreField
 *     private transient Object cache;  // Not saved to YAML
 *
 *     public MyConfig(File file) {
 *         super(file);
 *     }
 *
 *     @Override
 *     protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
 *         return List.of(
 *             (yaml) -> {
 *                 yaml.set("config-version", 1);
 *             }
 *         );
 *     }
 * }
 * }</pre>
 */
public abstract class BaseConfig {

    @IgnoreField
    @Getter
    private final File file;

    @IgnoreField
    private YamlConfiguration yaml;

    @IgnoreField
    private static final Logger LOGGER = Logger.getLogger("VaultPack");

    /**
     * Creates a new config instance.
     *
     * @param file The config file
     */
    public BaseConfig(File file) {
        this.file = file;
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Load config values from YAML into this object.
     * This will run migrations first, then load all fields.
     */
    public void load() {
        migrate();
        loadFields();
    }

    /**
     * Save config values from this object to YAML.
     */
    public void save() {
        saveFields();
        try {
            yaml.save(file);
        } catch (IOException e) {
            LOGGER.severe("Failed to save config file: " + file.getName() + " - " + e.getMessage());
        }
    }

    /**
     * Reload config from disk and re-load all fields.
     */
    public void reload() {
        this.yaml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    /**
     * Get the underlying YAML configuration.
     * Useful for advanced manipulation.
     *
     * @return The YamlConfiguration
     */
    protected YamlConfiguration getYaml() {
        return yaml;
    }

    /**
     * Override this method to provide migration steps.
     * Each consumer receives the YAML configuration and can modify it.
     * Migrations run in order based on the current config-version.
     *
     * @return List of migration steps
     */
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        return Collections.emptyList();
    }

    /**
     * Execute migrations if needed.
     * Checks config-version and runs any missing migration steps.
     */
    private void migrate() {
        int currentVersion = yaml.getInt("config-version", 0);
        List<Consumer<YamlConfiguration>> migrations = getMigrationSteps();

        if (currentVersion < migrations.size()) {
            LOGGER.info("Migrating " + file.getName() + " from version " + currentVersion + " to " + migrations.size());

            for (int i = currentVersion; i < migrations.size(); i++) {
                try {
                    migrations.get(i).accept(yaml);
                    LOGGER.info("Applied migration step " + (i + 1) + " to " + file.getName());
                } catch (Exception e) {
                    LOGGER.severe("Failed to apply migration step " + (i + 1) + " to " + file.getName() + " - " + e.getMessage());
                }
            }

            try {
                yaml.save(file);
                LOGGER.info("Migration complete for " + file.getName());
            } catch (IOException e) {
                LOGGER.severe("Failed to save migrated config: " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * Load all non-ignored fields from YAML using reflection.
     */
    private void loadFields() {
        for (Field field : getAllFields(getClass())) {
            if (shouldIgnoreField(field)) continue;

            field.setAccessible(true);
            String key = serializeKey(field.getName());

            try {
                Object value = yaml.get(key);
                if (value != null) {
                    // Handle type conversion if needed
                    if (field.getType().equals(Integer.class) && value instanceof Number) {
                        field.set(this, ((Number) value).intValue());
                    } else if (field.getType().equals(Long.class) && value instanceof Number) {
                        field.set(this, ((Number) value).longValue());
                    } else if (field.getType().equals(Double.class) && value instanceof Number) {
                        field.set(this, ((Number) value).doubleValue());
                    } else if (field.getType().equals(Float.class) && value instanceof Number) {
                        field.set(this, ((Number) value).floatValue());
                    } else if (field.getType().equals(Boolean.class) && value instanceof Boolean) {
                        field.set(this, value);
                    } else {
                        field.set(this, value);
                    }
                }
            } catch (IllegalAccessException e) {
                LOGGER.warning("Failed to load field '" + field.getName() + "' from " + file.getName() + " - " + e.getMessage());
            } catch (ClassCastException e) {
                LOGGER.warning("Type mismatch for field '" + field.getName() + "' in " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * Save all non-ignored fields to YAML using reflection.
     */
    private void saveFields() {
        for (Field field : getAllFields(getClass())) {
            if (shouldIgnoreField(field)) continue;

            field.setAccessible(true);
            String key = serializeKey(field.getName());

            try {
                Object value = field.get(this);
                yaml.set(key, value);
            } catch (IllegalAccessException e) {
                LOGGER.warning("Failed to save field '" + field.getName() + "' to " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * Get all fields from this class and its superclasses (except BaseConfig).
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != BaseConfig.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Check if a field should be ignored.
     */
    private boolean shouldIgnoreField(Field field) {
        return field.isAnnotationPresent(IgnoreField.class) ||
               java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
               java.lang.reflect.Modifier.isTransient(field.getModifiers());
    }

    /**
     * Convert camelCase field name to kebab-case YAML key.
     *
     * Examples:
     * - serverName → server-name
     * - maxPlayers → max-players
     * - enableFeatureX → enable-feature-x
     *
     * @param fieldName The field name in camelCase
     * @return The YAML key in kebab-case
     */
    private String serializeKey(String fieldName) {
        return fieldName.replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase(Locale.ROOT);
    }
}
