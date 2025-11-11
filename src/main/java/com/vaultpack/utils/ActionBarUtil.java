package com.vaultpack.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Utility for sending action bar messages to players
 * Uses Paper's Adventure API for modern text components
 */
public class ActionBarUtil {

    /**
     * Send an action bar message to a player
     * @param player The player to send to
     * @param message The message (supports & color codes)
     */
    public static void send(Player player, String message) {
        if (player == null || message == null) return;

        // Translate color codes
        String colored = ChatColor.translateAlternateColorCodes('&', message);

        // Convert to Adventure Component and send
        Component component = LegacyComponentSerializer.legacySection().deserialize(colored);
        player.sendActionBar(component);
    }

    /**
     * Send a success message (green) to action bar
     */
    public static void sendSuccess(Player player, String message) {
        send(player, "&a" + message);
    }

    /**
     * Send an error message (red) to action bar
     */
    public static void sendError(Player player, String message) {
        send(player, "&c" + message);
    }

    /**
     * Send a warning message (yellow) to action bar
     */
    public static void sendWarning(Player player, String message) {
        send(player, "&e" + message);
    }

    /**
     * Send an info message (gray) to action bar
     */
    public static void sendInfo(Player player, String message) {
        send(player, "&7" + message);
    }

    /**
     * Send a highlighted message (gold) to action bar
     */
    public static void sendHighlight(Player player, String message) {
        send(player, "&6" + message);
    }
}
