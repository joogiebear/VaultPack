# Database Integration Patterns

Guide for integrating MongoDB and MySQL databases into Paper/Folia plugins with async operations and proper connection management.

## General Principles

1. **Always use async operations** - Database I/O should never block the server
2. **Connection pooling** - Reuse connections efficiently
3. **Graceful shutdown** - Close connections properly on plugin disable
4. **Configuration-driven** - Database settings in config.yml
5. **Error handling** - Always handle database errors gracefully

## Configuration Structure

```yaml
# config.yml
database:
  enabled: true
  type: mysql # or 'mongodb'
  
  # MySQL configuration
  mysql:
    host: localhost
    port: 3306
    database: minecraft
    username: root
    password: changeme
    pool:
      minimum-idle: 2
      maximum-pool-size: 10
      connection-timeout: 30000
  
  # MongoDB configuration
  mongodb:
    uri: mongodb://localhost:27017
    database: minecraft
    pool:
      min-size: 2
      max-size: 10
```

## MySQL Integration

### Dependencies (build.gradle.kts)

```kotlin
dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0") // Connection pooling
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.0") // MariaDB driver (works for MySQL too)
}
```

### Connection Manager

```java
public class MySQLConnection {
    private final Plugin plugin;
    private HikariDataSource dataSource;
    
    public MySQLConnection(Plugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        initializeDataSource(config);
    }
    
    private void initializeDataSource(ConfigurationSection config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        String host = config.getString("host", "localhost");
        int port = config.getInt("port", 3306);
        String database = config.getString("database", "minecraft");
        String username = config.getString("username", "root");
        String password = config.getString("password", "");
        
        hikariConfig.setJdbcUrl(String.format(
            "jdbc:mariadb://%s:%d/%s",
            host, port, database
        ));
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        
        // Pool settings
        hikariConfig.setMinimumIdle(config.getInt("pool.minimum-idle", 2));
        hikariConfig.setMaximumPoolSize(config.getInt("pool.maximum-pool-size", 10));
        hikariConfig.setConnectionTimeout(config.getLong("pool.connection-timeout", 30000));
        
        // Performance settings
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        
        this.dataSource = new HikariDataSource(hikariConfig);
    }
    
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                plugin.getLogger().info("MySQL connection established!");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to connect to MySQL: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, Bukkit.getAsyncScheduler().getExecutor(plugin));
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("MySQL connection closed.");
        }
    }
    
    // Helper method for async queries
    public <T> CompletableFuture<T> executeQuery(String sql, ThrowingFunction<ResultSet, T> processor, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                // Set parameters
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return processor.apply(rs);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Database query failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, Bukkit.getAsyncScheduler().getExecutor(plugin));
    }
    
    // Helper method for async updates
    public CompletableFuture<Integer> executeUpdate(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                return stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Database update failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, Bukkit.getAsyncScheduler().getExecutor(plugin));
    }
    
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }
}
```

### Usage Example - Data Handler

```java
public class PlayerDataHandler {
    private final MySQLConnection database;
    private final Plugin plugin;
    
    public PlayerDataHandler(Plugin plugin, MySQLConnection database) {
        this.plugin = plugin;
        this.database = database;
        initializeTables();
    }
    
    private void initializeTables() {
        String createTable = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                coins INT DEFAULT 0,
                experience INT DEFAULT 0,
                last_login BIGINT,
                data JSON
            )
        """;
        
        database.executeUpdate(createTable).join();
    }
    
    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        String query = "SELECT * FROM player_data WHERE uuid = ?";
        
        return database.executeQuery(query, rs -> {
            if (rs.next()) {
                return new PlayerData(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("username"),
                    rs.getInt("coins"),
                    rs.getInt("experience"),
                    rs.getLong("last_login")
                );
            }
            return null; // Player not found
        }, uuid.toString());
    }
    
    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        String upsert = """
            INSERT INTO player_data (uuid, username, coins, experience, last_login)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                coins = VALUES(coins),
                experience = VALUES(experience),
                last_login = VALUES(last_login)
        """;
        
        return database.executeUpdate(
            upsert,
            data.getUuid().toString(),
            data.getUsername(),
            data.getCoins(),
            data.getExperience(),
            System.currentTimeMillis()
        ).thenAccept(rows -> {
            plugin.getLogger().fine("Saved player data for " + data.getUsername());
        });
    }
    
    public CompletableFuture<List<PlayerData>> getTopPlayers(String orderBy, int limit) {
        String query = "SELECT * FROM player_data ORDER BY " + orderBy + " DESC LIMIT ?";
        
        return database.executeQuery(query, rs -> {
            List<PlayerData> players = new ArrayList<>();
            while (rs.next()) {
                players.add(new PlayerData(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("username"),
                    rs.getInt("coins"),
                    rs.getInt("experience"),
                    rs.getLong("last_login")
                ));
            }
            return players;
        }, limit);
    }
}
```

