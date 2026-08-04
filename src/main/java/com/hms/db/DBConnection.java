package com.hms.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection utility class.
 * Updated for Java 21 / Jakarta EE 10 compatibility:
 * - Removed redundant Class.forName() call (JDBC 4.0+ auto-loads drivers via ServiceLoader)
 * - Replaced static shared Connection field with per-call connection to avoid thread-safety issues
 * - Added explicit SQLException handling for better diagnostics
 */
public class DBConnection {

	/**
	 * Creates and returns a new database connection on each call.
	 * Note: In production, replace with a connection pool (e.g., HikariCP).
	 *
	 * @return a new {@link Connection} to the hospital database, or {@code null} on failure
	 */
	public static Connection getConn() {

		Connection conn = null;

		try {
			// JDBC 4.0+ (Java 6+): Driver is auto-registered via ServiceLoader;
			// Class.forName("com.mysql.cj.jdbc.Driver") is no longer required.
			conn = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/hospital", "root", "wasim");

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return conn;
	}
}
