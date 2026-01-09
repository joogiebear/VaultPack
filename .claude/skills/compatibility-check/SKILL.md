---
name: paper-folia-dev
description: Comprehensive Paper and Folia plugin development for Minecraft 1.21.4+. Use when developing Minecraft server plugins, implementing RPG/gameplay mechanics, economy systems, database integrations (MySQL/MongoDB), PlaceholderAPI expansions, or ensuring Folia compatibility. Emphasizes Folia-compatible schedulers, config-driven messages, event-driven architecture, and modern Paper APIs including data components, PDC, and Adventure text components.
---

# Paper and Folia Plugin Development

Complete guide for building modern, high-performance Paper plugins compatible with both Paper and Folia servers for Minecraft 1.21.4+.

## When to Use This Skill

Use this skill when:
- Creating new Paper/Folia plugins from scratch
- Implementing RPG mechanics, quests, gameplay features, or economy systems
- Integrating databases (MySQL or MongoDB)
- Adding PlaceholderAPI support
- Ensuring Folia compatibility with proper schedulers
- Working with modern Paper APIs (Data Components, PDC, registries)
- Following best architectural practices for maintainable plugins

## Quick Start

### Creating a New Plugin

1. **Copy the plugin template** from `assets/plugin-template/`:
   ```bash
   cp -r assets/plugin-template/ /path/to/your/plugin
   ```

2. **Update package names** from `com.example.myplugin` to your package

3. **Configure `paper-plugin.yml`**:
   - Set name, version, description, author
   - Ensure `folia-supported: true` is present

4. **Build the plugin**:
   ```bash
   ./gradlew shadowJar
   ```

### Key Principles to Follow

**Always**:
- ✅ Use Folia-compatible schedulers (EntityScheduler, RegionScheduler, GlobalRegionScheduler)
- ✅ Put all messages in config.yml, never hardcode strings
- ✅ Use event-driven architecture (EventBus patterns)
- ✅ Design with abstraction for future extensibility
- ✅ Use async operations for database/I/O
- ✅ Pass dependencies rather than hardcoding lookups

**Never**:
- ❌ Use old `BukkitScheduler` (not Folia-compatible)
- ❌ Hardcode user-facing messages in source code
- ❌ Tightly couple components (e.g., Quest depending directly on ProgressionPool type)
- ❌ Access Bukkit API from async threads
- ❌ Perform blocking I/O on main thread

## Core Patterns

### Folia-Compatible Scheduling

```java
// Entity-specific task (runs on entity's region)
player.getScheduler().run(plugin, task -> {
    player.sendMessage("Hello!");
}, null);

// Location-specific task (runs on location's region)
Bukkit.getRegionScheduler().run(plugin, location, task -> {
    world.getBlockAt(location).setType(Material.DIAMOND_BLOCK);
});

// Global task (database, cross-region logic)
Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
    database.saveAllData();
});

// Async task (heavy computation, I/O)
Bukkit.getAsyncScheduler().runNow(plugin, task -> {
    // NEVER access Bukkit API here!
    expensiveCalculation();
});
```

See `references/folia-schedulers.md` for complete guide.

### Config-Driven Messages

```yaml
# config.yml
messages:
  quest-started: "<green>Started quest: <yellow>{quest}</yellow>!"
  quest-completed: "<gold>Completed {quest}! Reward: {reward}</gold>"
```

```java
// In code - use MessageManager
messageManager.send(player, "messages.quest-started", 
    "{quest}", questName);
```

Never hardcode:
```java
// BAD - hardcoded message
player.sendMessage("Started quest: " + questName);
```

### Event-Driven Architecture

```java
// GOOD: Components communicate through events
public class Quest {
    private final EventBus eventBus;
    
    public void progressTask(Task task, int amount) {
        // Post event, let listeners handle it
        eventBus.post(new TaskProgressEvent(this, task, amount));
    }
}

public class ProgressionPool {
    @Subscribe
    public void onTaskProgress(TaskProgressEvent event) {
        // Pool listens to events
        updateProgress(event.getQuest(), event.getTask());
    }
}

// BAD: Tight coupling
public class Quest {
    private ProgressionPool pool; // Hardcoded dependency
    
    public void progressTask(Task task, int amount) {
        pool.updateProgress(this, task, amount); // Direct call
    }
}
```

