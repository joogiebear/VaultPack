# Architecture Principles for Paper/Folia Plugins

This document outlines core architectural patterns and principles for building maintainable, extensible Paper/Folia plugins.

## Core Principles

### 1. Separation of Concerns

**Data Handler Separation**
Not all data is user-specific. Separate data handling by scope and lifecycle:

```java
// GOOD: Separate data handlers by scope
public class UserDataHandler {
    // Only handles player-specific data, required when player is online
    private final Map<UUID, PlayerData> onlinePlayerData;
}

public class QuestDataHandler {
    // Handles quest progression data - can be global, team-based, etc.
    // Has its own persistence strategy (database, local files)
    private final DatabaseConnection database;
    
    public CompletableFuture<QuestData> loadQuestData(String questId) {
        // Async loading from database
    }
}

// BAD: Mixing concerns
public class DataManager {
    // Everything in one place - hard to maintain and scale
    private Map<UUID, PlayerData> players;
    private Map<String, QuestData> quests;
    private Map<String, EconomyData> economy;
}
```

### 2. Folia-Compatible Schedulers

Always use Paper's Folia-compatible schedulers. Never use deprecated BukkitScheduler for region-specific tasks.

```java
// GOOD: Folia-compatible region scheduler
player.getScheduler().run(plugin, task -> {
    // This runs on the region owning the player
    player.sendMessage("Hello!");
}, null);

// GOOD: Global scheduler for non-region tasks
Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
    // This runs on the global region
    // Good for database operations, cross-region logic
});

// GOOD: Location-based scheduler
Bukkit.getRegionScheduler().run(plugin, location, task -> {
    // This runs on the region owning this location
});

// BAD: Old BukkitScheduler (not Folia-compatible)
Bukkit.getScheduler().runTask(plugin, () -> {
    // This won't work properly on Folia
});
```

### 3. Config-Driven Messages

All user-facing messages must be in config files, never hardcoded in source code.

```java
// GOOD: Messages in config
public class Messages {
    private final FileConfiguration config;
    
    public Component getQuestStarted(String questName) {
        return MiniMessage.miniMessage().deserialize(
            config.getString("messages.quest-started")
                .replace("{quest}", questName)
        );
    }
}

// In config.yml:
// messages:
//   quest-started: "<green>Started quest: <yellow>{quest}</yellow>!"
//   quest-completed: "<gold>Completed quest: <yellow>{quest}</yellow>!"

// BAD: Hardcoded messages
public void startQuest(Player player, Quest quest) {
    player.sendMessage("Started quest: " + quest.getName());
}
```

### 4. Event-Driven Architecture

Use event buses and listeners instead of tight coupling. Let components communicate through events.

```java
// GOOD: Event-driven with EventBus
public class Quest {
    private final EventBus eventBus;
    
    public void progressTask(TaskProgressEvent event) {
        // Quest forwards the event, doesn't process it
        eventBus.post(new TaskProgressEvent(this, event.getTask(), event.getAmount()));
    }
}

public class ProgressionPool {
    public void onTaskProgress(TaskProgressEvent event) {
        // Pool listens to events, not hardcoded into Quest
        updateProgress(event.getQuest(), event.getTask(), event.getAmount());
    }
}

// BAD: Tight coupling
public class Quest {
    private ProgressionPool pool; // Hardcoded dependency
    
    public void progressTask(Task task, int amount) {
        pool.updateProgress(this, task, amount); // Directly calling pool
    }
}
```

### 5. Abstraction for Extensibility

Design with interfaces and abstract classes to support future features without refactoring.

```java
// GOOD: Abstract progression state for different pool types
public abstract class ProgressionState {
    protected final String questId;
    
    public abstract boolean isCompleted(String taskId);
    public abstract void incrementProgress(String taskId, int amount);
    public abstract Map<String, Integer> getProgress();
}

public class PersonalProgressionState extends ProgressionState {
    private final UUID playerId;
    // Personal progress tracking
}

public class GlobalProgressionState extends ProgressionState {
    // Shared across all players
}

public class TeamProgressionState extends ProgressionState {
    private final Set<UUID> teamMembers;
    // Team-based progress tracking
}

// Profile passes appropriate state to Quest
public class Profile {
    public Quest createQuest(String questId, ProgressionPool pool) {
        ProgressionState state = pool.createProgressionState(questId, this);
        return new Quest(questId, state);
    }
}

// BAD: Hardcoded single type
public class Quest {
    private Map<String, Integer> progress; // Can't support different pool types
}
```

### 6. Dependency Injection Over Hardcoded Lookups

