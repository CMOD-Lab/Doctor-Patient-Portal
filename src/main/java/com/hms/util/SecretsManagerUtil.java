package com.hms.util;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for retrieving secrets from AWS Secrets Manager.
 * This replaces hardcoded credentials and API keys with secure, centralized secret management.
 * 
 * Configuration:
 * - Set AWS_REGION environment variable (default: us-east-1)
 * - Ensure AWS credentials are configured via IAM role, environment variables, or AWS credentials file
 * - Store secrets in AWS Secrets Manager with appropriate secret names
 */
public class SecretsManagerUtil {
    
    private static final Logger LOGGER = Logger.getLogger(SecretsManagerUtil.class.getName());
    private static final String DEFAULT_REGION = "us-east-1";
    private static SecretsManagerClient secretsClient;
    private static final Map<String, String> secretCache = new HashMap<>();
    
    static {
        try {
            String region = System.getenv("AWS_REGION");
            if (region == null || region.isEmpty()) {
                region = DEFAULT_REGION;
                LOGGER.warning("AWS_REGION not set, using default: " + DEFAULT_REGION);
            }
            
            secretsClient = SecretsManagerClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
                    
            LOGGER.info("AWS Secrets Manager client initialized for region: " + region);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize AWS Secrets Manager client", e);
        }
    }
    
    /**
     * Retrieves a secret value from AWS Secrets Manager.
     * Results are cached to minimize API calls.
     * 
     * @param secretName The name of the secret in AWS Secrets Manager
     * @return The secret value as a string
     * @throws RuntimeException if the secret cannot be retrieved
     */
    public static String getSecret(String secretName) {
        if (secretCache.containsKey(secretName)) {
            return secretCache.get(secretName);
        }
        
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            
            GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest);
            String secret = valueResponse.secretString();
            
            // Cache the secret
            secretCache.put(secretName, secret);
            
            LOGGER.info("Successfully retrieved secret: " + secretName);
            return secret;
            
        } catch (SecretsManagerException e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve secret: " + secretName, e);
            throw new RuntimeException("Failed to retrieve secret from AWS Secrets Manager: " + secretName, e);
        }
    }
    
    /**
     * Retrieves a secret and parses it as JSON to extract a specific key.
     * Useful for secrets stored as JSON objects with multiple key-value pairs.
     * 
     * @param secretName The name of the secret in AWS Secrets Manager
     * @param key The JSON key to extract from the secret
     * @return The value associated with the key
     * @throws RuntimeException if the secret cannot be retrieved or parsed
     */
    public static String getSecretValue(String secretName, String key) {
        String cacheKey = secretName + ":" + key;
        if (secretCache.containsKey(cacheKey)) {
            return secretCache.get(cacheKey);
        }
        
        try {
            String secret = getSecret(secretName);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(secret);
            
            if (jsonNode.has(key)) {
                String value = jsonNode.get(key).asText();
                secretCache.put(cacheKey, value);
                return value;
            } else {
                throw new RuntimeException("Key '" + key + "' not found in secret: " + secretName);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse secret JSON: " + secretName, e);
            throw new RuntimeException("Failed to parse secret JSON: " + secretName, e);
        }
    }
    
    /**
     * Clears the secret cache. Use this if secrets are rotated and need to be refreshed.
     */
    public static void clearCache() {
        secretCache.clear();
        LOGGER.info("Secret cache cleared");
    }
    
    /**
     * Closes the Secrets Manager client. Call this during application shutdown.
     */
    public static void close() {
        if (secretsClient != null) {
            secretsClient.close();
            LOGGER.info("AWS Secrets Manager client closed");
        }
    }
}
