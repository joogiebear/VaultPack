package com.vaultpack.data;

import com.vaultpack.VaultPackPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Phase 2: Database connection manager with HikariCP pooling
 * Handles MySQL connections for high-performance data storage
 */
public class DatabaseManager {

    private final VaultPackPlugin plugin;
    private HikariDataSource dataSource;
    private boolean connected = false;

    // Connection retry settings
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds

    public DatabaseManager(VaultPackPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize database connection pool
     */
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                FileConfiguration config = plugin.getConfig();

                // Build HikariCP configuration
                HikariConfig hikariConfig = new HikariConfig();

                // Connection details
                String host = config.getString("database.host", "localhost");
                int port = config.getInt("database.port", 3306);
                String database = config.getString("database.name", "vaultpack");
                String username = config.getString("database.username", "root");
                String password = config.getString("database.password", "");

                // JDBC URL
                hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
                hikariConfig.setUsername(username);
                hikariConfig.setPassword(password);

                // Connection pool settings
                hikariConfig.setMaximumPoolSize(config.getInt("database.pool.maximum-pool-size", 10));
                hikariConfig.setMinimumIdle(config.getInt("database.pool.minimum-idle", 2));
                hikariConfig.setConnectionTimeout(config.getLong("database.pool.connection-timeout", 30000));
                hikariConfig.setIdleTimeout(config.getLong("database.pool.idle-timeout", 600000)); // 10 minutes
                hikariConfig.setMaxLifetime(config.getLong("database.pool.max-lifetime", 1800000)); // 30 minutes

                // Performance optimizations
                hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
                hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
                hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
                hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
                hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
                hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
                hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
                hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
                hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

                // SSL if configured
                if (config.getBoolean("database.ssl.enabled", false)) {
                    hikariConfig.addDataSourceProperty("useSSL", "true");
                    hikariConfig.addDataSourceProperty("requireSSL", "true");
                    hikariConfig.addDataSourceProperty("verifyServerCertificate",
                        config.getBoolean("database.ssl.verify-certificate", true));
                }

                // Connection pool name
                hikariConfig.setPoolName("VaultPack-MySQL-Pool");

                // Create data source
                dataSource = new HikariDataSource(hikariConfig);

                // Test connection
                try (Connection conn = dataSource.getConnection()) {
                    if (!conn.isValid(5)) {
                        throw new SQLException("Connection validation failed");
                    }
                    plugin.getLogger().info("Database connection established successfully");
                }

                // Initialize schema
                initializeSchema();

                connected = true;
                return true;

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
                connected = false;
                return false;
            }
        });
    }

    /**
     * Initialize database schema from schema.sql
     */
    private void initializeSchema() throws SQLException {
        plugin.getLogger().info("Initializing database schema...");

        try (InputStream is = plugin.getResource("schema.sql")) {
            if (is == null) {
                throw new SQLException("schema.sql not found in plugin resources");
            }

            // Read SQL file
            String sql = new BufferedReader(new InputStreamReader(is))
                .lines()
                .collect(Collectors.joining("\n"));

            // Execute each statement
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                // Split by semicolon and execute
                String[] statements = sql.split(";");
                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        stmt.execute(trimmed);
                    }
                }

                plugin.getLogger().info("Database schema initialized successfully");
            }

        } catch (Exception e) {
            throw new SQLException("Failed to initialize schema", e);
        }
    }

    /**
     * Get a connection from the pool
     * IMPORTANT: Must be closed after use (try-with-resources)
     */
    public Connection getConnection() throws SQLException {
        if (!connected || dataSource == null) {
            throw new SQLException("Database not connected");
        }
        return dataSource.getConnection();
    }

    /**
     * Execute a query with retry logic
     */
    public <T> CompletableFuture<T> executeQueryAsync(DatabaseQuery<T> query) {
        return CompletableFuture.supplyAsync(() -> {
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try (Connection conn = getConnection()) {
                    return query.execute(conn);

                } catch (SQLException e) {
                    if (attempt == MAX_RETRIES) {
                        plugin.getLogger().log(Level.SEVERE,
                            "Database query failed after " + MAX_RETRIES + " attempts", e);
                        throw new RuntimeException("Database query failed", e);
                    }

                    plugin.getLogger().warning("Database query failed (attempt " + attempt + "/" +
                        MAX_RETRIES + "), retrying in " + RETRY_DELAY_MS + "ms...");

                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Query interrupted", ie);
                    }
                }
            }
            throw new RuntimeException("Unexpected error in query execution");
        });
    }

    /**
     * Execute an update with retry logic
     */
    public CompletableFuture<Integer> executeUpdateAsync(String sql, Object... params) {
        return executeQueryAsync(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                return stmt.executeUpdate();
            }
        });
    }

    /**
     * Test database connection
     */
    public boolean testConnection() {
        if (!connected || dataSource == null) {
            return false;
        }

        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Database connection test failed", e);
            return false;
        }
    }

    /**
     * Close database connection pool
     */
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed");
        }
        connected = false;
    }

    /**
     * Check if database is connected
     */
    public boolean isConnected() {
        return connected && dataSource != null && !dataSource.isClosed();
    }

    /**
     * Get connection pool statistics
     */
    public String getPoolStats() {
        if (dataSource == null) {
            return "Database not initialized";
        }

        return String.format(
            "Pool Stats: Active=%d, Idle=%d, Waiting=%d, Total=%d",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
            dataSource.getHikariPoolMXBean().getTotalConnections()
        );
    }

    /**
     * Functional interface for database queries
     */
    @FunctionalInterface
    public interface DatabaseQuery<T> {
        T execute(Connection connection) throws SQLException;
    }
}
