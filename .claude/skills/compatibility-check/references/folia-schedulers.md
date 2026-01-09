# Folia-Compatible Schedulers

Folia introduces regionized multithreading, requiring different scheduler patterns than traditional Paper/Bukkit. This guide covers all Folia-compatible schedulers available in the Paper API.

## Overview

Folia divides the world into independently ticking regions. Traditional `BukkitScheduler` is not region-aware and will not work correctly on Folia servers. Always use the Folia-compatible schedulers described here.

**Key principle**: These schedulers work on both Paper and Folia. On Paper, they internally delegate to appropriate mechanisms. On Folia, they ensure thread-safety across regions.

## Scheduler Types

### 1. Entity Scheduler

Use for tasks that should run on the region owning a specific entity.

```java
// Get the scheduler for an entity
EntityScheduler scheduler = entity.getScheduler();

// Run a task immediately on the entity's region
scheduler.run(plugin, scheduledTask -> {
    // This code runs on the region that owns this entity
    entity.setHealth(20.0);
}, null);

// Run a delayed task (in ticks)
scheduler.runDelayed(plugin, scheduledTask -> {
    entity.getWorld().createExplosion(entity.getLocation(), 4.0f);
}, null, 20L); // 20 ticks = 1 second

// Run a repeating task
scheduler.runAtFixedRate(plugin, scheduledTask -> {
    // Runs every 20 ticks
    if (entity.isOnGround()) {
        entity.setVelocity(entity.getVelocity().setY(0.5));
    }
}, null, 1L, 20L); // Initial delay: 1 tick, Period: 20 ticks
```

**When to use**:
- Modifying entity data (health, velocity, location)
- Entity-specific logic that needs thread-safety
- Following an entity across regions (scheduler moves with entity)

**Important notes**:
- If entity is teleporting asynchronously, the task may be delayed until teleport completes
- Scheduler is automatically cancelled if entity is removed

### 2. Region Scheduler

Use for tasks that should run on the region owning a specific location.

```java
// Get the region scheduler from Bukkit
RegionScheduler scheduler = Bukkit.getRegionScheduler();

// Run a task on the region owning a location
Location location = new Location(world, 100, 64, 200);
scheduler.run(plugin, location, scheduledTask -> {
    // This runs on the region owning (100, 64, 200)
    world.getBlockAt(location).setType(Material.DIAMOND_BLOCK);
});

// Run a delayed task
scheduler.runDelayed(plugin, location, scheduledTask -> {
    world.createExplosion(location, 3.0f);
}, 40L); // 2 seconds delay

// Run a repeating task
scheduler.runAtFixedRate(plugin, location, scheduledTask -> {
    // Check chunk every 10 seconds
    Chunk chunk = location.getChunk();
    if (chunk.getEntities().length > 50) {
        // Too many entities, do something
    }
}, 1L, 200L); // Initial: 1 tick, Period: 200 ticks (10 seconds)
```

**When to use**:
- Block modifications at specific coordinates
- Chunk-specific operations
- Location-based effects or checks
- World modifications tied to a location

**Important notes**:
- If the region at that location is not loaded, task may be delayed
- Task is tied to the location, not an entity

### 3. Global Region Scheduler

Use for tasks that don't belong to any specific region.

```java
// Get the global region scheduler
GlobalRegionScheduler scheduler = Bukkit.getGlobalRegionScheduler();

// Run a global task immediately
scheduler.run(plugin, scheduledTask -> {
    // This runs on the global region
    // Good for cross-region operations, database tasks, etc.
    database.saveAllData();
});

// Run a delayed global task
scheduler.runDelayed(plugin, scheduledTask -> {
    Bukkit.broadcastMessage("Server will restart in 5 minutes!");
}, 20L * 60); // 1 minute delay

// Run a repeating global task
scheduler.runAtFixedRate(plugin, scheduledTask -> {
    // Auto-save every 5 minutes
    getLogger().info("Auto-saving...");
    dataManager.saveAllData();
}, 20L * 60 * 5, 20L * 60 * 5); // Every 5 minutes
```

