# PlaceholderAPI Integration

Guide for integrating PlaceholderAPI into Paper/Folia plugins, including creating custom expansions and using placeholders from other plugins.

## Overview

PlaceholderAPI (PAPI) allows plugins to share placeholders (like `%player_name%`, `%vault_eco_balance%`) across the server. You can:
1. **Use placeholders** from other plugins in your messages/configs
2. **Create expansions** to provide your own placeholders for other plugins to use

## Dependencies

### build.gradle.kts

```kotlin
repositories {
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("me.clip:placeholderapi:2.11.5")
}
```

### plugin.yml

```yaml
depend: []
softdepend: [PlaceholderAPI]
```

## Using Placeholders from Other Plugins

### Basic Usage

```java
import me.clip.placeholderapi.PlaceholderAPI;

public class MessageManager {
    
    public void sendMessage(Player player, String message) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            // Parse placeholders in the message
            message = PlaceholderAPI.setPlaceholders(player, message);
        }
        
        player.sendMessage(message);
    }
}
```

### In Configuration Files

```yaml
# config.yml
messages:
  welcome: "&aWelcome &b%player_name%&a! You have &e%vault_eco_balance% &acoins."
  quest-start: "&7[&6Quest&7] &fStarted: &e{quest} &7- Difficulty: &c%myplugin_quest_difficulty%"

# Usage in code
String message = config.getString("messages.welcome");
message = PlaceholderAPI.setPlaceholders(player, message);
player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
```

### With Adventure Components

```java
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public Component parseMessage(Player player, String message) {
    // Replace PAPI placeholders
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
        message = PlaceholderAPI.setPlaceholders(player, message);
    }
    
    // Parse MiniMessage format
    return MiniMessage.miniMessage().deserialize(message);
}

// Usage
Component component = parseMessage(player, 
    "<green>Welcome <yellow>%player_name%</yellow>! Balance: <gold>%vault_eco_balance%</gold>");
player.sendMessage(component);
```

### Bulk Placeholder Replacement

```java
public List<String> parseList(Player player, List<String> messages) {
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
        return PlaceholderAPI.setPlaceholders(player, messages);
    }
    return messages;
}

// Usage
List<String> lore = config.getStringList("items.sword.lore");
lore = parseList(player, lore);
```

## Creating Your Own Expansion

### Simple Expansion

```java
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
        return true; // Required or expansion will not show up
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        // %myplugin_coins%
        if (params.equalsIgnoreCase("coins")) {
            return String.valueOf(plugin.getPlayerData(player).getCoins());
        }
        
        // %myplugin_level%
        if (params.equalsIgnoreCase("level")) {
            return String.valueOf(plugin.getPlayerData(player).getLevel());
        }
        
        // %myplugin_quest_active%
        if (params.equalsIgnoreCase("quest_active")) {
            Quest activeQuest = plugin.getQuestManager().getActiveQuest(player);
            return activeQuest != null ? activeQuest.getName() : "None";
        }
        
        // %myplugin_quest_progress_<questid>%
        if (params.startsWith("quest_progress_")) {
            String questId = params.substring("quest_progress_".length());
            int progress = plugin.getQuestManager().getProgress(player, questId);
            return String.valueOf(progress);
        }
        
        return null; // Placeholder not recognized
    }
}
```

### Registration

```java
@Override
public void onEnable() {
    // Check if PlaceholderAPI is enabled
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
        new MyPluginExpansion(this).register();
        getLogger().info("PlaceholderAPI expansion registered!");
    }
}
```

### Advanced Expansion with OfflinePlayer Support

```java
public class AdvancedExpansion extends PlaceholderExpansion {
    
    private final MyPlugin plugin;
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // This method supports both online and offline players
        
        if (player == null) {
            return "";
        }
        
        // Get data (works for offline players if cached)
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        
        if (data == null) {
            return "N/A"; // Data not loaded
        }
        
        // %myplugin_total_quests_completed%
        if (params.equalsIgnoreCase("total_quests_completed")) {
            return String.valueOf(data.getCompletedQuestsCount());
        }
        
        // %myplugin_rank%
        if (params.equalsIgnoreCase("rank")) {
            return data.getRank().getDisplayName();
        }
        
        return null;
    }
}
```

## Dynamic Placeholders with Parameters

