// src/main/java/com/debug/queryapp/connection/ConnectionManager.java

package com.debug.queryapp.connection;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Manages active database connections.
 *
 * Allows the user to have multiple connections open simultaneously.
 * Implements singleton pattern for application-wide access.
 */
public class ConnectionManager {
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());
    private static final ConnectionManager INSTANCE = new ConnectionManager();

    // Store all active connections with unique IDs
    private final Map<String, DatabaseConnection> activeConnections = new HashMap<>();

    // Currently selected connection
    private String currentConnectionId = null;

    /**
     * Private constructor for singleton pattern.
     */
    private ConnectionManager() {
    }

    /**
     * Get the singleton instance of ConnectionManager.
     *
     * @return The ConnectionManager instance
     */
    public static ConnectionManager getInstance() {
        return INSTANCE;
    }

    /**
     * Add a new connection to the manager.
     * The new connection becomes the current connection.
     *
     * @param connection The DatabaseConnection to add
     * @return Unique ID for this connection
     */
    public String addConnection(DatabaseConnection connection) {
        String connectionId = UUID.randomUUID().toString();
        activeConnections.put(connectionId, connection);
        currentConnectionId = connectionId;
        LOGGER.info("Connection added with ID: " + connectionId +
                " (" + connection.getDatabaseType() + ")");
        return connectionId;
    }

    /**
     * Get a specific connection by ID.
     *
     * @param connectionId The connection ID
     * @return The DatabaseConnection, or null if not found
     */
    public DatabaseConnection getConnection(String connectionId) {
        return activeConnections.get(connectionId);
    }

    /**
     * Get all active connections.
     *
     * @return Map of connection ID to DatabaseConnection
     */
    public Map<String, DatabaseConnection> getAllConnections() {
        return new HashMap<>(activeConnections);
    }

    /**
     * Get the currently selected connection.
     * This is the connection used for query execution by default.
     *
     * @return The current DatabaseConnection
     * @throws RuntimeException if no connection is active
     */
    public DatabaseConnection getCurrentConnection() {
        if (currentConnectionId == null) {
            throw new RuntimeException("No active connection. Please establish a connection first.");
        }
        DatabaseConnection conn = activeConnections.get(currentConnectionId);
        if (conn == null) {
            throw new RuntimeException("Current connection ID is invalid");
        }
        return conn;
    }

    /**
     * Set which connection is currently active.
     *
     * @param connectionId The ID of the connection to activate
     * @throws IllegalArgumentException if connection ID doesn't exist
     */
    public void setCurrentConnection(String connectionId) {
        if (!activeConnections.containsKey(connectionId)) {
            throw new IllegalArgumentException("Connection not found: " + connectionId);
        }
        DatabaseConnection conn = activeConnections.get(connectionId);
        currentConnectionId = connectionId;
        LOGGER.info("Current connection changed to: " + connectionId +
                " (" + conn.getDatabaseType() + ")");
    }

    /**
     * Check if there is a current active connection.
     *
     * @return true if a connection is active
     */
    public boolean hasCurrentConnection() {
        return currentConnectionId != null && activeConnections.containsKey(currentConnectionId);
    }

    /**
     * Get the ID of the current connection.
     *
     * @return Current connection ID, or null if none
     */
    public String getCurrentConnectionId() {
        return currentConnectionId;
    }

    /**
     * Close a specific connection and remove it from the manager.
     * If this was the current connection, the current connection is cleared.
     *
     * @param connectionId The ID of the connection to close
     * @throws Exception if close operation fails
     */
    public void closeConnection(String connectionId) throws Exception {
        DatabaseConnection conn = activeConnections.get(connectionId);
        if (conn != null) {
            try {
                conn.close();
                LOGGER.info("Connection closed: " + connectionId);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error closing connection: " + connectionId, e);
                throw e;
            } finally {
                activeConnections.remove(connectionId);
                if (connectionId.equals(currentConnectionId)) {
                    currentConnectionId = null;
                }
            }
        }
    }

    /**
     * Close all active connections.
     * The current connection is cleared.
     *
     * @throws Exception if any close operation fails
     */
    public void closeAllConnections() throws Exception {
        Exception lastException = null;

        for (String connectionId : new ArrayList<>(activeConnections.keySet())) {
            try {
                closeConnection(connectionId);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing connection: " + connectionId, e);
                lastException = e;
            }
        }

        currentConnectionId = null;
        LOGGER.info("All connections closed");

        if (lastException != null) {
            throw lastException;
        }
    }

    /**
     * Get the number of active connections.
     *
     * @return Number of active connections
     */
    public int getConnectionCount() {
        return activeConnections.size();
    }

    /**
     * Get summary information about all connections.
     * Useful for UI display.
     *
     * @return List of connection summaries
     */
    public List<Map<String, String>> getConnectionSummaries() {
        List<Map<String, String>> summaries = new ArrayList<>();

        for (Map.Entry<String, DatabaseConnection> entry : activeConnections.entrySet()) {
            Map<String, String> summary = new LinkedHashMap<>();
            try {
                DatabaseConnection conn = entry.getValue();
                summary.put("id", entry.getKey());
                summary.put("database", conn.getDatabaseType());
                summary.put("jdbcUrl", conn.getJdbcUrl());
                summary.put("isCurrent", entry.getKey().equals(currentConnectionId) ? "true" : "false");
            } catch (Exception e) {
                summary.put("error", e.getMessage());
            }
            summaries.add(summary);
        }

        return summaries;
    }
}
