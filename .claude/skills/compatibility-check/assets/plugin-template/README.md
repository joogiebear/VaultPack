# Paper Plugin Template

Modern Paper/Folia plugin template for Minecraft 1.21.4+

## Features

- ✅ Folia-compatible schedulers
- ✅ Paper 1.21.4+ with Mojang mappings
- ✅ Config-driven messages (no hardcoded strings)
- ✅ Database support (MySQL/MongoDB)
- ✅ PlaceholderAPI integration
- ✅ Modern Adventure text components
- ✅ Proper lifecycle management
- ✅ Java 21 toolchain

## Structure

```
plugin-template/
├── build.gradle.kts          # Gradle build configuration
├── paper-plugin.yml           # Plugin metadata
├── config.yml                 # Configuration template
├── MyPlugin.java              # Main plugin class
├── MessageManager.java        # Message handling
├── DatabaseManager.java       # Database abstraction
└── MyPluginExpansion.java     # PlaceholderAPI expansion
```

## Usage

1. Copy all files from this template to your project
2. Update package names from `com.example.myplugin` to your package
3. Update plugin details in `paper-plugin.yml` and `build.gradle.kts`
4. Implement your features following the patterns established
5. Build with `./gradlew shadowJar`

## Key Patterns

### Config-Driven Messages
All messages in `config.yml`:
```java
messageManager.send(player, "messages.welcome");
```

### Folia-Compatible Scheduling
```java
// Entity scheduler
player.getScheduler().run(plugin, task -> {
    // Runs on player's region
}, null);

// Global scheduler
Bukkit.getGlobalRegionScheduler().run(plugin, task -> {
    // Runs on global region
});
```

### Database Operations
```java
databaseManager.loadPlayerData(uuid).thenAccept(data -> {
    player.getScheduler().run(plugin, task -> {
        // Back to sync for player interaction
    }, null);
});
```

## Dependencies Used

- Paper API 1.21.4
- PlaceholderAPI (optional)
- HikariCP (MySQL pooling)
- MariaDB driver (MySQL)
- MongoDB driver (optional)
