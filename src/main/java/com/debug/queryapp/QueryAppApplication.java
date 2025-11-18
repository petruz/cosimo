// src/main/java/com/debug/queryapp/QueryAppApplication.java

package com.debug.queryapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import com.debug.queryapp.connection.DriverLoader;
import com.debug.queryapp.connection.ConnectionManager;
import java.util.logging.Logger;

/**
 * Main Spring Boot application class.
 * Initializes JDBC drivers and manages application lifecycle.
 */
@SpringBootApplication
public class QueryAppApplication {
    private static final Logger LOGGER = Logger.getLogger(QueryAppApplication.class.getName());

    /**
     * Application entry point.
     * Loads JDBC drivers BEFORE starting Spring Boot.
     */
    public static void main(String[] args) {
        LOGGER.info("=== Query Debug App Starting ===");
        LOGGER.info("Loading JDBC drivers...");

        // Load all JDBC drivers from jdbc-drivers/ folder
        // This must happen BEFORE Spring Boot initializes
        DriverLoader.loadAllDrivers();

        LOGGER.info("JDBC drivers loaded successfully");
        LOGGER.info("Starting Spring Boot application...");

        SpringApplication.run(QueryAppApplication.class, args);

        LOGGER.info("=== Application Started ===");
    }

    /**
     * Cleanup hook - called when application shuts down.
     * Closes all database connections gracefully.
     */
    @EventListener
    public void onApplicationEvent(ContextClosedEvent event) {
        LOGGER.info("Application shutting down...");
        try {
            ConnectionManager.getInstance().closeAllConnections();
            LOGGER.info("All connections closed");
        } catch (Exception e) {
            LOGGER.severe("Error closing connections: " + e.getMessage());
        }
        LOGGER.info("=== Application Stopped ===");
    }
}