Pass dependencies to components rather than having them look up their own dependencies.

```java
// GOOD: Dependency injection with generalized methods
public interface ProgressionPool {
    ProgressionState createProgressionState(String questId, Profile profile);
    List<Placeholder<?>> getQuestSpecificPlaceholders(Quest quest);
}

public class Quest {
    private final ProgressionPool pool;
    
    public List<Placeholder<?>> getPlaceholders() {
        // Pool provides placeholders, Quest doesn't know about pool types
        return pool.getQuestSpecificPlaceholders(this);
    }
}

// BAD: Hardcoded type checking
public class Quest {
    private PoolType poolType;
    
    public List<Placeholder<?>> getPlaceholders() {
        if (poolType == PoolType.GLOBAL) {
            // Hardcoded logic for each type
            return Arrays.asList(/* global placeholders */);
        } else if (poolType == PoolType.PERSONAL) {
            return Arrays.asList(/* personal placeholders */);
        }
        // Not extensible to new pool types
    }
}
```

## Data Persistence Patterns

### Async Database Operations

Always perform database operations asynchronously to avoid blocking the server.

```java
public class QuestDataHandler {
    private final ExecutorService asyncExecutor;
    
    public CompletableFuture<QuestData> loadQuest(String questId) {
        return CompletableFuture.supplyAsync(() -> {
            // Database query on async thread
            return database.query("SELECT * FROM quests WHERE id = ?", questId);
        }, asyncExecutor);
    }
    
    public CompletableFuture<Void> saveQuest(QuestData data) {
        return CompletableFuture.runAsync(() -> {
            // Database write on async thread
            database.update("UPDATE quests SET data = ? WHERE id = ?", 
                data.serialize(), data.getId());
        }, asyncExecutor);
    }
}

// Usage with Folia scheduler
public void loadPlayerQuests(Player player) {
    questDataHandler.loadQuest(questId).thenAccept(questData -> {
        // Switch back to player's region for server interaction
        player.getScheduler().run(plugin, task -> {
            player.sendMessage("Quest loaded!");
        }, null);
    });
}
```

### Local vs Database Persistence

Provide options for both local file storage and database storage.

```java
public interface DataPersistence<T> {
    CompletableFuture<T> load(String id);
    CompletableFuture<Void> save(String id, T data);
}

public class LocalFilePersistence<T> implements DataPersistence<T> {
    // Stores in YAML/JSON files
}

public class DatabasePersistence<T> implements DataPersistence<T> {
    // Stores in MySQL/MongoDB
}

// Configuration driven
persistence:
  type: database # or 'local'
  database:
    type: mysql # or 'mongodb'
    connection: "jdbc:mysql://localhost/quests"
```

## Component Lifecycle

### Initialization Order

1. Load configuration
2. Initialize database connections (if using databases)
3. Register event listeners
4. Load persistent data
5. Register commands
6. Start scheduled tasks

```java
@Override
public void onEnable() {
    // 1. Configuration
    saveDefaultConfig();
    this.messages = new Messages(getConfig());
    
    // 2. Database (if configured)
    if (getConfig().getBoolean("database.enabled")) {
        this.database = new DatabaseConnection(getConfig().getConfigurationSection("database"));
        database.connect().join(); // Wait for connection
    }
    
    // 3. Data handlers
    this.questDataHandler = new QuestDataHandler(database, getDataFolder());
    
    // 4. Event listeners
    getServer().getPluginManager().registerEvents(new QuestListener(this), this);
    
    // 5. Commands
    this.questCommand = new QuestCommand(this);
    getLifecycleManager().registerEventHandler(/* ... */);
    
    // 6. Scheduled tasks (using Folia-compatible schedulers)
    Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
        // Auto-save every 5 minutes
        saveAllData();
    }, 5 * 60 * 20, 5 * 60 * 20);
}
```

### Shutdown Sequence

```java
@Override
public void onDisable() {
    // 1. Cancel all scheduled tasks
    Bukkit.getGlobalRegionScheduler().cancelTasks(this);
    
    // 2. Save all data
    saveAllData().join(); // Wait for completion
    
    // 3. Close database connections
    if (database != null) {
        database.close();
    }
    
    // 4. Cleanup resources
    questDataHandler.shutdown();
}
```

## Summary

These principles ensure:
- **Maintainability**: Clear separation of concerns, easy to understand
- **Extensibility**: New features don't require refactoring existing code
- **Performance**: Async operations, Folia-compatible for multi-threaded servers
- **Configurability**: Messages and behavior driven by configs
- **Testability**: Dependency injection makes unit testing easier
