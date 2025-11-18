// src/main/java/com/debug/queryapp/connection/DriverLoader.java

package com.debug.queryapp.connection;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Dynamically loads JDBC drivers from the jdbc-drivers/ folder and from classpath.
 *
 * This approach:
 * 1. First tries to load drivers from the classpath (Maven dependencies)
 * 2. Then tries to load additional drivers from the jdbc-drivers/ folder
 *
 * This hybrid approach works well because:
 * - ClickHouse has many dependencies (loaded from Maven)
 * - PostgreSQL can be loaded locally from JAR (optional)
 * - Provides flexibility for future databases
 */
public class DriverLoader {
    private static final Logger LOGGER = Logger.getLogger(DriverLoader.class.getName());
    private static final String DRIVERS_DIR = "jdbc-drivers";

    /**
     * Load all available JDBC drivers.
     * First from classpath (Maven dependencies), then from local JAR files.
     */
    public static void loadAllDrivers() {
        LOGGER.info("=== Starting JDBC Driver Loading ===");

        // First, load drivers from classpath (Maven dependencies)
        // This is automatic with DriverManager for drivers on the classpath
        LOGGER.info("Loading drivers from classpath (Maven dependencies)...");
        loadClasspathDrivers();

        // Second, load additional drivers from jdbc-drivers/ folder
        LOGGER.info("Loading drivers from local JAR files...");
        loadLocalDrivers();

        LOGGER.info("=== JDBC Driver Loading Completed ===");
    }

    /**
     * Load drivers from the classpath (Maven dependencies).
     * This happens automatically via ServiceLoader for drivers in pom.xml.
     *
     * We explicitly register known drivers to ensure they're loaded.
     */
    private static void loadClasspathDrivers() {
        // Known drivers in classpath
        String[] classpathDrivers = {
                "org.postgresql.Driver",
                "com.clickhouse.jdbc.ClickHouseDriver"
        };

        for (String driverClass : classpathDrivers) {
            try {
                Class<?> clazz = Class.forName(driverClass);
                LOGGER.info("Loaded driver from classpath: " + driverClass);
            } catch (ClassNotFoundException e) {
                LOGGER.info("Driver not in classpath: " + driverClass + " (will try local JAR)");
            } catch (Exception e) {
                LOGGER.warning("Error loading driver " + driverClass + ": " + e.getMessage());
            }
        }
    }

    /**
     * Load additional drivers from the jdbc-drivers/ folder.
     * This is for local JAR files not in Maven.
     */
    private static void loadLocalDrivers() {
        File driversDir = new File(DRIVERS_DIR);

        if (!driversDir.exists() || !driversDir.isDirectory()) {
            LOGGER.info("Local drivers directory not found: " + DRIVERS_DIR);
            return;
        }

        File[] jarFiles = driversDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            LOGGER.info("No .jar files found in: " + DRIVERS_DIR);
            return;
        }

        LOGGER.info("Found " + jarFiles.length + " local JDBC driver file(s)");

        for (File jar : jarFiles) {
            loadDriverFromJar(jar);
        }
    }

    /**
     * Load a single JDBC driver from a local JAR file using URLClassLoader.
     *
     * This is only used for drivers with all their dependencies included
     * in a single JAR file (e.g., PostgreSQL).
     *
     * For drivers with external dependencies (e.g., ClickHouse),
     * use Maven dependencies instead.
     */
    private static void loadDriverFromJar(File jarFile) {
        try {
            URL jarUrl = jarFile.toURI().toURL();

            // Create a ClassLoader with the JAR file as source
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarUrl},
                    Thread.currentThread().getContextClassLoader()
            );

            // Discover all Driver implementations using ServiceLoader
            var drivers = java.util.ServiceLoader.load(Driver.class, classLoader);

            boolean driverFound = false;
            for (Driver driver : drivers) {
                try {
                    // Register the driver with DriverManager using the Shim wrapper
                    DriverManager.registerDriver(new DriverShim(driver));
                    LOGGER.info("Loaded driver: " + driver.getClass().getName() +
                            " from " + jarFile.getName());
                    driverFound = true;
                } catch (Exception e) {
                    LOGGER.warning("Could not register driver from JAR: " + e.getMessage());
                }
            }

            if (!driverFound) {
                LOGGER.info("No JDBC drivers found in: " + jarFile.getName());
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Error loading driver from " + jarFile.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Driver Shim - Wrapper for dynamically loaded JDBC drivers.
     *
     * This allows the DriverManager to use drivers loaded from a URLClassLoader.
     */
    private static class DriverShim implements Driver {
        private final Driver wrappedDriver;

        public DriverShim(Driver driver) {
            this.wrappedDriver = driver;
        }

        @Override
        public boolean acceptsURL(String url) throws java.sql.SQLException {
            return wrappedDriver.acceptsURL(url);
        }

        @Override
        public java.sql.Connection connect(String url, java.util.Properties info)
                throws java.sql.SQLException {
            return wrappedDriver.connect(url, info);
        }

        @Override
        public int getMajorVersion() {
            return wrappedDriver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return wrappedDriver.getMinorVersion();
        }

        @Override
        public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info)
                throws java.sql.SQLException {
            return wrappedDriver.getPropertyInfo(url, info);
        }

        @Override
        public boolean jdbcCompliant() {
            return wrappedDriver.jdbcCompliant();
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
            return wrappedDriver.getParentLogger();
        }
    }
}
