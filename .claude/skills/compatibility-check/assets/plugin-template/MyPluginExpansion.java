package com.example.myplugin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for MyPlugin
 * 
 * Provides custom placeholders that other plugins can use.
 * Examples: %myplugin_balance%, %myplugin_level%, etc.
 */
public class MyPluginExpansion extends PlaceholderExpansion {
    
    private final MyPlugin plugin;
    
    public MyPluginExpansion(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "myplugin"; // Used as %myplugin_<placeholder>%
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // Required for the expansion to show up
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        // Example placeholders - implement your own logic
        
        // %myplugin_example%
        if (params.equalsIgnoreCase("example")) {
            return "Example value";
        }
        
        // %myplugin_player_name%
        if (params.equalsIgnoreCase("player_name")) {
            return player.getName();
        }
        
        // Add your custom placeholders here
        // Examples:
        // - %myplugin_balance%
        // - %myplugin_level%
        // - %myplugin_rank%
        // - %myplugin_quest_active%
        
        return null; // Placeholder not recognized
    }
}