### Integration with Player Events

```java
public class PlayerListener implements Listener {
    private final PlayerDataHandler dataHandler;
    private final Plugin plugin;
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load data async
        dataHandler.loadPlayerData(player.getUniqueId()).thenAccept(data -> {
            // Switch to player's region for interaction
            player.getScheduler().run(plugin, task -> {
                if (data == null) {
                    // New player
                    PlayerData newData = new PlayerData(player.getUniqueId(), player.getName());
                    dataHandler.savePlayerData(newData);
                    player.sendMessage("Welcome to the server!");
                } else {
                    // Existing player
                    player.sendMessage("Welcome back! You have " + data.getCoins() + " coins.");
                }
            }, null);
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to load player data: " + ex.getMessage());
            player.kick(Component.text("Failed to load your data. Please try again."));
            return null;
        });
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Get current player data (from cache/memory)
        PlayerData data = getPlayerData(player.getUniqueId());
        
        // Save async (don't wait, server is shutting down player anyway)
        dataHandler.savePlayerData(data).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to save player data on quit: " + ex.getMessage());
            return null;
        });
    }
}
```

## MongoDB Integration

### Dependencies (build.gradle.kts)

```kotlin
dependencies {
    implementation("org.mongodb:mongodb-driver-sync:4.11.1")
}
```

### Connection Manager

```java
public class MongoDBConnection {
    private final Plugin plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;
    
    public MongoDBConnection(Plugin plugin, ConfigurationSection config) {
        this.plugin = plugin;
        initializeConnection(config);
    }
    
    private void initializeConnection(ConfigurationSection config) {
        String uri = config.getString("uri", "mongodb://localhost:27017");
        String dbName = config.getString("database", "minecraft");
        
        // Connection pool settings
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(uri))
            .applyToConnectionPoolSettings(builder -> 
                builder
                    .minSize(config.getInt("pool.min-size", 2))
                    .maxSize(config.getInt("pool.max-size", 10))
            )
            .build();
        
        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(dbName);
    }
    
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Ping database to verify connection
                database.runCommand(new Document("ping", 1));
                plugin.getLogger().info("MongoDB connection established!");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to connect to MongoDB: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, Bukkit.getAsyncScheduler().getExecutor(plugin));
    }
    
    public MongoDatabase getDatabase() {
        return database;
    }
    
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            plugin.getLogger().info("MongoDB connection closed.");
        }
    }
    
    // Helper method for async operations
    public <T> CompletableFuture<T> executeAsync(Supplier<T> operation) {
        return CompletableFuture.supplyAsync(operation, 
            Bukkit.getAsyncScheduler().getExecutor(plugin));
    }
}
```

### Usage Example - Data Handler

