// src/main/java/com/debug/queryapp/connection/ConnectionFactory.java

package com.debug.queryapp.connection;

import java.util.logging.Logger;

/**
 * Factory Pattern for creating database connections.
 *
 * This factory handles the instantiation of DatabaseConnection implementations
 * based on database type, ensuring consistent connection creation logic.
 *
 * Supports multiple formats for database type:
 * - "PostgreSQL" or "postgres" (case-insensitive)
 * - "ClickHouse" or "clickhouse" (case-insensitive)
 */
public class ConnectionFactory {
    private static final Logger LOGGER = Logger.getLogger(ConnectionFactory.class.getName());

    /**
     * Enum for supported database types.
     */
    public enum DatabaseType {
        POSTGRESQL("PostgreSQL", "postgres"),
        CLICKHOUSE("ClickHouse", "clickhouse");

        private final String displayName;
        private final String identifier;

        DatabaseType(String displayName, String identifier) {
            this.displayName = displayName;
            this.identifier = identifier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIdentifier() {
            return identifier;
        }

        /**
         * Convert string to DatabaseType enum.
         * Accepts multiple formats:
         * - Display name: "PostgreSQL", "ClickHouse"
         * - Identifier: "postgres", "clickhouse"
         * - Case-insensitive
         *
         * @param typeString The database type string
         * @return DatabaseType enum
         * @throws IllegalArgumentException if type is not recognized
         */
        public static DatabaseType fromString(String typeString) {
            if (typeString == null || typeString.trim().isEmpty()) {
                throw new IllegalArgumentException("Database type cannot be null or empty");
            }

            String normalized = typeString.trim().toLowerCase();

            for (DatabaseType type : DatabaseType.values()) {
                // Check against identifier (lowercase)
                if (type.identifier.equalsIgnoreCase(normalized)) {
                    return type;
                }
                // Check against display name (case-insensitive)
                if (type.displayName.equalsIgnoreCase(normalized)) {
                    return type;
                }
            }

            throw new IllegalArgumentException(
                    "Unknown database type: " + typeString +
                            ". Supported types: PostgreSQL/postgres, ClickHouse/clickhouse"
            );
        }

        /**
         * Convert string identifier to DatabaseType enum.
         * Case-insensitive.
         *
         * @param identifier The identifier string (e.g., "postgres", "clickhouse")
         * @return DatabaseType enum
         * @throws IllegalArgumentException if identifier is not recognized
         * @deprecated Use fromString() instead for more flexible parsing
         */
        @Deprecated
        public static DatabaseType fromIdentifier(String identifier) {
            return fromString(identifier);
        }

        /**
         * Convert display name to DatabaseType enum.
         * Case-insensitive.
         *
         * @param displayName The display name (e.g., "PostgreSQL", "ClickHouse")
         * @return DatabaseType enum
         * @throws IllegalArgumentException if display name is not recognized
         * @deprecated Use fromString() instead for more flexible parsing
         */
        @Deprecated
        public static DatabaseType fromDisplayName(String displayName) {
            return fromString(displayName);
        }
    }

    /**
     * Create a database connection based on the specified type.
     *
     * Accepts multiple database type formats:
     * - "PostgreSQL" or "postgres" (case-insensitive)
     * - "ClickHouse" or "clickhouse" (case-insensitive)
     *
     * @param dbType Database type (POSTGRESQL or CLICKHOUSE)
     * @param host Database host
     * @param port Database port
     * @param database Database name
     * @param username Username
     * @param password Password
     * @return DatabaseConnection instance
     * @throws IllegalArgumentException if database type is not supported
     */
    public static DatabaseConnection createConnection(DatabaseType dbType, String host, int port,
                                                      String database, String username,
                                                      String password) {
        switch (dbType) {
            case POSTGRESQL:
                LOGGER.info("Creating PostgreSQL connection to " + host + ":" + port);
                return new PostgresConnection(host, port, database, username, password);

            case CLICKHOUSE:
                LOGGER.info("Creating ClickHouse connection to " + host + ":" + port);
                return new ClickhouseConnection(host, port, database, username, password);

            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }

    /**
     * Create a database connection from string identifiers.
     * This version is useful for REST APIs and accepts flexible input formats.
     *
     * Examples of valid inputs:
     * - "PostgreSQL", "postgresql", "POSTGRESQL", "postgres", "Postgres"
     * - "ClickHouse", "clickhouse", "CLICKHOUSE"
     *
     * @param dbTypeString Database type as string (flexible format)
     * @param host Database host
     * @param port Database port
     * @param database Database name
     * @param username Username
     * @param password Password
     * @return DatabaseConnection instance
     * @throws IllegalArgumentException if database type is not recognized
     */
    public static DatabaseConnection createConnection(String dbTypeString, String host, int port,
                                                      String database, String username,
                                                      String password) {
        DatabaseType dbType = DatabaseType.fromString(dbTypeString);
        return createConnection(dbType, host, port, database, username, password);
    }
}