### Database Integration

```java
// Always async database operations
public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
        // Database query on async thread
        return database.query("SELECT * FROM players WHERE uuid = ?", uuid);
    }, Bukkit.getAsyncScheduler().getExecutor(plugin));
}

// Usage with Folia scheduler
loadPlayerData(uuid).thenAccept(data -> {
    // Switch to player's region for interaction
    player.getScheduler().run(plugin, task -> {
        player.sendMessage("Data loaded!");
    }, null);
});
```

See `references/database-patterns.md` for MySQL and MongoDB examples.

### Abstraction for Extensibility

```java
// GOOD: Abstract base class supports multiple implementations
public abstract class ProgressionState {
    public abstract void incrementProgress(String taskId, int amount);
}

public class PersonalProgressionState extends ProgressionState {
    private final UUID playerId;
    // Personal progress
}

public class GlobalProgressionState extends ProgressionState {
    // Shared progress
}

public class TeamProgressionState extends ProgressionState {
    private final Set<UUID> teamMembers;
    // Team progress
}

// Quest doesn't know which type it has
public class Quest {
    private final ProgressionState progressionState;
    
    public void progress(String taskId, int amount) {
        progressionState.incrementProgress(taskId, amount);
    }
}
```

### Dependency Injection

```java
// GOOD: Pass dependencies, use generalized methods
public interface ProgressionPool {
    List<Placeholder<?>> getQuestSpecificPlaceholders(Quest quest);
}

public class Quest {
    private final ProgressionPool pool;
    
    public List<Placeholder<?>> getPlaceholders() {
        // Pool provides placeholders, Quest doesn't check pool type
        return pool.getQuestSpecificPlaceholders(this);
    }
}

// BAD: Hardcoded type checking
public class Quest {
    private PoolType poolType;
    
    public List<Placeholder<?>> getPlaceholders() {
        if (poolType == PoolType.GLOBAL) {
            // Not extensible!
        }
    }
}
```

## Project Setup

### Modern Gradle Configuration (Java 21)

```kotlin
// build.gradle.kts
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.5")
    implementation("com.zaxxer:HikariCP:5.1.0")
}
```

### Plugin Configuration (paper-plugin.yml)

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.myplugin.MyPlugin
api-version: 1.21
folia-supported: true  # Important!

dependencies:
  server:
    PlaceholderAPI:
      load: BEFORE
      required: false