```java
public class QuestDataHandler {
    private final MongoDBConnection database;
    private final MongoCollection<Document> questsCollection;
    private final Plugin plugin;
    
    public QuestDataHandler(Plugin plugin, MongoDBConnection database) {
        this.plugin = plugin;
        this.database = database;
        this.questsCollection = database.getDatabase().getCollection("quests");
        
        // Create indexes
        createIndexes();
    }
    
    private void createIndexes() {
        database.executeAsync(() -> {
            questsCollection.createIndex(Indexes.ascending("questId"));
            questsCollection.createIndex(Indexes.ascending("playerId"));
            return null;
        });
    }
    
    public CompletableFuture<QuestData> loadQuest(String questId, UUID playerId) {
        return database.executeAsync(() -> {
            Document filter = new Document("questId", questId)
                .append("playerId", playerId.toString());
            
            Document doc = questsCollection.find(filter).first();
            
            if (doc == null) {
                return null;
            }
            
            return QuestData.fromDocument(doc);
        });
    }
    
    public CompletableFuture<Void> saveQuest(QuestData quest) {
        return database.executeAsync(() -> {
            Document filter = new Document("questId", quest.getQuestId())
                .append("playerId", quest.getPlayerId().toString());
            
            Document doc = quest.toDocument();
            
            questsCollection.replaceOne(
                filter,
                doc,
                new ReplaceOptions().upsert(true)
            );
            
            return null;
        });
    }
    
    public CompletableFuture<List<QuestData>> loadPlayerQuests(UUID playerId) {
        return database.executeAsync(() -> {
            Document filter = new Document("playerId", playerId.toString());
            
            List<QuestData> quests = new ArrayList<>();
            for (Document doc : questsCollection.find(filter)) {
                quests.add(QuestData.fromDocument(doc));
            }
            
            return quests;
        });
    }
    
    public CompletableFuture<Long> getQuestCompletionCount(String questId) {
        return database.executeAsync(() -> {
            Document filter = new Document("questId", questId)
                .append("completed", true);
            
            return questsCollection.countDocuments(filter);
        });
    }
}

// Example data class with serialization
public class QuestData {
    private final String questId;
    private final UUID playerId;
    private final Map<String, Integer> progress;
    private final boolean completed;
    private final long startTime;
    
    public Document toDocument() {
        return new Document("questId", questId)
            .append("playerId", playerId.toString())
            .append("progress", new Document(progress))
            .append("completed", completed)
            .append("startTime", startTime);
    }
    
    public static QuestData fromDocument(Document doc) {
        Map<String, Integer> progress = new HashMap<>();
        Document progressDoc = doc.get("progress", Document.class);
        if (progressDoc != null) {
            progressDoc.forEach((key, value) -> 
                progress.put(key, ((Number) value).intValue())
            );
        }
        
        return new QuestData(
            doc.getString("questId"),
            UUID.fromString(doc.getString("playerId")),
            progress,
            doc.getBoolean("completed", false),
            doc.getLong("startTime")
        );
    }
}
```

## Hybrid Approach - Local Cache + Database

For high-performance plugins, cache data in memory and sync periodically:

```java
public class CachedDataHandler {
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final PlayerDataHandler databaseHandler;
    private final Plugin plugin;
    
    public CachedDataHandler(Plugin plugin, PlayerDataHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;
        
        // Auto-save every 5 minutes
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            saveAll();
        }, 20L * 60 * 5, 20L * 60 * 5);
    }
    
    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        // Check cache first
        if (cache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(cache.get(uuid));
        }
        
        // Load from database
        return databaseHandler.loadPlayerData(uuid).thenApply(data -> {
            if (data != null) {
                cache.put(uuid, data);
            }
            return data;
        });
    }
    
    public void updatePlayerData(UUID uuid, Consumer<PlayerData> updater) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            updater.accept(data);
            data.markDirty(); // Flag for save
        }
    }
    
    public CompletableFuture<Void> saveAll() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        cache.values().stream()
            .filter(PlayerData::isDirty)
            .forEach(data -> {
                futures.add(databaseHandler.savePlayerData(data));
                data.clearDirty();
            });
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
    
    public void removeFromCache(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null && data.isDirty()) {
            // Save before removing
            databaseHandler.savePlayerData(data);
        }
    }
}
```

## Error Handling Best Practices

```java
// Always handle errors in CompletableFuture chains
database.loadPlayerData(uuid)
    .thenAccept(data -> {
        // Success path
        player.sendMessage("Data loaded!");
    })
    .exceptionally(ex -> {
        // Error path
        plugin.getLogger().severe("Failed to load data: " + ex.getMessage());
        player.sendMessage("Failed to load your data. Please contact an admin.");
        return null;
    });

// For critical operations, use join() with try-catch
try {
    database.saveAllData().join(); // Wait for completion
} catch (Exception e) {
    plugin.getLogger().severe("Critical: Failed to save data: " + e.getMessage());
}
```

## Shutdown Handling

```java
@Override
public void onDisable() {
    getLogger().info("Saving all data before shutdown...");
    
    try {
        // Save all cached data
        if (cachedDataHandler != null) {
            cachedDataHandler.saveAll().join(); // Wait for completion
        }
        
        // Close database connections
        if (mysqlConnection != null) {
            mysqlConnection.close();
        }
        if (mongoConnection != null) {
            mongoConnection.close();
        }
        
        getLogger().info("Data saved and connections closed successfully!");
    } catch (Exception e) {
        getLogger().severe("Error during shutdown: " + e.getMessage());
    }
}
```

## Summary

- **Always async**: Use CompletableFuture for all database operations
- **Connection pooling**: Use HikariCP (MySQL) or built-in pooling (MongoDB)
- **Error handling**: Always use exceptionally() or try-catch
- **Caching**: Consider memory cache for frequently accessed data
- **Graceful shutdown**: Save all data and close connections in onDisable()
- **Folia-compatible**: Use async scheduler, then switch to appropriate region scheduler for Bukkit API access
