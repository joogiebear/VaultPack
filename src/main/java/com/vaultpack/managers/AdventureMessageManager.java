package com.vaultpack.managers;

import com.vaultpack.VaultPackPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 3: Modern message manager using Adventure API
 * Replaces legacy ChatColor with Components and MiniMessage
 *
 * Features:
 * - MiniMessage formatting (gradients, hover, click events)
 * - Component caching for performance
 * - Placeholder support
 * - Backward compatible with legacy color codes
 */
public class AdventureMessageManager {

    private final VaultPackPlugin plugin;
    private final MiniMessage miniMessage;
    private final File langFile;
    private FileConfiguration langConfig;

    // Cache parsed components for performance
    private final Map<String, Component> componentCache;
    private final Map<String, List<Component>> listComponentCache;

    // Legacy support flag
    private boolean legacyColorCodes = true;

    public AdventureMessageManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.langFile = new File(plugin.getDataFolder(), "lang.yml");
        this.componentCache = new ConcurrentHashMap<>();
        this.listComponentCache = new ConcurrentHashMap<>();

        loadLangFile();
    }

    /**
     * Load language file
     */
    private void loadLangFile() {
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Clear caches when reloading
        componentCache.clear();
        listComponentCache.clear();

        plugin.getLogger().info("Loaded " + langConfig.getKeys(true).size() + " messages");
    }

    /**
     * Reload language file
     */
    public void reload() {
        loadLangFile();
    }

    /**
     * Send a message to a player
     *
     * @param sender The recipient
     * @param path Message path in lang.yml
     * @param placeholders Placeholder pairs (key1, value1, key2, value2, ...)
     */
    public void send(CommandSender sender, String path, String... placeholders) {
        Component message = getMessage(path, placeholders);
        if (message != null && !Component.empty().equals(message)) {
            sender.sendMessage(message);
        }
    }

    /**
     * Send multiple messages to a player
     */
    public void sendList(CommandSender sender, String path, String... placeholders) {
        List<Component> messages = getMessageList(path, placeholders);
        for (Component message : messages) {
            sender.sendMessage(message);
        }
    }

    /**
     * Get a formatted message component
     *
     * @param path Message path in lang.yml
     * @param placeholders Placeholder pairs
     * @return Formatted component
     */
    public Component getMessage(String path, String... placeholders) {
        String rawMessage = langConfig.getString(path);

        if (rawMessage == null || rawMessage.isEmpty()) {
            plugin.getLogger().warning("Missing message: " + path);
            return Component.text("Missing message: " + path);
        }

        // Apply placeholders to raw string first
        String processedMessage = applyPlaceholders(rawMessage, placeholders);

        // Check cache (with placeholders applied)
        String cacheKey = path + ":" + Arrays.toString(placeholders);
        Component cached = componentCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Parse message
        Component component = parseMessage(processedMessage);

        // Cache it
        componentCache.put(cacheKey, component);

        return component;
    }

    /**
     * Get a list of formatted message components
     */
    public List<Component> getMessageList(String path, String... placeholders) {
        List<String> rawMessages = langConfig.getStringList(path);

        if (rawMessages.isEmpty()) {
            // Try as single string
            String single = langConfig.getString(path);
            if (single != null && !single.isEmpty()) {
                return Collections.singletonList(getMessage(path, placeholders));
            }
            return Collections.emptyList();
        }

        // Check cache
        String cacheKey = path + ":" + Arrays.toString(placeholders);
        List<Component> cached = listComponentCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Parse all messages
        List<Component> components = new ArrayList<>();
        for (String rawMessage : rawMessages) {
            String processedMessage = applyPlaceholders(rawMessage, placeholders);
            components.add(parseMessage(processedMessage));
        }

        // Cache it
        listComponentCache.put(cacheKey, components);

        return components;
    }

    /**
     * Parse a message string into a Component
     * Supports both MiniMessage and legacy color codes
     */
    private Component parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        // If legacy color codes are enabled, convert them first
        if (legacyColorCodes) {
            message = convertLegacyColors(message);
        }

        // Parse with MiniMessage
        try {
            return miniMessage.deserialize(message);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse message: " + message);
            plugin.getLogger().warning("Error: " + e.getMessage());
            return Component.text(message); // Fallback to plain text
        }
    }

    /**
     * Convert legacy color codes (&c) to MiniMessage format (<red>)
     */
    private String convertLegacyColors(String message) {
        if (message == null) {
            return "";
        }

        return message
            // Colors
            .replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>")
            .replace("&b", "<aqua>")
            .replace("&c", "<red>")
            .replace("&d", "<light_purple>")
            .replace("&e", "<yellow>")
            .replace("&f", "<white>")
            // Formatting
            .replace("&k", "<obfuscated>")
            .replace("&l", "<bold>")
            .replace("&m", "<strikethrough>")
            .replace("&n", "<underlined>")
            .replace("&o", "<italic>")
            .replace("&r", "<reset>");
    }

    /**
     * Apply placeholder replacements to a string
     */
    private String applyPlaceholders(String message, String... placeholders) {
        if (placeholders.length == 0) {
            return message;
        }

        String result = message;
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            result = result.replace(placeholders[i], placeholders[i + 1]);
        }

        return result;
    }

    /**
     * Get raw message string (for compatibility)
     */
    public String getRawMessage(String path) {
        return langConfig.getString(path, "Missing: " + path);
    }

    /**
     * Get raw message list (for compatibility)
     */
    public List<String> getRawMessageList(String path) {
        List<String> list = langConfig.getStringList(path);
        if (list.isEmpty()) {
            String single = langConfig.getString(path);
            if (single != null) {
                return Collections.singletonList(single);
            }
        }
        return list;
    }

    /**
     * Clear message cache
     */
    public void clearCache() {
        componentCache.clear();
        listComponentCache.clear();
    }

    /**
     * Get cache statistics
     */
    public String getCacheStats() {
        return String.format("Message Cache: %d components, %d lists",
            componentCache.size(),
            listComponentCache.size());
    }

    /**
     * Enable/disable legacy color code support
     */
    public void setLegacyColorCodes(boolean enabled) {
        this.legacyColorCodes = enabled;
        clearCache(); // Clear cache when changing mode
    }

    /**
     * Parse a MiniMessage string directly (for custom formatting)
     *
     * Example usage:
     * <pre>
     * Component msg = messageManager.parseMiniMessage(
     *     "<gradient:red:blue>Rainbow Text</gradient>",
     *     Placeholder.parsed("player", playerName)
     * );
     * </pre>
     */
    public Component parseMiniMessage(String message, TagResolver... resolvers) {
        if (legacyColorCodes) {
            message = convertLegacyColors(message);
        }

        if (resolvers.length > 0) {
            return miniMessage.deserialize(message, resolvers);
        } else {
            return miniMessage.deserialize(message);
        }
    }

    /**
     * Create a gradient message
     *
     * @param text The text to apply gradient to
     * @param startColor Start color (e.g., "red", "#FF0000")
     * @param endColor End color
     * @return Component with gradient
     */
    public Component gradient(String text, String startColor, String endColor) {
        String formatted = String.format("<gradient:%s:%s>%s</gradient>", startColor, endColor, text);
        return miniMessage.deserialize(formatted);
    }

    /**
     * Create a clickable message
     *
     * @param text The text to display
     * @param action Click action (e.g., "run_command", "open_url")
     * @param value The command or URL
     * @return Clickable component
     */
    public Component clickable(String text, String action, String value) {
        String formatted = String.format("<click:%s:'%s'>%s</click>", action, value, text);
        return miniMessage.deserialize(formatted);
    }

    /**
     * Create a hoverable message
     *
     * @param text The text to display
     * @param hoverText Text shown on hover
     * @return Hoverable component
     */
    public Component hoverable(String text, String hoverText) {
        String formatted = String.format("<hover:show_text:'%s'>%s</hover>", hoverText, text);
        return miniMessage.deserialize(formatted);
    }
}
