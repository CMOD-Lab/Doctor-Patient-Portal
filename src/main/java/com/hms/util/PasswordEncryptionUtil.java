package com.hms.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for password encryption using keys stored in AWS Secrets Manager.
 * This ensures encryption keys are never hardcoded in source code.
 * 
 * Configuration:
 * - Store encryption key in AWS Secrets Manager with secret name: "hms/encryption/password-key"
 * - The secret should contain a JSON object with key "encryptionKey"
 * - Example: {"encryptionKey": "your-base64-encoded-key"}
 */
public class PasswordEncryptionUtil {
    
    private static final Logger LOGGER = Logger.getLogger(PasswordEncryptionUtil.class.getName());
    private static final String SECRET_NAME = "hms/encryption/password-key";
    private static final String SECRET_KEY_FIELD = "encryptionKey";
    private static final String ALGORITHM = "AES";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    
    private static SecretKey secretKey;
    
    static {
        try {
            initializeEncryptionKey();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize encryption key from AWS Secrets Manager", e);
        }
    }
    
    /**
     * Initializes the encryption key from AWS Secrets Manager.
     * Falls back to environment variable if Secrets Manager is unavailable (for local development).
     */
    private static void initializeEncryptionKey() {
        try {
            // Try to get encryption key from AWS Secrets Manager
            String encryptionKeyBase64 = SecretsManagerUtil.getSecretValue(SECRET_NAME, SECRET_KEY_FIELD);
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
            secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            LOGGER.info("Encryption key successfully loaded from AWS Secrets Manager");
        } catch (Exception e) {
            LOGGER.warning("Failed to load encryption key from AWS Secrets Manager, trying environment variable");
            
            // Fallback to environment variable for local development
            String envKey = System.getenv("HMS_ENCRYPTION_KEY");
            if (envKey != null && !envKey.isEmpty()) {
                try {
                    byte[] keyBytes = Base64.getDecoder().decode(envKey);
                    secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
                    LOGGER.info("Encryption key loaded from environment variable");
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to load encryption key from environment variable", ex);
                    throw new RuntimeException("No valid encryption key available", ex);
                }
            } else {
                throw new RuntimeException("Encryption key not found in AWS Secrets Manager or environment variables");
            }
        }
    }
    
    /**
     * Encrypts a password using AES encryption with key from AWS Secrets Manager.
     * 
     * @param password The plain text password to encrypt
     * @return Base64-encoded encrypted password
     */
    public static String encryptPassword(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to encrypt password", e);
            throw new RuntimeException("Password encryption failed", e);
        }
    }
    
    /**
     * Decrypts an encrypted password.
     * 
     * @param encryptedPassword Base64-encoded encrypted password
     * @return Decrypted plain text password
     */
    public static String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return encryptedPassword;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to decrypt password", e);
            throw new RuntimeException("Password decryption failed", e);
        }
    }
    
    /**
     * Hashes a password using PBKDF2 with SHA-256.
     * This is recommended for password storage (one-way hashing).
     * 
     * @param password The plain text password to hash
     * @param salt The salt to use (should be unique per user)
     * @return Base64-encoded hashed password
     */
    public static String hashPassword(String password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to hash password", e);
            throw new RuntimeException("Password hashing failed", e);
        }
    }
    
    /**
     * Generates a random salt for password hashing.
     * 
     * @return Random salt bytes
     */
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }
    
    /**
     * Verifies a password against a stored hash.
     * 
     * @param password The plain text password to verify
     * @param storedHash The stored password hash
     * @param salt The salt used for hashing
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash, byte[] salt) {
        try {
            String computedHash = hashPassword(password, salt);
            return computedHash.equals(storedHash);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to verify password", e);
            return false;
        }
    }
    
    /**
     * Reloads the encryption key from AWS Secrets Manager.
     * Use this after key rotation.
     */
    public static void reloadEncryptionKey() {
        SecretsManagerUtil.clearCache();
        initializeEncryptionKey();
        LOGGER.info("Encryption key reloaded from AWS Secrets Manager");
    }
}