```java
@Override
public String onPlaceholderRequest(Player player, @NotNull String params) {
    // %myplugin_top_<stat>_<position>%
    // Example: %myplugin_top_coins_1% - Top player by coins
    if (params.startsWith("top_")) {
        String[] parts = params.split("_");
        if (parts.length == 3) {
            String stat = parts[1]; // "coins", "level", etc.
            int position;
            try {
                position = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return "Invalid position";
            }
            
            List<PlayerData> topPlayers = plugin.getLeaderboard().getTop(stat, 10);
            if (position > 0 && position <= topPlayers.size()) {
                PlayerData topPlayer = topPlayers.get(position - 1);
                return topPlayer.getName();
            }
            return "N/A";
        }
    }
    
    // %myplugin_quest_<questid>_progress%
    if (params.startsWith("quest_") && params.endsWith("_progress")) {
        String questId = params.substring(6, params.length() - 9);
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest != null) {
            int progress = quest.getProgress(player);
            int required = quest.getRequiredProgress();
            return progress + "/" + required;
        }
    }
    
    return null;
}
```

## Relational Placeholders

For placeholders that involve two players (like in holograms showing nearby players):

```java
import me.clip.placeholderapi.expansion.Relational;

public class RelationalExpansion extends PlaceholderExpansion implements Relational {
    
    @Override
    public String onPlaceholderRequest(Player one, Player two, String params) {
        if (one == null || two == null) {
            return "";
        }
        
        // %rel_myplugin_distance%
        if (params.equalsIgnoreCase("distance")) {
            double distance = one.getLocation().distance(two.getLocation());
            return String.format("%.1f", distance);
        }
        
        // %rel_myplugin_same_team%
        if (params.equalsIgnoreCase("same_team")) {
            Team teamOne = plugin.getTeamManager().getTeam(one);
            Team teamTwo = plugin.getTeamManager().getTeam(two);
            return teamOne != null && teamOne.equals(teamTwo) ? "Yes" : "No";
        }
        
        return null;
    }
}
```

## Formatted Placeholders

### Number Formatting

```java
import java.text.DecimalFormat;
import java.text.NumberFormat;

private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.##");
private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();

@Override
public String onPlaceholderRequest(Player player, @NotNull String params) {
    PlayerData data = plugin.getPlayerData(player);
    
    // %myplugin_coins_formatted%
    if (params.equalsIgnoreCase("coins_formatted")) {
        return DECIMAL_FORMAT.format(data.getCoins());
    }
    
    // %myplugin_coins_currency%
    if (params.equalsIgnoreCase("coins_currency")) {
        return CURRENCY_FORMAT.format(data.getCoins());
    }
    
    // %myplugin_coins_short%
    if (params.equalsIgnoreCase("coins_short")) {
        return formatShort(data.getCoins());
    }
    
    return null;
}

private String formatShort(long number) {
    if (number >= 1_000_000_000) return String.format("%.1fB", number / 1_000_000_000.0);
    if (number >= 1_000_000) return String.format("%.1fM", number / 1_000_000.0);
    if (number >= 1_000) return String.format("%.1fK", number / 1_000.0);
    return String.valueOf(number);
}
```

### Time Formatting

```java
@Override
public String onPlaceholderRequest(Player player, @NotNull String params) {
    // %myplugin_playtime%
    if (params.equalsIgnoreCase("playtime")) {
        long millis = plugin.getPlayerData(player).getPlaytime();
        return formatTime(millis);
    }
    
    // %myplugin_last_login%
    if (params.equalsIgnoreCase("last_login")) {
        long timestamp = plugin.getPlayerData(player).getLastLogin();
        return formatTimeSince(timestamp);
    }
    
    return null;
}

private String formatTime(long millis) {
    long seconds = millis / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    long days = hours / 24;
    
    if (days > 0) return days + "d " + (hours % 24) + "h";
    if (hours > 0) return hours + "h " + (minutes % 60) + "m";
    if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
    return seconds + "s";
}

private String formatTimeSince(long timestamp) {
    long diff = System.currentTimeMillis() - timestamp;
    long seconds = diff / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;
    long days = hours / 24;
    
    if (days > 0) return days + " day" + (days > 1 ? "s" : "") + " ago";
    if (hours > 0) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
    if (minutes > 0) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
    return "Just now";
}
```

## Performance Considerations

### Caching Expensive Calculations

