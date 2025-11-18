// src/main/java/com/debug/queryapp/connection/PostgresConnection.java

package com.debug.queryapp.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * PostgreSQL database connection implementation.
 * Uses the PostgreSQL JDBC driver directly with HikariCP connection pooling.
 * Queries are executed WITHOUT MODIFICATION.
 */
public class PostgresConnection implements DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(PostgresConnection.class.getName());

    private HikariDataSource dataSource;
    private String host;
    private int port;
    private String database;
    private String username;
    private String jdbcUrl;

    /**
     * Create a new PostgreSQL connection.
     * Initializes HikariCP connection pool automatically.
     *
     * @param host PostgreSQL host
     * @param port PostgreSQL port
     * @param database Database name
     * @param username Database username
     * @param password Database password
     */
    public PostgresConnection(String host, int port, String database,
                              String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;

        initializeDataSource(host, port, database, username, password);
    }

    /**
     * Initialize HikariCP connection pool for PostgreSQL.
     */
    private void initializeDataSource(String host, int port, String database,
                                      String username, String password) {
        try {
            HikariConfig config = new HikariConfig();

            // Build JDBC URL for PostgreSQL
            this.jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
                    host, port, database);

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);

            // HikariCP Configuration
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(20000);        // 20 seconds
            config.setIdleTimeout(300000);             // 5 minutes
            config.setMaxLifetime(1800000);            // 30 minutes
            config.setAutoCommit(true);
            config.setPoolName("PostgreSQL-Pool");

            // Specify the driver class
            config.setDriverClassName("org.postgresql.Driver");

            // Connection initialization SQL (optional)
            config.setConnectionInitSql("SELECT 1");

            this.dataSource = new HikariDataSource(config);
            LOGGER.info("PostgreSQL DataSource initialized: " + jdbcUrl);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing PostgreSQL DataSource", e);
            throw new RuntimeException("Failed to initialize PostgreSQL connection: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a SELECT query directly using PostgreSQL JDBC driver.
     * Query is NOT modified in any way.
     *
     * @param sql The exact SQL query to execute
     * @return ResultSet with query results
     * @throws Exception if execution fails
     */
    @Override
    public ResultSet executeQuery(String sql) throws Exception {
        try {
            Connection conn = dataSource.getConnection();

            // Create statement with scrollable ResultSet
            // This allows the frontend to access all rows
            Statement stmt = conn.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY
            );

            // Execute query WITHOUT MODIFICATION
            LOGGER.info("Executing PostgreSQL query: " + sql.substring(0, Math.min(50, sql.length())));
            return stmt.executeQuery(sql);

        } catch (SQLException e) {
            throw new Exception("PostgreSQL query execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute INSERT, UPDATE, or DELETE statement directly.
     * Query is NOT modified in any way.
     *
     * @param sql The exact SQL statement to execute
     * @return Number of rows affected
     * @throws Exception if execution fails
     */
    @Override
    public int executeUpdate(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Execute update WITHOUT MODIFICATION
            LOGGER.info("Executing PostgreSQL update: " + sql.substring(0, Math.min(50, sql.length())));
            return stmt.executeUpdate(sql);

        } catch (SQLException e) {
            throw new Exception("PostgreSQL update execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute EXPLAIN ANALYZE query for PostgreSQL query planning.
     * This helps identify slow queries and missing indexes.
     *
     * @param sql The query to analyze
     * @return ResultSet with execution plan
     * @throws Exception if execution fails
     */
    @Override
    public ResultSet explainQuery(String sql) throws Exception {
        try {
            Connection conn = dataSource.getConnection();

            // PostgreSQL uses EXPLAIN ANALYZE
            String explainSql = "EXPLAIN ANALYZE " + sql;

            Statement stmt = conn.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY
            );

            LOGGER.info("Executing EXPLAIN ANALYZE");
            return stmt.executeQuery(explainSql);

        } catch (SQLException e) {
            throw new Exception("EXPLAIN ANALYZE execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test if connection to PostgreSQL is active and working.
     *
     * @return true if connection is successful
     * @throws Exception if test fails
     */
    @Override
    public boolean testConnection() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            boolean result = rs.next();
            LOGGER.info("PostgreSQL connection test: " + (result ? "SUCCESS" : "FAILED"));
            return result;
        } catch (SQLException e) {
            throw new Exception("PostgreSQL connection test failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get HikariCP connection pool metrics.
     *
     * @return Map with pool statistics
     */
    @Override
    public Map<String, Object> getConnectionMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        if (dataSource != null) {
            try {
                metrics.put("database", "PostgreSQL");
                metrics.put("jdbcUrl", jdbcUrl);
                metrics.put("poolName", dataSource.getPoolName());
                metrics.put("maximumPoolSize", dataSource.getMaximumPoolSize());
                metrics.put("activeConnections", dataSource.getHikariPoolMXBean().getActiveConnections());
                metrics.put("idleConnections", dataSource.getHikariPoolMXBean().getIdleConnections());
                metrics.put("totalConnections", dataSource.getHikariPoolMXBean().getTotalConnections());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error retrieving pool metrics", e);
            }
        }
        return metrics;
    }

    /**
     * Close the connection pool and release all resources.
     *
     * @throws Exception if close operation fails
     */
    @Override
    public void close() throws Exception {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("PostgreSQL DataSource closed");
        }
    }

    @Override
    public String getDatabaseType() {
        return "PostgreSQL";
    }

    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
