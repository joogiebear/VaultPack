package com.vaultpack.config;

import com.vaultpack.config.base.BaseConfig;
import com.vaultpack.config.base.IgnoreField;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/**
 * Language configuration for VaultPack.
 * Loads all messages from lang.yml with support for single strings and lists.
 */
public class LangConfig extends BaseConfig {

    @IgnoreField
    @Getter
    private final Map<String, Object> messages = new HashMap<>();

    /**
     * Creates a new LangConfig instance.
     *
     * @param file The lang.yml file
     */
    public LangConfig(File file) {
        super(file);
    }

    @Override
    public void load() {
        super.load(); // Handle migrations
        loadMessages();
    }

    @Override
    public void save() {
        saveMessages();
        super.save(); // Save to file
    }

    /**
     * Load all messages from the messages section OR root level.
     * Flexible loading: tries "messages:" section first, falls back to root.
     */
    private void loadMessages() {
        messages.clear();

        YamlConfiguration yaml = getYaml();
        ConfigurationSection messagesSection = yaml.getConfigurationSection("messages");

        if (messagesSection != null) {
            // Load from messages: section
            loadMessagesRecursive(messagesSection, "", messages);
        } else {
            // Fall back to root level (Aurora-style flexibility)
            loadMessagesRecursive(yaml, "", messages);
        }

        getLogger().info("Loaded " + messages.size() + " messages from " + getFile().getName());
    }

    /**
     * Recursively load messages from nested sections.
     */
    private void loadMessagesRecursive(ConfigurationSection section, String prefix, Map<String, Object> map) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (section.isConfigurationSection(key)) {
                // Nested section - recurse
                loadMessagesRecursive(section.getConfigurationSection(key), fullKey, map);
            } else if (section.isList(key)) {
                // List of strings
                map.put(fullKey, section.getStringList(key));
            } else {
                // Single string
                map.put(fullKey, section.getString(key, ""));
            }
        }
    }

    /**
     * Save all messages to the messages section.
     */
    private void saveMessages() {
        YamlConfiguration yaml = getYaml();

        // Clear existing messages section
        yaml.set("messages", null);

        for (Map.Entry<String, Object> entry : messages.entrySet()) {
            yaml.set("messages." + entry.getKey(), entry.getValue());
        }
    }

    /**
     * Get a message string by key.
     *
     * @param key The message key (e.g., "prefix", "no-permission")
     * @return The message string, or the key if not found
     */
    public String getMessage(String key) {
        Object value = messages.get(key);
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) value;
            return String.join("\n", list);
        }
        return key; // Return key if not found (fallback)
    }

    /**
     * Get a message list by key.
     *
     * @param key The message key
     * @return The message list, or empty list if not found
     */
    public List<String> getMessageList(String key) {
        Object value = messages.get(key);
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) value;
            return new ArrayList<>(list);
        } else if (value instanceof String) {
            return Collections.singletonList((String) value);
        }
        return Collections.emptyList();
    }

    /**
     * Check if a message exists.
     *
     * @param key The message key
     * @return true if the message exists
     */
    public boolean hasMessage(String key) {
        return messages.containsKey(key);
    }

    /**
     * Get all message keys.
     *
     * @return Set of all message keys
     */
    public Set<String> getMessageKeys() {
        return messages.keySet();
    }

    @Override
    protected List<Consumer<YamlConfiguration>> getMigrationSteps() {
        return Arrays.asList(
            // Migration 1: Convert legacy &-codes to MiniMessage
            (yaml) -> {
                // This migration would convert old color codes to MiniMessage format
                // For now, we'll keep both formats compatible
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
