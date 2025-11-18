// src/main/java/com/debug/queryapp/controller/QueryController.java

package com.debug.queryapp.controller;

import com.debug.queryapp.connection.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Connection;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * REST controller for query execution.
 * Handles execution of SELECT, INSERT, UPDATE, DELETE queries.
 * Queries are executed WITHOUT MODIFICATION using the JDBC drivers.
 */
@RestController
@RequestMapping("/api/v1/query")
@CrossOrigin(origins = "*")
public class QueryController {
    private static final Logger LOGGER = Logger.getLogger(QueryController.class.getName());

    /**
     * Execute a SELECT query without modification.
     *
     * POST /api/v1/query/execute
     * Body: {
     *   "sql": "SELECT * FROM table WHERE id = 1",
     *   "connectionId": "optional-connection-id"
     * }
     */
    @PostMapping("/execute")
    public ResponseEntity<?> executeQuery(@RequestBody ExecuteQueryRequest request) {
        long startTime = System.currentTimeMillis();

        LOGGER.info("=== Query Execution Started ===");
        LOGGER.info("SQL: " + request.getSql());

        try {
            // Validate input
            if (request.getSql() == null || request.getSql().trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "SQL query is required",
                        "message", "Please provide a valid SQL query"
                ));
            }

            // Get the connection to use
            DatabaseConnection conn = getConnection(request.getConnectionId());
            LOGGER.info("Using connection: " + conn.getDatabaseType());

            // Execute the query WITHOUT MODIFICATION using JDBC driver
            ResultSet rs = conn.executeQuery(request.getSql());

            // Convert ResultSet to JSON - MUST be done before closing
            QueryResultSet resultSet = resultSetToJson(rs);

            // Now close resources in correct order: ResultSet -> Statement -> Connection
            Statement stmt = rs.getStatement();
            Connection jdbcConn = (stmt != null) ? stmt.getConnection() : null;

            rs.close();
            if (stmt != null) {
                stmt.close();
            }
            if (jdbcConn != null) {
                jdbcConn.close(); // Return connection to pool
            }

            long executionTime = System.currentTimeMillis() - startTime;

            LOGGER.info("Query executed successfully in " + executionTime + "ms");
            LOGGER.info("Rows returned: " + resultSet.getRowCount());
            LOGGER.info("Columns: " + resultSet.getColumns().size());
            LOGGER.info("Column names: " + resultSet.getColumns());
            if (resultSet.getRowCount() > 0) {
                LOGGER.info("First row sample: " + resultSet.getRows().get(0));
            }

            ExecuteQueryResponse response = new ExecuteQueryResponse(
                    true,
                    resultSet,
                    executionTime,
                    conn.getDatabaseType()
            );

            LOGGER.info("Response created - success: " + response.isSuccess());
            LOGGER.info("Response data: columns=" + response.getData().getColumns().size() +
                       ", rows=" + response.getData().getRows().size());

            // Log the response structure before returning
            LOGGER.info("Response structure check:");
            LOGGER.info("  - success type: " + response.isSuccess());
            LOGGER.info("  - data type: " + response.getData().getClass().getName());
            LOGGER.info("  - executionTimeMs type: " + response.getExecutionTimeMs());
            LOGGER.info("  - databaseType: " + response.getDatabaseType());

            ResponseEntity<ExecuteQueryResponse> responseEntity = ResponseEntity.ok(response);
            LOGGER.info("ResponseEntity created with status: " + responseEntity.getStatusCode());

