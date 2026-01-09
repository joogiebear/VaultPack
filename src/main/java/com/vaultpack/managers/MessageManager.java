package com.vaultpack.managers;

import com.vaultpack.VaultPackPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Phase 3: Enhanced message manager with Adventure API support
 * Maintains backward compatibility with legacy ChatColor
 * Supports both String-based and Component-based messaging
 */
public class MessageManager {

    private final VaultPackPlugin plugin;
    private final File langFile;
    private FileConfiguration langConfig;
    private final Map<String, String> messageCache;
    private final Map<String, Component> componentCache;
    private String prefix;

    // Adventure support
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;

    public MessageManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.langFile = new File(plugin.getDataFolder(), "lang.yml");
        this.messageCache = new HashMap<>();
        this.componentCache = new HashMap<>();

        // Phase 3: Initialize Adventure API support
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();

        // Create lang.yml if it doesn't exist
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }

        loadMessages();
    }

    /**
     * Load all messages from lang.yml
     */
    public void loadMessages() {
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        messageCache.clear();

        // Load prefix
        prefix = colorize(langConfig.getString("prefix", "&8[&6Backpack&8]&r "));

        // Cache all messages
        cacheMessages("", langConfig);
    }

    /**
     * Recursively cache messages from config
     */
    private void cacheMessages(String path, org.bukkit.configuration.ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (section.isConfigurationSection(fullPath)) {
                cacheMessages(fullPath, section.getConfigurationSection(fullPath));
            } else if (section.isString(fullPath)) {
                String message = section.getString(fullPath);
                messageCache.put(fullPath, colorize(message));
            }
        }
    }

    /**
     * Get a message by key with optional placeholder replacements
     *
     * @param key Message key from lang.yml
     * @param replacements Pairs of placeholder and value (e.g., "%player%", playerName)
     * @return Formatted message with colors and placeholders replaced
     */
    public String getMessage(String key, String... replacements) {
        String message = messageCache.getOrDefault(key, key);

        // Replace placeholders
        if (replacements.length > 0) {
            if (replacements.length % 2 != 0) {
                plugin.getLogger().warning("Odd number of replacements for message: " + key);
            } else {
                for (int i = 0; i < replacements.length; i += 2) {
                    String placeholder = replacements[i];
                    String value = replacements[i + 1];
                    message = message.replace(placeholder, value);
                }
            }
        }

        return message;
    }

    /**
     * Get a message with prefix
     */
    public String getMessageWithPrefix(String key, String... replacements) {
        return prefix + getMessage(key, replacements);
    }

    /**
     * Send a message to a player/console
     *
     * @param sender CommandSender to send message to
     * @param key Message key from lang.yml
     * @param replacements Placeholder replacements
     */
    public void send(CommandSender sender, String key, String... replacements) {
        String message = getMessage(key, replacements);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    /**
     * Send a message with prefix
     */
    public void sendWithPrefix(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(getMessageWithPrefix(key, replacements));
    }

    /**
     * Get multiple messages (for help text, lore, etc.)
     */
    public List<String> getMessageList(String key) {
        List<String> messages = langConfig.getStringList(key);
        messages.replaceAll(this::colorize);
        return messages;
    }

    /**
     * Get prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Reload messages from lang.yml
     */
    public void reload() {
        loadMessages();
        componentCache.clear(); // Clear component cache too
    }

    /**
     * Translate color codes using & symbol
     */
    private String colorize(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // ==================== Phase 3: Adventure API Methods ====================

    /**
     * Get a message as an Adventure Component
     * Supports MiniMessage formatting
     *
     * @param key Message key from lang.yml
     * @param replacements Placeholder replacements
     * @return Adventure Component
     */
    public Component getComponent(String key, String... replacements) {
        // Check cache first
        String cacheKey = key + ":" + Arrays.toString(replacements);
        Component cached = componentCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Get raw message
        String rawMessage = langConfig.getString(key);
        if (rawMessage == null || rawMessage.isEmpty()) {
            plugin.getLogger().warning("Missing message: " + key);
            return Component.text("Missing: " + key);
        }

        // Apply placeholder replacements
        if (replacements.length > 0) {
            if (replacements.length % 2 != 0) {
                plugin.getLogger().warning("Odd number of replacements for message: " + key);
            } else {
                for (int i = 0; i < replacements.length; i += 2) {
                    rawMessage = rawMessage.replace(replacements[i], replacements[i + 1]);
                }
            }
        }

        // Parse as Component
        Component component;
        try {
            // Try MiniMessage first (supports <color>, <gradient>, etc.)
            component = miniMessage.deserialize(rawMessage);
        } catch (Exception e) {
            // Fallback to legacy format (&c, &l, etc.)
            component = legacySerializer.deserialize(rawMessage);
        }

        // Cache it
        componentCache.put(cacheKey, component);

        return component;
    }

    /**
     * Get a list of messages as Components
     */
    public List<Component> getComponentList(String key) {
        List<String> messages = langConfig.getStringList(key);
        List<Component> components = new ArrayList<>();

        for (String message : messages) {
            try {
                components.add(miniMessage.deserialize(message));
            } catch (Exception e) {
                components.add(legacySerializer.deserialize(message));
            }
        }

        return components;
    }

    /**
     * Send a Component message to a sender
     */
    public void sendComponent(CommandSender sender, String key, String... replacements) {
        Component message = getComponent(key, replacements);
        sender.sendMessage(message);
    }

    /**
     * Send a Component list to a sender
     */
    public void sendComponentList(CommandSender sender, String key) {
        List<Component> messages = getComponentList(key);
        for (Component message : messages) {
            sender.sendMessage(message);
        }
    }

    /**
     * Parse a MiniMessage string directly
     *
     * Example: parseMiniMessage("<gradient:red:blue>Rainbow!</gradient>")
     */
    public Component parseMiniMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        try {
            return miniMessage.deserialize(message);
        } catch (Exception e) {
            return legacySerializer.deserialize(message);
        }
    }

    /**
     * Create a gradient Component
     */
    public Component gradient(String text, String startColor, String endColor) {
        String formatted = String.format("<gradient:%s:%s>%s</gradient>", startColor, endColor, text);
        return miniMessage.deserialize(formatted);
    }

    /**
     * Create a clickable Component
     */
    public Component clickable(String text, String action, String value) {
        String formatted = String.format("<click:%s:'%s'>%s</click>", action, value, text);
        return miniMessage.deserialize(formatted);
    }

    /**
     * Create a hoverable Component
     */
    public Component hoverable(String text, String hoverText) {
        String formatted = String.format("<hover:show_text:'%s'>%s</hover>", hoverText, text);
        return miniMessage.deserialize(formatted);
    }

    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        return String.format("Message Cache: %d strings, %d components",
            messageCache.size(),
            componentCache.size());
    }
}
