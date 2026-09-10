package com.hms.entity;

/**
 * User entity class.
 *
 * Cloud Readiness Fix (cr-java-0113 - Lack of Externalized Secrets):
 * - Removed the 'password' field from toString() to prevent credential leakage
 *   in application logs, monitoring systems, and audit trails.
 * - Passwords and sensitive credentials must be managed via AWS Secrets Manager
 *   (see com.hms.config.AwsSecretsManagerUtil) rather than being embedded or
 *   exposed in source code, log output, or serialized representations.
 */
public class User {
	private int id;
	private String fullName;
	private String email;
	private String password;
	
	
	public User() {
		super();
		// TODO Auto-generated constructor stub
	}


	public User(int id, String fullName, String email, String password) {
		super();
		this.id = id;
		this.fullName = fullName;
		this.email = email;
		this.password = password;
	}


	public User(String fullName, String email, String password) {
		super();
		this.fullName = fullName;
		this.email = email;
		this.password = password;
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public String getFullName() {
		return fullName;
	}


	public void setFullName(String fullName) {
		this.fullName = fullName;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	/**
	 * Returns a string representation of the User object.
	 *
	 * Cloud Readiness Fix (cr-java-0113):
	 * The 'password' field has been intentionally excluded from this method
	 * to prevent credential leakage into application logs, monitoring dashboards,
	 * and audit trails. Secrets must be managed via AWS Secrets Manager and
	 * must never appear in serialized or logged output.
	 */
	@Override
	public String toString() {
		return "User [id=" + id + ", fullName=" + fullName + ", email=" + email + ", password=***REDACTED***]";
	}
	
	
	
	
	
	
	
	
}