**When to use**:
- Database operations
- Cross-server/cross-region logic
- Global broadcasts
- Plugin-wide periodic tasks (auto-save, cleanup)
- Tasks that don't interact with world/entities directly

**Important notes**:
- Does NOT have access to specific regions/entities/blocks
- Best for coordination and non-region-specific operations

### 4. Async Scheduler

Use for CPU-intensive or blocking operations that should run off the main thread.

```java
// Get the async scheduler
AsyncScheduler scheduler = Bukkit.getAsyncScheduler();

// Run an async task immediately
scheduler.runNow(plugin, scheduledTask -> {
    // This runs on a separate thread pool
    // NEVER access Bukkit API from here
    List<String> data = expensiveCalculation();
    
    // Switch back to appropriate scheduler for Bukkit API access
    Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
        // Now safe to use Bukkit API
        getLogger().info("Calculation complete!");
    });
});

// Run a delayed async task
scheduler.runDelayed(plugin, scheduledTask -> {
    // Heavy computation after 5 seconds
    processLargeDataset();
}, 5L, TimeUnit.SECONDS);

// Run a repeating async task
scheduler.runAtFixedRate(plugin, scheduledTask -> {
    // Background cleanup every hour
    cleanupOldFiles();
}, 1L, 1L, TimeUnit.HOURS);
```

**When to use**:
- File I/O operations
- Network requests
- Database queries (though consider async database libraries)
- Heavy computations
- Anything that would block/lag the server

**Critical warnings**:
- **NEVER** access Bukkit API from async tasks
- **NEVER** modify world, entities, or blocks from async
- Always switch back to appropriate scheduler (entity/region/global) before touching Bukkit API

## Checking Folia vs Paper

Sometimes you need to know if you're running on Folia to make decisions:

```java
public static boolean isFolia() {
    try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
        return true;
    } catch (ClassNotFoundException e) {
        return false;
    }
}

// Usage
if (isFolia()) {
    getLogger().info("Running on Folia - using regionized schedulers");
} else {
    getLogger().info("Running on Paper - schedulers will use compatibility layer");
}
```

**Note**: Usually you don't need to check - just use the Folia-compatible schedulers everywhere.

## Common Patterns

### Pattern 1: Entity Operation with Async Prep

```java
// 1. Do async work
Bukkit.getAsyncScheduler().runNow(plugin, task -> {
    // Load data from database (async, no Bukkit API)
    PlayerData data = database.loadPlayerData(player.getUniqueId()).join();
    
    // 2. Switch to entity scheduler for Bukkit API
    player.getScheduler().run(plugin, task2 -> {
        // Now safe to modify player
        player.setHealth(data.getHealth());
        player.sendMessage("Data loaded!");
    }, null);
});
```

### Pattern 2: Location-Based with Delay

```java
Location targetLocation = player.getLocation().add(0, 0, 10);

// Spawn particle effect after 2 seconds at that location
Bukkit.getRegionScheduler().runDelayed(plugin, targetLocation, task -> {
    targetLocation.getWorld().spawnParticle(
        Particle.EXPLOSION_LARGE, 
        targetLocation, 
        1
    );
}, 40L); // 2 seconds
```

### Pattern 3: Global Periodic Task with Database

```java
// Auto-save every 5 minutes
Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
    getLogger().info("Starting auto-save...");
    
    // Do database save async
    Bukkit.getAsyncScheduler().runNow(plugin, asyncTask -> {
        try {
            database.saveAllPlayerData().join();
            
            // Log result on global scheduler
            Bukkit.getGlobalRegionScheduler().run(plugin, logTask -> {
                getLogger().info("Auto-save complete!");
            });
        } catch (Exception e) {
            getLogger().severe("Auto-save failed: " + e.getMessage());
        }
    });
}, 20L * 60 * 5, 20L * 60 * 5);
```

