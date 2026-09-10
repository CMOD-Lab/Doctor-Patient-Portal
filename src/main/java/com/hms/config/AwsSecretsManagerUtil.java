package com.hms.config;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.InvalidParameterException;
import com.amazonaws.services.secretsmanager.model.InvalidRequestException;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;

/**
 * AWS Secrets Manager Utility class.
 *
 * Cloud Readiness Fix (cr-java-0113 - Lack of Externalized Secrets):
 * Centralizes all secret retrieval through AWS Secrets Manager, replacing
 * hardcoded credentials, API keys, and sensitive configuration values
 * embedded in source code or property files.
 *
 * Usage:
 *   String dbPassword = AwsSecretsManagerUtil.getSecret("hms/db/password");
 *   String apiKey     = AwsSecretsManagerUtil.getSecret("hms/api/key");
 *
 * Secrets should be stored in AWS Secrets Manager under a structured naming
 * convention (e.g., "hms/<environment>/<secret-name>") and retrieved at
 * application startup or on demand. This enables:
 *   - Centralized secret lifecycle management
 *   - Automatic rotation without code changes
 *   - Full audit logging via AWS CloudTrail
 *   - Elimination of credentials from source code and configuration files
 */
public class AwsSecretsManagerUtil {

    /**
     * AWS region read from the environment variable AWS_REGION.
     * Defaults to "us-east-1" if not set.
     */
    private static final String AWS_REGION =
            System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";

    /**
     * Retrieves a secret value from AWS Secrets Manager by its secret name/ARN.
     *
     * @param secretName the name or ARN of the secret stored in AWS Secrets Manager
     * @return the secret string value
     * @throws RuntimeException if the secret cannot be retrieved
     */
    public static String getSecret(String secretName) {
        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
                .withRegion(AWS_REGION)
                .build();

        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
                .withSecretId(secretName);

        GetSecretValueResult getSecretValueResult;

        try {
            getSecretValueResult = client.getSecretValue(getSecretValueRequest);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("AWS Secrets Manager: Secret not found: " + secretName, e);
        } catch (InvalidRequestException e) {
            throw new RuntimeException("AWS Secrets Manager: Invalid request for secret: " + secretName, e);
        } catch (InvalidParameterException e) {
            throw new RuntimeException("AWS Secrets Manager: Invalid parameter for secret: " + secretName, e);
        }

        if (getSecretValueResult.getSecretString() != null) {
            return getSecretValueResult.getSecretString();
        } else {
            // Binary secrets are not expected in this application
            throw new RuntimeException(
                    "AWS Secrets Manager: Secret '" + secretName + "' is stored as binary, expected string.");
        }
    }

    /**
     * Retrieves the database password from AWS Secrets Manager.
     * Secret name is resolved from the environment variable HMS_DB_SECRET_NAME,
     * defaulting to "hms/db/password".
     *
     * @return the database password string
     */
    public static String getDatabasePassword() {
        String secretName = System.getenv("HMS_DB_SECRET_NAME") != null
                ? System.getenv("HMS_DB_SECRET_NAME")
                : "hms/db/password";
        return getSecret(secretName);
    }

    /**
     * Retrieves the database username from AWS Secrets Manager.
     * Secret name is resolved from the environment variable HMS_DB_USER_SECRET_NAME,
     * defaulting to "hms/db/username".
     *
     * @return the database username string
     */
    public static String getDatabaseUsername() {
        String secretName = System.getenv("HMS_DB_USER_SECRET_NAME") != null
                ? System.getenv("HMS_DB_USER_SECRET_NAME")
                : "hms/db/username";
        return getSecret(secretName);
    }
}
