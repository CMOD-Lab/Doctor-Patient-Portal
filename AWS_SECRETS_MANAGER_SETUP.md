# AWS Secrets Manager Setup Guide

## Overview
This application has been updated to use AWS Secrets Manager for secure credential and encryption key management. All hardcoded secrets have been removed from the source code and replaced with AWS Secrets Manager integration.

## Prerequisites
- AWS Account with appropriate permissions
- AWS CLI configured (optional, for command-line setup)
- IAM role or credentials with `secretsmanager:GetSecretValue` permission

## Required Secrets

### 1. Password Encryption Key
**Secret Name:** `hms/encryption/password-key`

**Secret Format:** JSON
```json
{
  "encryptionKey": "BASE64_ENCODED_AES_KEY"
}
```

**How to Generate the Encryption Key:**
```bash
# Generate a 256-bit AES key and encode it in Base64
openssl rand -base64 32
```

**AWS CLI Command to Create Secret:**
```bash
aws secretsmanager create-secret \
    --name hms/encryption/password-key \
    --description "AES encryption key for password encryption in HMS application" \
    --secret-string '{"encryptionKey":"YOUR_BASE64_ENCODED_KEY_HERE"}' \
    --region us-east-1
```

**AWS Console Steps:**
1. Navigate to AWS Secrets Manager in the AWS Console
2. Click "Store a new secret"
3. Select "Other type of secret"
4. Choose "Plaintext" and paste the JSON:
   ```json
   {
     "encryptionKey": "YOUR_BASE64_ENCODED_KEY_HERE"
   }
   ```
5. Name the secret: `hms/encryption/password-key`
6. Configure automatic rotation (recommended)
7. Review and store the secret

### 2. Database Credentials (if using DBConnection with Secrets Manager)
**Secret Name:** `hms/database/credentials`

**Secret Format:** JSON
```json
{
  "host": "your-rds-endpoint.region.rds.amazonaws.com",
  "port": "3306",
  "database": "hospital",
  "username": "admin",
  "password": "your-secure-password"
}
```

**AWS CLI Command to Create Secret:**
```bash
aws secretsmanager create-secret \
    --name hms/database/credentials \
    --description "Database credentials for HMS application" \
    --secret-string '{"host":"your-rds-endpoint","port":"3306","database":"hospital","username":"admin","password":"your-password"}' \
    --region us-east-1
```

## IAM Permissions

### Required IAM Policy
Attach this policy to the IAM role used by your application (EC2 instance role, ECS task role, etc.):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:hms/encryption/password-key-*",
        "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:hms/database/credentials-*"
      ]
    }
  ]
}
```

Replace `ACCOUNT_ID` with your AWS account ID and `us-east-1` with your region.

## Environment Variables

### Required Environment Variables
Set these environment variables in your deployment environment:

```bash
# AWS Region for Secrets Manager
export AWS_REGION=us-east-1

# Optional: For local development only (fallback if Secrets Manager is unavailable)
export HMS_ENCRYPTION_KEY=YOUR_BASE64_ENCODED_KEY
```

### Setting Environment Variables in Different Environments

#### EC2 Instance
Add to `/etc/environment` or user profile:
```bash
AWS_REGION=us-east-1
```

#### ECS Task Definition
```json
{
  "containerDefinitions": [
    {
      "environment": [
        {
          "name": "AWS_REGION",
          "value": "us-east-1"
        }
      ]
    }
  ]
}
```

#### Elastic Beanstalk
Use the EB console or `.ebextensions` configuration:
```yaml
option_settings:
  - namespace: aws:elasticbeanstalk:application:environment
    option_name: AWS_REGION
    value: us-east-1
