package com.vaultpack.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a message that can be sent to players using Adventure API.
 * Supports MiniMessage formatting with placeholders.
 *
 * <p>Examples:</p>
 * <pre>
 * // Simple message
 * Message.of("&lt;green&gt;Welcome!").send(player);
 *
 * // With placeholders
 * Message.of("&lt;gold&gt;Hello, &lt;player&gt;!")
 *     .placeholder("player", player.getName())
 *     .send(player);
 *
 * // Gradient
 * Message.of("&lt;gradient:red:blue&gt;VaultPack&lt;/gradient&gt;").send(player);
 * </pre>
 */
public class Message {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    private final String rawMessage;
    private final List<TagResolver> placeholders;

    private Message(String rawMessage) {
        this.rawMessage = rawMessage;
        this.placeholders = new ArrayList<>();
    }

    /**
     * Create a new message from a raw string.
     *
     * @param rawMessage The raw message with MiniMessage tags
     * @return New Message instance
     */
    public static Message of(String rawMessage) {
        return new Message(rawMessage);
    }

    /**
     * Add a placeholder to this message.
     *
     * @param key   The placeholder key (e.g., "player")
     * @param value The placeholder value
     * @return This message for chaining
     */
    public Message placeholder(String key, String value) {
        placeholders.add(Placeholder.parsed(key, value));
        return this;
    }

    /**
     * Add a component placeholder to this message.
     *
     * @param key   The placeholder key
     * @param value The component value
     * @return This message for chaining
     */
    public Message placeholder(String key, Component value) {
        placeholders.add(Placeholder.component(key, value));
        return this;
    }

    /**
     * Add multiple placeholders at once.
     *
     * @param pairs Key-value pairs (key1, value1, key2, value2, ...)
     * @return This message for chaining
     */
    public Message placeholders(String... pairs) {
        for (int i = 0; i < pairs.length; i += 2) {
            if (i + 1 < pairs.length) {
                placeholder(pairs[i], pairs[i + 1]);
            }
        }
        return this;
    }

    /**
     * Convert this message to an Adventure Component.
     *
     * @return The parsed Component
     */
    public Component asComponent() {
        // Convert legacy & codes to MiniMessage format
        String converted = convertLegacyToMiniMessage(rawMessage);

        if (placeholders.isEmpty()) {
            return MINI_MESSAGE.deserialize(converted);
        } else {
            TagResolver resolver = TagResolver.resolver(placeholders);
            return MINI_MESSAGE.deserialize(converted, resolver);
        }
    }

    /**
     * Convert legacy ampersand color codes to MiniMessage format.
     *
     * @param message The message with & codes
     * @return The message with MiniMessage tags
     */
    private String convertLegacyToMiniMessage(String message) {
        if (message == null) return "";

        return message
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
            .replace("&r", "<reset>")
            .replace("&l", "<bold>")
            .replace("&o", "<italic>")
            .replace("&n", "<underlined>")
            .replace("&m", "<strikethrough>")
            .replace("&k", "<obfuscated>");
    }

    /**
     * Send this message to a command sender.
     *
     * @param sender The command sender to send to
     */
    public void send(CommandSender sender) {
        sender.sendMessage(asComponent());
    }

    /**
     * Send this message to multiple command senders.
     *
     * @param senders The command senders to send to
     */
    public void send(CommandSender... senders) {
        Component component = asComponent();
        for (CommandSender sender : senders) {
            sender.sendMessage(component);
        }
    }

    /**
     * Get the raw message string.
     *
     * @return The raw message
     */
    public String getRaw() {
        return rawMessage;
    }

    /**
     * Check if this message is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return rawMessage == null || rawMessage.isEmpty();
    }

    @Override
    public String toString() {
        return rawMessage;
    }
}