            return responseEntity;        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            LOGGER.log(Level.SEVERE, "Error executing query", e);
            LOGGER.severe("Error details: " + e.getMessage());

            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Query execution failed",
                    "message", e.getMessage(),
                    "executionTimeMs", executionTime
            ));
        }
    }

    /**
     * Execute an INSERT, UPDATE, or DELETE statement without modification.
     *
     * POST /api/v1/query/update
     * Body: {
     *   "sql": "UPDATE table SET column = value WHERE id = 1",
     *   "connectionId": "optional-connection-id"
     * }
     */
    @PostMapping("/update")
    public ResponseEntity<?> executeUpdate(@RequestBody ExecuteQueryRequest request) {
        long startTime = System.currentTimeMillis();

        LOGGER.info("=== Update Execution Started ===");
        LOGGER.info("SQL: " + request.getSql());

        try {
            // Validate input
            if (request.getSql() == null || request.getSql().trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "SQL statement is required"
                ));
            }

            DatabaseConnection conn = getConnection(request.getConnectionId());
            LOGGER.info("Using connection: " + conn.getDatabaseType());

            // Execute the update WITHOUT MODIFICATION
            int rowsAffected = conn.executeUpdate(request.getSql());

            long executionTime = System.currentTimeMillis() - startTime;

            LOGGER.info("Update executed successfully in " + executionTime + "ms");
            LOGGER.info("Rows affected: " + rowsAffected);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "rowsAffected", rowsAffected,
                    "executionTimeMs", executionTime,
                    "databaseType", conn.getDatabaseType()
            ));

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            LOGGER.log(Level.SEVERE, "Error executing update", e);

            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Update execution failed",
                    "message", e.getMessage(),
                    "executionTimeMs", executionTime
            ));
        }
    }

    /**
     * Execute EXPLAIN query for query optimization.
     * PostgreSQL: EXPLAIN ANALYZE
     * ClickHouse: EXPLAIN
     *
     * POST /api/v1/query/explain
     * Body: {
     *   "sql": "SELECT * FROM table",
     *   "connectionId": "optional-connection-id"
     * }
     */
    @PostMapping("/explain")
    public ResponseEntity<?> explainQuery(@RequestBody ExecuteQueryRequest request) {
        long startTime = System.currentTimeMillis();

        LOGGER.info("=== EXPLAIN Execution Started ===");
        LOGGER.info("SQL: " + request.getSql());

        try {
            // Validate input
            if (request.getSql() == null || request.getSql().trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of(
                        "success", false,
                        "error", "SQL query is required"
                ));
            }

            DatabaseConnection conn = getConnection(request.getConnectionId());
            LOGGER.info("Using connection: " + conn.getDatabaseType());

            // Execute EXPLAIN using the appropriate database syntax
            ResultSet rs = conn.explainQuery(request.getSql());

            QueryResultSet resultSet = resultSetToJson(rs);

            // Now close resources in correct order: ResultSet -> Statement -> Connection
            Statement stmt = rs.getStatement();
            Connection jdbcConn = (stmt != null) ? stmt.getConnection() : null;

            rs.close();
            if (stmt != null) {
                stmt.close();
            }
            if (jdbcConn != null) {
                jdbcConn.close(); // Return connection to pool
            }

            long executionTime = System.currentTimeMillis() - startTime;

            LOGGER.info("EXPLAIN executed successfully in " + executionTime + "ms");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", resultSet,
                    "executionTimeMs", executionTime,
                    "databaseType", conn.getDatabaseType(),
                    "queryType", conn.getDatabaseType().equals("PostgreSQL") ? "EXPLAIN ANALYZE" : "EXPLAIN"
            ));

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            LOGGER.log(Level.SEVERE, "Error executing EXPLAIN", e);

            return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "EXPLAIN execution failed",
                    "message", e.getMessage(),
                    "executionTimeMs", executionTime
            ));
        }
    }

    /**
     * Get the database connection to use.
     * If no connectionId is provided, uses the current connection.
     */
    private DatabaseConnection getConnection(String connectionId) throws Exception {
        ConnectionManager manager = ConnectionManager.getInstance();

        if (connectionId != null && !connectionId.isEmpty()) {
            DatabaseConnection conn = manager.getConnection(connectionId);
            if (conn == null) {
                throw new Exception("Connection not found: " + connectionId);
            }
            return conn;
        }

        return manager.getCurrentConnection();
    }

    /**
     * Convert a JDBC ResultSet to a JSON-compatible format.
     * Preserves data types from the database.
     * IMPORTANT: This must be called BEFORE closing the ResultSet!
     */
    private QueryResultSet resultSetToJson(ResultSet rs) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();

        try {
            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();

            LOGGER.info("Processing ResultSet with " + columnCount + " columns");

            // Extract column names and types
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metadata.getColumnName(i);
                columnNames.add(columnName);
                LOGGER.fine("Column " + i + ": " + columnName + " (Type: " + metadata.getColumnTypeName(i) + ")");
            }

            // Extract rows - read all data before closing
            int rowCount = 0;
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = columnNames.get(i - 1);
                    Object value = rs.getObject(i);

                    // Convert to JSON-friendly types
                    if (value != null) {
                        row.put(columnName, value.toString());
                    } else {
                        row.put(columnName, null);
                    }
                }
                rows.add(row);
                rowCount++;
            }

            LOGGER.info("Read " + rowCount + " rows from ResultSet");

            return new QueryResultSet(columnNames, rows, rows.size());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error converting ResultSet to JSON", e);
            throw new Exception("Failed to process query results: " + e.getMessage(), e);
        }
    }
}

/**
 * Request body for query execution.
 */
class ExecuteQueryRequest {
    private String sql;
    private String connectionId;

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }

    public String getConnectionId() { return connectionId; }
    public void setConnectionId(String connectionId) { this.connectionId = connectionId; }
}

/**
 * Response for successful query execution.
 */
class ExecuteQueryResponse {
    private boolean success;
    private QueryResultSet data;
    private long executionTimeMs;
    private String databaseType;

    public ExecuteQueryResponse(boolean success, QueryResultSet data, long executionTimeMs, String databaseType) {
        this.success = success;
        this.data = data;
        this.executionTimeMs = executionTimeMs;
        this.databaseType = databaseType;
    }

    public boolean isSuccess() { return success; }
    public QueryResultSet getData() { return data; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public String getDatabaseType() { return databaseType; }
}

/**
 * Result set data structure.
 */
class QueryResultSet {
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private int rowCount;

    public QueryResultSet(List<String> columns, List<Map<String, Object>> rows, int rowCount) {
        this.columns = columns;
        this.rows = rows;
        this.rowCount = rowCount;
    }

    public List<String> getColumns() { return columns; }
    public List<Map<String, Object>> getRows() { return rows; }
    public int getRowCount() { return rowCount; }
}
