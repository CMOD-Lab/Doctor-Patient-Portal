package com.hms.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

	private static Connection conn;
	
	public static Connection getConn() {
		
		try {
			
			//step:1 for connection - load the driver class 
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			// step:2 - create a connection using environment variables for EKS service isolation
			// Replaces hardcoded "localhost:3306/hospital" with environment-driven configuration
			// to enforce Kubernetes NetworkPolicy-compatible service discovery
			String dbHost = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
			String dbPort = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "3306";
			String dbName = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "hospital";
			String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
			String dbPassword = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";
			
			String jdbcUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
			conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
			
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		
		return conn;
	}
}
