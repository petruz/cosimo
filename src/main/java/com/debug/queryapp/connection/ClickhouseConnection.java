// src/main/java/com/debug/queryapp/connection/ClickhouseConnection.java

package com.debug.queryapp.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * ClickHouse database connection implementation.
 * Uses the ClickHouse JDBC driver directly with HikariCP connection pooling.
 * Queries are executed WITHOUT MODIFICATION.
 */
public class ClickhouseConnection implements DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(ClickhouseConnection.class.getName());

    private HikariDataSource dataSource;
    private String host;
    private int port;
    private String database;
    private String username;
    private String jdbcUrl;

    /**
     * Create a new ClickHouse connection.
     * Initializes HikariCP connection pool automatically.
     *
     * @param host ClickHouse host
     * @param port ClickHouse HTTP port (usually 8123)
     * @param database Database name
     * @param username ClickHouse username
     * @param password ClickHouse password
     */
    public ClickhouseConnection(String host, int port, String database,
                                String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;

        initializeDataSource(host, port, database, username, password);
    }

    /**
     * Initialize HikariCP connection pool for ClickHouse.
     */
    private void initializeDataSource(String host, int port, String database,
                                      String username, String password) {
        try {
            HikariConfig config = new HikariConfig();

            // Build JDBC URL for ClickHouse
            // ClickHouse JDBC driver uses HTTP protocol on port 8123 by default
            this.jdbcUrl = String.format("jdbc:clickhouse://%s:%d/%s",
                    host, port, database);

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            if (password != null && !password.isEmpty()) {
                config.setPassword(password);
            }

            // HikariCP Configuration (adjusted for ClickHouse)
            config.setMaximumPoolSize(15);
            config.setMinimumIdle(3);
            config.setConnectionTimeout(30000);        // 30 seconds
            config.setIdleTimeout(600000);             // 10 minutes
            config.setMaxLifetime(1800000);            // 30 minutes
            config.setAutoCommit(true);
            config.setPoolName("ClickHouse-Pool");

            // Specify the driver class
            config.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");

            // Connection initialization SQL
            config.setConnectionInitSql("SELECT 1");

            this.dataSource = new HikariDataSource(config);
            LOGGER.info("ClickHouse DataSource initialized: " + jdbcUrl);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing ClickHouse DataSource", e);
            throw new RuntimeException("Failed to initialize ClickHouse connection: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a SELECT query directly using ClickHouse JDBC driver.
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
            Statement stmt = conn.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY
            );

            // Execute query WITHOUT MODIFICATION
            LOGGER.info("Executing ClickHouse query: " + sql.substring(0, Math.min(50, sql.length())));
            return stmt.executeQuery(sql);

        } catch (SQLException e) {
            throw new Exception("ClickHouse query execution failed: " + e.getMessage(), e);
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
            LOGGER.info("Executing ClickHouse update: " + sql.substring(0, Math.min(50, sql.length())));
            return stmt.executeUpdate(sql);

        } catch (SQLException e) {
            throw new Exception("ClickHouse update execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute EXPLAIN query for ClickHouse query optimization.
     * ClickHouse uses EXPLAIN to show query processing pipeline.
     *
     * @param sql The query to analyze
     * @return ResultSet with query explanation
     * @throws Exception if execution fails
     */
    @Override
    public ResultSet explainQuery(String sql) throws Exception {
        try {
            Connection conn = dataSource.getConnection();

            // ClickHouse uses EXPLAIN syntax
            String explainSql = "EXPLAIN " + sql;

            Statement stmt = conn.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY
            );

            LOGGER.info("Executing EXPLAIN query");
            return stmt.executeQuery(explainSql);

        } catch (SQLException e) {
            throw new Exception("EXPLAIN execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test if connection to ClickHouse is active and working.
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
            LOGGER.info("ClickHouse connection test: " + (result ? "SUCCESS" : "FAILED"));
            return result;
        } catch (SQLException e) {
            throw new Exception("ClickHouse connection test failed: " + e.getMessage(), e);
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
                metrics.put("database", "ClickHouse");
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
            LOGGER.info("ClickHouse DataSource closed");
        }
    }

    @Override
    public String getDatabaseType() {
        return "ClickHouse";
    }

    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
