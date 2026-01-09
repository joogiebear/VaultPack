package com.vaultpack.messages;

import com.vaultpack.VaultPackPlugin;
import com.vaultpack.config.LangConfig;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Modern message manager using Adventure API and MiniMessage.
 * Handles all plugin messages with support for gradients, hover text, click events, etc.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>MiniMessage formatting (gradients, colors, formatting)</li>
 *   <li>Placeholder support</li>
 *   <li>Prefix handling</li>
 *   <li>Multi-line messages</li>
 *   <li>Component-based architecture</li>
 * </ul>
 */
public class MessageManager {

    private final VaultPackPlugin plugin;

    @Getter
    private LangConfig langConfig;

    @Getter
    private Component prefix;

    public MessageManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    /**
     * Load messages from lang.yml.
     */
    public void loadMessages() {
        File langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }

        langConfig = new LangConfig(langFile);
        langConfig.load();

        // Load prefix
        String prefixRaw = langConfig.getMessage("prefix");
        prefix = Message.of(prefixRaw).asComponent();

        plugin.getLogger().info("Messages loaded from lang.yml");
    }

    /**
     * Reload messages from disk.
     */
    public void reload() {
        if (langConfig != null) {
            langConfig.reload();
        }

        // Reload prefix
        String prefixRaw = langConfig.getMessage("prefix");
        prefix = Message.of(prefixRaw).asComponent();
    }

    /**
     * Get a message by key.
     *
     * @param key The message key
     * @return The Message object
     */
    public Message get(String key) {
        String rawMessage = langConfig.getMessage(key);
        return Message.of(rawMessage);
    }

    /**
     * Get a message with placeholders.
     *
     * @param key   The message key
     * @param pairs Placeholder pairs (key1, value1, key2, value2, ...)
     * @return The Message object with placeholders
     */
    public Message get(String key, String... pairs) {
        return get(key).placeholders(pairs);
    }

    /**
     * Get a prefixed message.
     *
     * @param key The message key
     * @return The Message object with prefix
     */
    public Message getPrefixed(String key) {
        Component messageComponent = get(key).asComponent();
        Component combined = prefix.append(Component.space()).append(messageComponent);
        return Message.of(combined.toString()); // TODO: Better way to combine components
    }

    /**
     * Get a prefixed message with placeholders.
     *
     * @param key   The message key
     * @param pairs Placeholder pairs
     * @return The Message object with prefix and placeholders
     */
    public Message getPrefixed(String key, String... pairs) {
        Component messageComponent = get(key, pairs).asComponent();
        Component combined = prefix.append(Component.space()).append(messageComponent);
        return Message.of(combined.toString()); // TODO: Better way to combine components
    }

    /**
     * Send a message to a command sender.
     *
     * @param sender The command sender
     * @param key    The message key
     * @param pairs  Placeholder pairs
     */
    public void send(CommandSender sender, String key, String... pairs) {
        get(key, pairs).send(sender);
    }

    /**
     * Send a prefixed message to a command sender.
     *
     * @param sender The command sender
     * @param key    The message key
     * @param pairs  Placeholder pairs
     */
    public void sendPrefixed(CommandSender sender, String key, String... pairs) {
        getPrefixed(key, pairs).send(sender);
    }

    /**
     * Get a message as a plain string (for legacy compatibility).
     *
     * @param key   The message key
     * @param pairs Placeholder pairs
     * @return The message as a plain string
     */
    public String getMessage(String key, String... pairs) {
        String message = langConfig.getMessage(key);

        // Apply placeholders
        for (int i = 0; i < pairs.length; i += 2) {
            if (i + 1 < pairs.length) {
                message = message.replace(pairs[i], pairs[i + 1]);
            }
        }

        // Convert & codes to § for legacy compatibility
        return message.replace("&", "§");
    }

    /**
     * Get a message list by key.
     *
     * @param key The message key
     * @return List of message strings
     */
    public List<String> getMessageList(String key) {
        return langConfig.getMessageList(key).stream()
            .map(msg -> msg.replace("&", "§"))
            .collect(Collectors.toList());
    }

    /**
     * Get a message list with placeholders.
     *
     * @param key   The message key
     * @param pairs Placeholder pairs
     * @return List of message strings with placeholders replaced
     */
    public List<String> getMessageList(String key, String... pairs) {
        return langConfig.getMessageList(key).stream()
            .map(msg -> {
                String result = msg;
                for (int i = 0; i < pairs.length; i += 2) {
                    if (i + 1 < pairs.length) {
                        result = result.replace(pairs[i], pairs[i + 1]);
                    }
                }
                return result.replace("&", "§");
            })
            .collect(Collectors.toList());
    }

    /**
     * Check if a message exists.
     *
     * @param key The message key
     * @return true if the message exists
     */
    public boolean hasMessage(String key) {
        return langConfig.hasMessage(key);
    }

    /**
     * Get the configured prefix component.
     *
     * @return The prefix component
     */
    public Component getPrefix() {
        return prefix;
    }
}
