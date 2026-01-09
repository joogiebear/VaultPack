package com.example.myplugin;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Manages all plugin messages from configuration
 * 
 * All user-facing messages should go through this class to ensure they are:
 * - Config-driven (not hardcoded)
 * - Support PlaceholderAPI
 * - Use MiniMessage formatting
 */
public class MessageManager {
    
    private final MyPlugin plugin;
    private final MiniMessage miniMessage;
    
    public MessageManager(MyPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    /**
     * Send a message from config to a command sender
     * 
     * @param sender The command sender
     * @param key The config key (e.g., "messages.no-permission")
     * @param placeholders Optional placeholder replacements
     */
    public void send(CommandSender sender, String key, String... placeholders) {
        String message = getMessage(key, placeholders);
        
        if (message == null || message.isEmpty()) {
            return;
        }
        
        // Parse PlaceholderAPI placeholders if sender is a player
        if (sender instanceof Player player && isPlaceholderAPIEnabled()) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        
        // Parse MiniMessage and send
        Component component = miniMessage.deserialize(message);
        sender.sendMessage(component);
    }
    
    /**
     * Get a message from config with placeholder replacements
     * 
     * @param key The config key
     * @param placeholders Alternating placeholder keys and values
     * @return The formatted message
     */
    public String getMessage(String key, String... placeholders) {
        FileConfiguration config = plugin.getConfig();
        String message = config.getString(key, "");
        
        if (message.isEmpty()) {
            plugin.getLogger().warning("Missing message: " + key);
            return "";
        }
        
        // Replace custom placeholders
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            message = message.replace(placeholder, value);
        }
        
        // Add prefix if not already present and prefix exists
        String prefix = config.getString("messages.prefix", "");
        if (!message.contains("<prefix>") && !prefix.isEmpty()) {
            message = prefix + message;
        } else {
            message = message.replace("<prefix>", prefix);
        }
        
        return message;
    }
    
    /**
     * Get a raw message component (for custom handling)
     */
    public Component getComponent(String key, String... placeholders) {
        String message = getMessage(key, placeholders);
        return miniMessage.deserialize(message);
    }
    
    /**
     * Broadcast a message to all online players
     */
    public void broadcast(String key, String... placeholders) {
        String message = getMessage(key, placeholders);
        
        if (message.isEmpty()) {
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Parse PlaceholderAPI for each player
            String playerMessage = message;
            if (isPlaceholderAPIEnabled()) {
                playerMessage = PlaceholderAPI.setPlaceholders(player, message);
            }
            
            Component component = miniMessage.deserialize(playerMessage);
            player.sendMessage(component);
        }
    }
    
    /**
     * Check if PlaceholderAPI is enabled
     */
    private boolean isPlaceholderAPIEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }
}
