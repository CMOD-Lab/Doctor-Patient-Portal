package com.hms.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection utility class.
 * Updated for PostgreSQL 16 compatibility:
 * - Uses PostgreSQL JDBC URL (jdbc:postgresql://)
 * - Updated driver connection parameters for PostgreSQL
 * - JDBC 4.0+ auto-loads the PostgreSQL driver via ServiceLoader; no Class.forName() needed
 * - PostgreSQL default port: 5432
 * - Note: In production, replace with a connection pool (e.g., HikariCP).
 */
public class DBConnection {

	// PostgreSQL connection parameters
	private static final String DB_URL      = "jdbc:postgresql://localhost:5432/hospital";
	private static final String DB_USER     = "postgres";
	private static final String DB_PASSWORD = "wasim";

	/**
	 * Creates and returns a new database connection on each call.
	 * Connects to PostgreSQL 16 database.
	 *
	 * @return a new {@link Connection} to the hospital database, or {@code null} on failure
	 */
	public static Connection getConn() {

		Connection conn = null;

		try {
			// PostgreSQL JDBC URL format: jdbc:postgresql://<host>:<port>/<database>
			// Default PostgreSQL port is 5432
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return conn;
	}
}