```

## Secret Rotation

### Enable Automatic Rotation
AWS Secrets Manager supports automatic rotation for enhanced security:

1. Navigate to your secret in AWS Secrets Manager
2. Click "Edit rotation"
3. Enable automatic rotation
4. Choose rotation interval (e.g., 30 days)
5. Select or create a Lambda function for rotation

### Application Support for Rotation
The application includes a cache-clearing mechanism to support secret rotation:

```java
// Call this method after secret rotation to reload keys
PasswordEncryptionUtil.reloadEncryptionKey();
SecretsManagerUtil.clearCache();
```

## Local Development

### Option 1: Use AWS Secrets Manager (Recommended)
1. Configure AWS credentials locally:
   ```bash
   aws configure
   ```
2. Ensure your IAM user has `secretsmanager:GetSecretValue` permission
3. Set `AWS_REGION` environment variable
4. Run the application - it will fetch secrets from AWS

### Option 2: Use Environment Variable Fallback
1. Generate an encryption key:
   ```bash
   openssl rand -base64 32
   ```
2. Set the environment variable:
   ```bash
   export HMS_ENCRYPTION_KEY=YOUR_BASE64_ENCODED_KEY
   export AWS_REGION=us-east-1
   ```
3. Run the application - it will use the environment variable

## Security Best Practices

1. **Never commit secrets to source control**
   - All secrets are now externalized to AWS Secrets Manager
   - The `.gitignore` should exclude any local credential files

2. **Use IAM roles instead of access keys**
   - Assign IAM roles to EC2 instances, ECS tasks, or Lambda functions
   - Avoid using long-term access keys

3. **Enable secret rotation**
   - Rotate encryption keys regularly (e.g., every 30-90 days)
   - Update application configuration to reload secrets after rotation

4. **Monitor secret access**
   - Enable AWS CloudTrail to log all Secrets Manager API calls
   - Set up CloudWatch alarms for unauthorized access attempts

5. **Use least privilege principle**
   - Grant only `GetSecretValue` permission, not `PutSecretValue` or `DeleteSecret`
   - Restrict access to specific secret ARNs

## Troubleshooting

### Error: "Failed to retrieve secret from AWS Secrets Manager"
**Possible Causes:**
- IAM role/user lacks `secretsmanager:GetSecretValue` permission
- Secret name is incorrect
- AWS region is not set or incorrect
- Network connectivity issues

**Solutions:**
1. Verify IAM permissions
2. Check secret name matches exactly: `hms/encryption/password-key`
3. Set `AWS_REGION` environment variable
4. Check VPC endpoints if running in private subnet

### Error: "Encryption key not found in AWS Secrets Manager or environment variables"
**Possible Causes:**
- Secret not created in AWS Secrets Manager
- Environment variable `HMS_ENCRYPTION_KEY` not set (for local dev)

**Solutions:**
1. Create the secret in AWS Secrets Manager (see above)
2. For local development, set `HMS_ENCRYPTION_KEY` environment variable

### Error: "Password encryption failed"
**Possible Causes:**
- Encryption key format is invalid
- Key is not properly Base64 encoded

**Solutions:**
1. Regenerate the encryption key using `openssl rand -base64 32`
2. Ensure the key in Secrets Manager is properly formatted as JSON

## Migration from Hardcoded Secrets

### Existing User Passwords
If you have existing users with plain-text passwords in the database:

1. **Create a migration script** to encrypt existing passwords:
   ```java
   // Pseudo-code for migration
   List<User> users = getAllUsers();
   for (User user : users) {
       String plainPassword = user.getPassword();
       String encryptedPassword = PasswordEncryptionUtil.encryptPassword(plainPassword);
       updateUserPassword(user.getId(), encryptedPassword);
   }
   ```

2. **Run the migration** in a maintenance window

3. **Verify** that login still works after migration

### Database Credentials
If you're migrating database credentials:

1. Create the secret in AWS Secrets Manager
2. Update `DBConnection.java` to use `SecretsManagerUtil`
3. Test connectivity before deploying to production

## Support and Documentation

- **AWS Secrets Manager Documentation:** https://docs.aws.amazon.com/secretsmanager/
- **AWS SDK for Java Documentation:** https://docs.aws.amazon.com/sdk-for-java/
- **Application Code:** See `SecretsManagerUtil.java` and `PasswordEncryptionUtil.java`

## Compliance and Audit

This implementation supports compliance with:
- **PCI DSS:** Encryption keys are not stored in source code
- **HIPAA:** Patient data passwords are encrypted with externally managed keys
- **SOC 2:** Centralized secret management with audit logging
- **GDPR:** Enhanced data protection through encryption

AWS CloudTrail automatically logs all Secrets Manager API calls for audit purposes.