```

Complete template available in `assets/plugin-template/`.

## Common Use Cases

### RPG/Gameplay Mechanics

For quests, skills, custom abilities:
- Use event-driven architecture for progression tracking
- Separate data handlers by scope (user data vs. quest data)
- Use region schedulers for player-specific effects
- Store all configuration in YAML files

### Economy Systems

For currency, shops, trading:
- Use database for persistent storage
- Cache frequently accessed data in memory
- Use global scheduler for periodic save operations
- Integrate with PlaceholderAPI for balance displays

### Database-Backed Features

For any persistent data:
1. Choose MySQL (structured) or MongoDB (flexible)
2. Use connection pooling (HikariCP for MySQL)
3. Always async operations with CompletableFuture
4. Switch back to appropriate scheduler before Bukkit API access

See `references/database-patterns.md` for complete examples.

## Reference Documentation

### Available References

Read these when needed for specific topics:

- **`architecture-principles.md`**: Core design patterns, separation of concerns, architectural best practices based on expert feedback
- **`folia-schedulers.md`**: Complete guide to Folia-compatible schedulers, when to use each type, migration from old BukkitScheduler
- **`database-patterns.md`**: MySQL and MongoDB integration with connection pooling, async operations, caching strategies
- **`placeholder-api.md`**: Creating PlaceholderAPI expansions, using placeholders, formatting patterns
- **`paper-api-links.md`**: Quick reference to Paper documentation for specific APIs

### When to Fetch Paper Docs

For features not covered in references, fetch from Paper's official docs:
- Data Components API: https://docs.papermc.io/paper/dev/api/data-components/
- PDC: https://docs.papermc.io/paper/dev/api/pdc/
- Registries: https://docs.papermc.io/paper/dev/api/registries/
- Complete list in `references/paper-api-links.md`

## Plugin Lifecycle

### Initialization Order

```java
@Override
public void onEnable() {
    // 1. Load configuration
    saveDefaultConfig();
    
    // 2. Initialize database (if used)
    if (getConfig().getBoolean("database.enabled")) {
        database = new DatabaseConnection(getConfig());
        database.connect().join();
    }
    
    // 3. Register event listeners
    getServer().getPluginManager().registerEvents(listener, this);
    
    // 4. Register commands
    getCommand("mycommand").setExecutor(commandHandler);
    
    // 5. Register PlaceholderAPI expansion
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
        new MyExpansion(this).register();
    }
    
    // 6. Start scheduled tasks (Folia-compatible!)
    Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
        autoSave();
    }, 20L * 60 * 5, 20L * 60 * 5); // Every 5 minutes
}
```

### Shutdown Sequence

```java
@Override
public void onDisable() {
    // 1. Cancel all tasks
    Bukkit.getGlobalRegionScheduler().cancelTasks(this);
    Bukkit.getAsyncScheduler().cancelTasks(this);
    
    // 2. Save all data (wait for completion!)
    saveAllData().join();
    
    // 3. Close database connections
    if (database != null) {
        database.close();
    }
}
```

## Common Mistakes to Avoid

### ❌ Using Old BukkitScheduler
```java
// DON'T
Bukkit.getScheduler().runTask(plugin, () -> { });

// DO
player.getScheduler().run(plugin, task -> { }, null);
```

### ❌ Hardcoding Messages
```java
// DON'T
player.sendMessage("Quest started!");

// DO
messageManager.send(player, "messages.quest-started");
```

### ❌ Accessing Bukkit API from Async
```java
// DON'T
Bukkit.getAsyncScheduler().runNow(plugin, task -> {
    player.sendMessage("Hello!"); // CRASH on Folia!
});

// DO
Bukkit.getAsyncScheduler().runNow(plugin, task -> {
    String data = loadFromDatabase();
    player.getScheduler().run(plugin, task2 -> {
        player.sendMessage(data); // Safe!
    }, null);
});
```

### ❌ Tight Coupling
```java
// DON'T
public class Quest {
    private PoolType poolType;
    public void checkPool() {
        if (poolType == PoolType.GLOBAL) { /* hardcoded logic */ }
    }
}

// DO
public interface ProgressionPool {
    List<Placeholder<?>> getPlaceholders(Quest quest);
}
// Quest calls pool.getPlaceholders(this) - extensible!
```

## Testing Folia Compatibility

```java
public static boolean isFolia() {
    try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
        return true;
    } catch (ClassNotFoundException e) {
        return false;
    }
}
```

Usually unnecessary - just use Folia-compatible schedulers everywhere.

## Summary Checklist

When developing a Paper/Folia plugin:

- [ ] Java 21 toolchain configured
- [ ] `folia-supported: true` in paper-plugin.yml
- [ ] All schedulers are Folia-compatible (no BukkitScheduler)
- [ ] All messages in config.yml (no hardcoded strings)
- [ ] Database operations are async (CompletableFuture)
- [ ] Event-driven architecture (EventBus for component communication)
- [ ] Abstraction for extensibility (interfaces, abstract classes)
- [ ] Dependency injection (pass dependencies, don't hardcode lookups)
- [ ] Proper lifecycle management (save on disable, cancel tasks)
- [ ] PlaceholderAPI integration (if displaying data to other plugins)

## Getting Help

1. Check the reference documents in this skill first
2. Fetch specific Paper API docs from `references/paper-api-links.md`
3. Follow architectural principles in `references/architecture-principles.md`
4. Use the plugin template in `assets/plugin-template/` as starting point
