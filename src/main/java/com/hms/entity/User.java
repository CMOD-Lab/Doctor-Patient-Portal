package com.hms.entity;

import java.io.Serializable;

import com.hms.util.PasswordEncryptionUtil;

/**
 * User entity representing a user in the Hospital Management System.
 * 
 * SECURITY NOTE: Passwords should be encrypted before storage using PasswordEncryptionUtil.
 * The encryption keys are securely managed in AWS Secrets Manager, not hardcoded.
 * 
 * Usage:
 * - When creating/updating users, encrypt passwords: user.setPassword(PasswordEncryptionUtil.encryptPassword(plainPassword))
 * - When verifying passwords, decrypt: PasswordEncryptionUtil.decryptPassword(user.getPassword())
 * - For better security, consider using hashPassword() for one-way hashing instead of encryption
 */
public class User implements Serializable {
	
	/**
	 * Serial version UID for Redis session serialization compatibility
	 */
	private static final long serialVersionUID = 1L;
	
	private int id;
	private String fullName;
	private String email;
	// Password stored in encrypted form using AWS Secrets Manager-backed encryption
	// Never store plain text passwords - use PasswordEncryptionUtil for encryption/decryption
	private String password; // Encrypted password
	
	
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


	@Override
	public String toString() {
		return "User [id=" + id + ", fullName=" + fullName + ", email=" + email + "]";
	}
	
	/**
	 * Sets the password in encrypted form.
	 * This method encrypts the plain text password using AWS Secrets Manager-backed encryption.
	 * 
	 * @param plainPassword The plain text password to encrypt and store
	 */
	public void setEncryptedPassword(String plainPassword) {
		if (plainPassword != null && !plainPassword.isEmpty()) {
			this.password = PasswordEncryptionUtil.encryptPassword(plainPassword);
		}
	}
	
	/**
	 * Gets the decrypted password.
	 * This method decrypts the stored encrypted password.
	 * 
	 * @return The decrypted plain text password
	 */
	public String getDecryptedPassword() {
		if (this.password != null && !this.password.isEmpty()) {
			return PasswordEncryptionUtil.decryptPassword(this.password);
		}
		return null;
	}
	
	/**
	 * Verifies if the provided plain text password matches the stored encrypted password.
	 * 
	 * @param plainPassword The plain text password to verify
	 * @return true if passwords match, false otherwise
	 */
	public boolean verifyPassword(String plainPassword) {
		if (plainPassword == null || this.password == null) {
			return false;
		}
		try {
			String decrypted = PasswordEncryptionUtil.decryptPassword(this.password);
			return plainPassword.equals(decrypted);
		} catch (Exception e) {
			return false;
		}
	}
}