```java
public class OptimizedExpansion extends PlaceholderExpansion {
    
    private final MyPlugin plugin;
    private final Map<String, CachedValue<String>> cache = new ConcurrentHashMap<>();
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        // For expensive operations, cache the result
        if (params.equalsIgnoreCase("top_player")) {
            return getCached("top_player", 60000, () -> {
                // This expensive operation only runs once per minute
                PlayerData top = plugin.getLeaderboard().getTopPlayer();
                return top != null ? top.getName() : "N/A";
            });
        }
        
        return null;
    }
    
    private String getCached(String key, long ttl, Supplier<String> supplier) {
        CachedValue<String> cached = cache.get(key);
        long now = System.currentTimeMillis();
        
        if (cached == null || now - cached.timestamp > ttl) {
            String value = supplier.get();
            cache.put(key, new CachedValue<>(value, now));
            return value;
        }
        
        return cached.value;
    }
    
    private static class CachedValue<T> {
        final T value;
        final long timestamp;
        
        CachedValue(T value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
```

### Async Data Loading

```java
@Override
public String onPlaceholderRequest(Player player, @NotNull String params) {
    UUID uuid = player.getUniqueId();
    
    // For database queries, return cached value or "Loading..."
    if (params.equalsIgnoreCase("total_quests")) {
        // Check if data is loaded
        if (plugin.getDataManager().isLoaded(uuid)) {
            return String.valueOf(plugin.getDataManager().getTotalQuests(uuid));
        }
        
        // If not loaded, trigger async load and return placeholder
        plugin.getDataManager().loadAsync(uuid);
        return "Loading...";
    }
    
    return null;
}
```

## Testing Placeholders

```java
// In onEnable() or a command
if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
    Player testPlayer = Bukkit.getPlayer("TestPlayer");
    if (testPlayer != null) {
        String result = PlaceholderAPI.setPlaceholders(testPlayer, "%myplugin_coins%");
        getLogger().info("Placeholder test result: " + result);
    }
}
```

## Common Patterns

### Using in Scoreboard

```java
public void updateScoreboard(Player player) {
    Scoreboard scoreboard = player.getScoreboard();
    Objective objective = scoreboard.getObjective("sidebar");
    
    List<String> lines = Arrays.asList(
        "&6&lMy Server",
        "&7",
        "&eCoins: &f%myplugin_coins%",
        "&eLevel: &f%myplugin_level%",
        "&7",
        "&aOnline: &f%server_online%"
    );
    
    // Parse placeholders
    lines = PlaceholderAPI.setPlaceholders(player, lines);
    
    // Set scoreboard lines
    for (int i = 0; i < lines.size(); i++) {
        Score score = objective.getScore(ChatColor.translateAlternateColorCodes('&', lines.get(i)));
        score.setScore(lines.size() - i);
    }
}
```

### Using in Items

```java
public ItemStack createQuestItem(Player player, Quest quest) {
    ItemStack item = new ItemStack(Material.PAPER);
    ItemMeta meta = item.getItemMeta();
    
    // Title with placeholders
    String title = config.getString("items.quest.title")
        .replace("{quest}", quest.getName());
    title = PlaceholderAPI.setPlaceholders(player, title);
    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
    
    // Lore with placeholders
    List<String> lore = config.getStringList("items.quest.lore");
    lore = lore.stream()
        .map(line -> line
            .replace("{quest}", quest.getName())
            .replace("{progress}", String.valueOf(quest.getProgress(player)))
        )
        .collect(Collectors.toList());
    lore = PlaceholderAPI.setPlaceholders(player, lore);
    lore = lore.stream()
        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
        .collect(Collectors.toList());
    meta.setLore(lore);
    
    item.setItemMeta(meta);
    return item;
}
```

## Summary

- **Using placeholders**: Use `PlaceholderAPI.setPlaceholders(player, string)` for any player-specific text
- **Creating expansions**: Extend `PlaceholderExpansion` and override `onPlaceholderRequest()`
- **Offline support**: Override `onRequest()` instead for offline player support
- **Caching**: Cache expensive operations to avoid performance issues
- **Formatting**: Provide formatted versions of placeholders (numbers, time, etc.)
- **Testing**: Always test your placeholders work correctly
- **Soft dependency**: Use `softdepend` in plugin.yml and check if PAPI is enabled before using
