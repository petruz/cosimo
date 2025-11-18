// src/main/java/com/debug/queryapp/controller/DatabaseController.java

package com.debug.queryapp.controller;

import com.debug.queryapp.connection.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * REST controller for database connection management.
 * Handles connection creation, testing, and management.
 */
@RestController
@RequestMapping("/api/v1/database")
@CrossOrigin(origins = "*")
public class DatabaseController {
    private static final Logger LOGGER = Logger.getLogger(DatabaseController.class.getName());

    /**
     * Test and establish a new database connection.
     *
     * POST /api/v1/database/connect
     * Body: {
     *   "databaseType": "PostgreSQL" or "ClickHouse",
     *   "host": "localhost",
     *   "port": 5432,
     *   "database": "mydb",
     *   "username": "user",
     *   "password": "pass"
     * }
     */
    @PostMapping("/connect")
    public ResponseEntity<?> connectDatabase(@RequestBody ConnectionRequest request) {
        try {
            LOGGER.info("Creating new " + request.getDatabaseType() + " connection...");

            // Use Factory to create the appropriate connection type
            DatabaseConnection connection = ConnectionFactory.createConnection(
                    request.getDatabaseType(),
                    request.getHost(),
                    request.getPort(),
                    request.getDatabase(),
                    request.getUsername(),
                    request.getPassword()
            );

            // Test the connection
            if (!connection.testConnection()) {
                return ResponseEntity.status(400).body(new ErrorResponse(
                        "Connection test failed",
                        "Unable to establish connection to " + request.getDatabaseType(),
                        false
                ));
            }

            // Add to connection manager
            String connectionId = ConnectionManager.getInstance().addConnection(connection);

            return ResponseEntity.ok(new ConnectResponse(
                    true,
                    connectionId,
                    connection.getDatabaseType(),
                    connection.getJdbcUrl()
            ));

        } catch (Exception e) {
            LOGGER.severe("Error connecting to database: " + e.getMessage());
            return ResponseEntity.status(500).body(new ErrorResponse(
                    "Connection failed",
                    e.getMessage(),
                    false
            ));
        }
    }

    /**
     * Get current connection metrics.
     *
     * GET /api/v1/database/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        try {
            DatabaseConnection conn = ConnectionManager.getInstance().getCurrentConnection();
            return ResponseEntity.ok(conn.getConnectionMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new ErrorResponse(
                    "No active connection",
                    e.getMessage(),
                    false
            ));
        }
    }

    /**
     * List all active connections.
     *
     * GET /api/v1/database/connections
     */
    @GetMapping("/connections")
    public ResponseEntity<?> listConnections() {
        try {
            ConnectionManager manager = ConnectionManager.getInstance();
            return ResponseEntity.ok(Map.of(
                    "connections", manager.getConnectionSummaries(),
                    "currentConnectionId", manager.getCurrentConnectionId(),
                    "connectionCount", manager.getConnectionCount()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse(
                    "Error listing connections",
                    e.getMessage(),
                    false
            ));
        }
    }

    /**
     * Set the current connection.
     *
     * POST /api/v1/database/select/{connectionId}
     */
    @PostMapping("/select/{connectionId}")
    public ResponseEntity<?> selectConnection(@PathVariable String connectionId) {
        try {
            ConnectionManager.getInstance().setCurrentConnection(connectionId);
            DatabaseConnection conn = ConnectionManager.getInstance().getCurrentConnection();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Connection activated",
                    "connectionId", connectionId,
                    "databaseType", conn.getDatabaseType()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new ErrorResponse(
                    "Error selecting connection",
                    e.getMessage(),
                    false
            ));
        }
    }

    /**
     * Close a specific connection.
     *
     * POST /api/v1/database/disconnect/{connectionId}
     */
    @PostMapping("/disconnect/{connectionId}")
    public ResponseEntity<?> disconnectConnection(@PathVariable String connectionId) {
        try {
            ConnectionManager.getInstance().closeConnection(connectionId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Connection closed"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse(
                    "Error closing connection",
                    e.getMessage(),
                    false
            ));
        }
    }

    /**
     * Close all connections.
     *
     * POST /api/v1/database/disconnect-all
     */
    @PostMapping("/disconnect-all")
    public ResponseEntity<?> disconnectAllConnections() {
        try {
            ConnectionManager.getInstance().closeAllConnections();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "All connections closed"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse(
                    "Error closing connections",
                    e.getMessage(),
                    false
            ));
        }
    }
}

/**
 * Request body for database connection.
 */
class ConnectionRequest {
    private String databaseType;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    // Getters and Setters
    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

/**
 * Response for successful connection.
 */
class ConnectResponse {
    private boolean success;
    private String connectionId;
    private String databaseType;
    private String jdbcUrl;

    public ConnectResponse(boolean success, String connectionId, String databaseType, String jdbcUrl) {
        this.success = success;
        this.connectionId = connectionId;
        this.databaseType = databaseType;
        this.jdbcUrl = jdbcUrl;
    }

    public boolean isSuccess() { return success; }
    public String getConnectionId() { return connectionId; }
    public String getDatabaseType() { return databaseType; }
    public String getJdbcUrl() { return jdbcUrl; }
}

/**
 * Error response format.
 */
class ErrorResponse {
    private String error;
    private String message;
    private boolean success;

    public ErrorResponse(String error, String message, boolean success) {
        this.error = error;
        this.message = message;
        this.success = success;
    }

    public String getError() { return error; }
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
}
