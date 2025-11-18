// src/main/java/com/debug/queryapp/connection/DatabaseConnection.java

package com.debug.queryapp.connection;

import java.sql.ResultSet;
import java.util.Map;

/**
 * Interface for database connections.
 * Implementations handle PostgreSQL and ClickHouse specific logic.
 *
 * All queries are executed WITHOUT MODIFICATION using the proprietary JDBC drivers.
 */
public interface DatabaseConnection {

    /**
     * Execute a SELECT query without any modifications.
     * Query is passed directly to the JDBC driver.
     *
     * @param sql The SQL query string
     * @return ResultSet containing the query results
     * @throws Exception if query execution fails
     */
    ResultSet executeQuery(String sql) throws Exception;

    /**
     * Execute an INSERT, UPDATE, or DELETE statement without modifications.
     *
     * @param sql The SQL statement
     * @return Number of rows affected
     * @throws Exception if execution fails
     */
    int executeUpdate(String sql) throws Exception;

    /**
     * Execute EXPLAIN ANALYZE (PostgreSQL) or EXPLAIN (ClickHouse) query.
     * Database-specific implementation.
     *
     * @param sql The SQL query to analyze
     * @return ResultSet containing the execution plan
     * @throws Exception if execution fails
     */
    ResultSet explainQuery(String sql) throws Exception;

    /**
     * Test the connection to the database.
     *
     * @return true if connection is successful
     * @throws Exception if connection test fails
     */
    boolean testConnection() throws Exception;

    /**
     * Get connection pool metrics and statistics.
     *
     * @return Map containing pool metrics (active connections, idle connections, etc.)
     */
    Map<String, Object> getConnectionMetrics();

    /**
     * Close the database connection and release resources.
     *
     * @throws Exception if close operation fails
     */
    void close() throws Exception;

    /**
     * Get the type of this database connection.
     *
     * @return Database type name (e.g., "PostgreSQL", "ClickHouse")
     */
    String getDatabaseType();

    /**
     * Get the JDBC URL used for this connection.
     *
     * @return The JDBC connection URL
     */
    String getJdbcUrl();
}
