package com.hms.entity;

import java.io.Serializable;

import com.hms.util.PasswordEncryptionUtil;

/**
 * Doctor entity representing a doctor in the Hospital Management System.
 * 
 * SECURITY NOTE: Passwords should be encrypted before storage using PasswordEncryptionUtil.
 * The encryption keys are securely managed in AWS Secrets Manager, not hardcoded.
 * 
 * Usage:
 * - When creating/updating doctors, encrypt passwords: doctor.setPassword(PasswordEncryptionUtil.encryptPassword(plainPassword))
 * - When verifying passwords, decrypt: PasswordEncryptionUtil.decryptPassword(doctor.getPassword())
 * - For better security, consider using hashPassword() for one-way hashing instead of encryption
 */
public class Doctor implements Serializable {
	
	/**
	 * Serial version UID for Redis session serialization compatibility
	 */
	private static final long serialVersionUID = 1L;
	
	private int id;
	private String fullName;
	private String dateOfBirth;
	private String qualification;
	private String specialist;
	private String email;
	private String phone;
	// Password stored in encrypted form using AWS Secrets Manager-backed encryption
	// Never store plain text passwords - use PasswordEncryptionUtil for encryption/decryption
	private String password; // Encrypted password
	
	
	public Doctor() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Doctor(String fullName, String dateOfBirth, String qualification, String specialist, String email,
			String phone, String password) {
		super();
		this.fullName = fullName;
		this.dateOfBirth = dateOfBirth;
		this.qualification = qualification;
		this.specialist = specialist;
		this.email = email;
		this.phone = phone;
		this.password = password;
	}
	
	
	


	public Doctor(int id, String fullName, String dateOfBirth, String qualification, String specialist, String email,
			String phone, String password) {
		super();
		this.id = id;
		this.fullName = fullName;
		this.dateOfBirth = dateOfBirth;
		this.qualification = qualification;
		this.specialist = specialist;
		this.email = email;
		this.phone = phone;
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


	public String getDateOfBirth() {
		return dateOfBirth;
	}


	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}


	public String getQualification() {
		return qualification;
	}


	public void setQualification(String qualification) {
		this.qualification = qualification;
	}


	public String getSpecialist() {
		return specialist;
	}


	public void setSpecialist(String specialist) {
		this.specialist = specialist;
	}


	public String getEmail() {
		return email;
	}


	public void setEmail(String email) {
		this.email = email;
	}


	public String getPhone() {
		return phone;
	}


	public void setPhone(String phone) {
		this.phone = phone;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
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
