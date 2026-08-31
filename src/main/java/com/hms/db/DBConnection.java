package com.hms.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database Connection Manager using HikariCP connection pooling.
 * This implementation provides efficient connection pooling for cloud environments,
 * compatible with AWS RDS and RDS Proxy.
 * 
 * Configuration is loaded from application.properties and can be overridden
 * via environment variables for cloud deployment.
 */
public class DBConnection {

	private static HikariDataSource dataSource;
	
	static {
		try {
			initializeDataSource();
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize HikariCP DataSource", e);
		}
	}
	
	/**
	 * Initialize HikariCP DataSource with configuration from application.properties
	 */
	private static void initializeDataSource() {
		Properties props = loadProperties();
		
		HikariConfig config = new HikariConfig();
		
		// Database connection settings
		config.setJdbcUrl(props.getProperty("db.url", "jdbc:mysql://localhost:3306/hospital"));
		config.setUsername(props.getProperty("db.username", "root"));
		config.setPassword(props.getProperty("db.password", "wasim"));
		config.setDriverClassName(props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
		
		// HikariCP pool settings
		config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.hikari.maximumPoolSize", "10")));
		config.setMinimumIdle(Integer.parseInt(props.getProperty("db.hikari.minimumIdle", "5")));
		config.setConnectionTimeout(Long.parseLong(props.getProperty("db.hikari.connectionTimeout", "30000")));
		config.setIdleTimeout(Long.parseLong(props.getProperty("db.hikari.idleTimeout", "600000")));
		config.setMaxLifetime(Long.parseLong(props.getProperty("db.hikari.maxLifetime", "1800000")));
		config.setAutoCommit(Boolean.parseBoolean(props.getProperty("db.hikari.autoCommit", "true")));
		
		// Additional HikariCP optimizations for cloud environments
		config.setConnectionTestQuery("SELECT 1");
		config.setPoolName("HMSConnectionPool");
		config.setLeakDetectionThreshold(60000); // 60 seconds
		
		// MySQL-specific optimizations
		config.addDataSourceProperty("cachePrepStmts", "true");
		config.addDataSourceProperty("prepStmtCacheSize", "250");
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		config.addDataSourceProperty("useServerPrepStmts", "true");
		config.addDataSourceProperty("useLocalSessionState", "true");
		config.addDataSourceProperty("rewriteBatchedStatements", "true");
		config.addDataSourceProperty("cacheResultSetMetadata", "true");
		config.addDataSourceProperty("cacheServerConfiguration", "true");
		config.addDataSourceProperty("elideSetAutoCommits", "true");
		config.addDataSourceProperty("maintainTimeStats", "false");
		
		// Network timeout configurations for cloud environments (AWS RDS)
		// Prevents indefinite hangs on network failures
		config.addDataSourceProperty("connectTimeout", props.getProperty("db.connectTimeout", "10000")); // 10 seconds
		config.addDataSourceProperty("socketTimeout", props.getProperty("db.socketTimeout", "30000")); // 30 seconds
		config.addDataSourceProperty("tcpKeepAlive", "true");
		
		dataSource = new HikariDataSource(config);
	}
	
	/**
	 * Load properties from application.properties file.
	 * Environment variables take precedence over properties file values.
	 */
	private static Properties loadProperties() {
		Properties props = new Properties();
		
		// Load from application.properties
		try (InputStream input = DBConnection.class.getClassLoader()
				.getResourceAsStream("application.properties")) {
			if (input != null) {
				props.load(input);
			}
		} catch (IOException e) {
			System.err.println("Warning: Could not load application.properties, using defaults");
		}
		
		// Override with environment variables if present
		overrideWithEnvVar(props, "db.url", "DB_URL");
		overrideWithEnvVar(props, "db.username", "DB_USERNAME");
		overrideWithEnvVar(props, "db.password", "DB_PASSWORD");
		overrideWithEnvVar(props, "db.driver", "DB_DRIVER");
		overrideWithEnvVar(props, "db.hikari.maximumPoolSize", "DB_POOL_MAX_SIZE");
		overrideWithEnvVar(props, "db.hikari.minimumIdle", "DB_POOL_MIN_IDLE");
		overrideWithEnvVar(props, "db.hikari.connectionTimeout", "DB_CONNECTION_TIMEOUT");
		overrideWithEnvVar(props, "db.hikari.idleTimeout", "DB_IDLE_TIMEOUT");
		overrideWithEnvVar(props, "db.hikari.maxLifetime", "DB_MAX_LIFETIME");
		overrideWithEnvVar(props, "db.hikari.autoCommit", "DB_AUTO_COMMIT");
		overrideWithEnvVar(props, "db.connectTimeout", "DB_CONNECT_TIMEOUT");
		overrideWithEnvVar(props, "db.socketTimeout", "DB_SOCKET_TIMEOUT");
		
		return props;
	}
	
	/**
	 * Override property value with environment variable if present
	 */
	private static void overrideWithEnvVar(Properties props, String propKey, String envKey) {
		String envValue = System.getenv(envKey);
		if (envValue != null && !envValue.isEmpty()) {
			props.setProperty(propKey, envValue);
		}
	}
	
	/**
	 * Get a connection from the HikariCP connection pool.
	 * This method is backward compatible with the original getConn() method.
	 * 
	 * @return Connection from the pool
	 */
	public static Connection getConn() {
		try {
			if (dataSource == null) {
				initializeDataSource();
			}
			return dataSource.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to get database connection from pool", e);
		}
	}
	
	/**
	 * Get the HikariDataSource instance.
	 * Useful for advanced operations or monitoring.
	 * 
	 * @return HikariDataSource instance
	 */
	public static HikariDataSource getDataSource() {
		if (dataSource == null) {
			initializeDataSource();
		}
		return dataSource;
	}
	
	/**
	 * Close the connection pool.
	 * Should be called during application shutdown.
	 */
	public static void closePool() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
	}
}