### Pattern 4: Teleport with Async Callback

```java
Location destination = new Location(world, 0, 100, 0);

// Teleport is async on Folia
player.teleportAsync(destination).thenAccept(success -> {
    if (success) {
        // Use entity scheduler to interact with player after teleport
        player.getScheduler().run(plugin, task -> {
            player.sendMessage("Teleported successfully!");
        }, null);
    }
});
```

## Task Cancellation

All schedulers return `ScheduledTask` which can be cancelled:

```java
// Store the task
ScheduledTask task = scheduler.runAtFixedRate(plugin, scheduledTask -> {
    // Repeating task
}, 20L, 20L);

// Cancel later
task.cancel();

// Cancel all tasks for a plugin
Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
Bukkit.getAsyncScheduler().cancelTasks(plugin);
// Entity schedulers are automatically cancelled when entity is removed
```

## Migration from Old Schedulers

### Old BukkitScheduler → New Schedulers

```java
// OLD (Don't use)
Bukkit.getScheduler().runTask(plugin, () -> {
    player.sendMessage("Hello!");
});

// NEW (Folia-compatible)
player.getScheduler().run(plugin, task -> {
    player.sendMessage("Hello!");
}, null);

// ---

// OLD (Don't use)
Bukkit.getScheduler().runTaskLater(plugin, () -> {
    world.createExplosion(location, 4.0f);
}, 20L);

// NEW (Folia-compatible)
Bukkit.getRegionScheduler().runDelayed(plugin, location, task -> {
    world.createExplosion(location, 4.0f);
}, 20L);

// ---

// OLD (Don't use)
Bukkit.getScheduler().runTaskTimer(plugin, () -> {
    database.save();
}, 0L, 20L * 60);

// NEW (Folia-compatible)
Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
    database.save();
}, 1L, 20L * 60);

// ---

// OLD (Don't use)
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    expensiveOperation();
});

// NEW (Folia-compatible)
Bukkit.getAsyncScheduler().runNow(plugin, task -> {
    expensiveOperation();
});
```

## Decision Tree

Use this to choose the right scheduler:

1. **Does it interact with Bukkit API?**
   - No → Use `AsyncScheduler`
   - Yes → Continue to #2

2. **Is it tied to a specific entity?**
   - Yes → Use `EntityScheduler` (from entity)
   - No → Continue to #3

3. **Is it tied to a specific location/block/chunk?**
   - Yes → Use `RegionScheduler` (from Bukkit)
   - No → Continue to #4

4. **Is it global/cross-region/database/broadcast?**
   - Yes → Use `GlobalRegionScheduler` (from Bukkit)

## Performance Considerations

- **Entity Scheduler**: Automatically moves with entity across regions - very efficient
- **Region Scheduler**: Efficient for location-specific tasks
- **Global Scheduler**: Single-threaded global region - don't overload with too many tasks
- **Async Scheduler**: Uses thread pool - great for blocking operations, but switching back to sync has overhead

## Common Mistakes

❌ **Using old BukkitScheduler**
```java
Bukkit.getScheduler().runTask(plugin, () -> { /* ... */ });
```

❌ **Accessing Bukkit API from async**
```java
Bukkit.getAsyncScheduler().runNow(plugin, task -> {
    player.sendMessage("Hi!"); // CRASH on Folia!
});
```

❌ **Not specifying correct scheduler for task type**
```java
// This is a location-based task, should use RegionScheduler
Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
    world.getBlockAt(location).setType(Material.STONE);
});
```

✅ **Correct patterns shown above**

## Summary

- **Always use Folia-compatible schedulers** - they work on both Paper and Folia
- **Choose the right scheduler type** for your task
- **Never access Bukkit API from async** schedulers
- **Use async for blocking operations** (I/O, network, heavy computation)
- **Store ScheduledTask references** if you need to cancel tasks later
