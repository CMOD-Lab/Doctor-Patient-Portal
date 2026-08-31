# AWS Secrets Manager Migration Guide

## Overview
This application has been updated to use AWS Secrets Manager for secure credential and secret management, replacing hardcoded credentials and sensitive data in source code.

## Changes Made

### 1. Removed Hardcoded Secrets
- **User.java (Line 75)**: Removed password field from `toString()` method to prevent password exposure in logs and debugging output
- Passwords and sensitive data are no longer exposed in string representations of objects

### 2. Added AWS Secrets Manager Integration
- **SecretsManagerUtil.java**: New utility class for retrieving secrets from AWS Secrets Manager
  - Supports simple string secrets
  - Supports JSON-formatted secrets with key-value pairs
  - Implements caching to minimize API calls
  - Provides automatic credential rotation support

### 3. Updated Dependencies
- Added AWS SDK for Secrets Manager (v2.20.26)
- Added Jackson for JSON parsing (v2.14.2)

## Configuration

### Environment Variables
Set the following environment variable:
```bash
export AWS_REGION=us-east-1  # or your preferred AWS region
```

### AWS Credentials
The application uses AWS SDK's DefaultCredentialsProvider, which checks for credentials in this order:
1. Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
2. Java system properties
3. AWS credentials file (`~/.aws/credentials`)
4. IAM role (recommended for production)

### Required Secrets in AWS Secrets Manager

#### Database Credentials
Create a secret named `hms/database/credentials` with the following JSON structure:
```json
{
  "username": "your-db-username",
  "password": "your-db-password",
  "host": "your-db-host",
  "port": "3306",
  "database": "hospital"
}
```

**AWS CLI Command:**
```bash
aws secretsmanager create-secret \
  --name hms/database/credentials \
  --description "Database credentials for Hospital Management System" \
  --secret-string '{"username":"dbuser","password":"dbpass","host":"your-rds-endpoint","port":"3306","database":"hospital"}'
```

## Usage Examples

### Retrieving a Simple Secret
```java
import com.hms.util.SecretsManagerUtil;

// Get entire secret as string
String apiKey = SecretsManagerUtil.getSecret("hms/api/key");
```

### Retrieving a Value from JSON Secret
```java
import com.hms.util.SecretsManagerUtil;

// Get specific value from JSON secret
String dbUsername = SecretsManagerUtil.getSecretValue("hms/database/credentials", "username");
String dbPassword = SecretsManagerUtil.getSecretValue("hms/database/credentials", "password");
String dbHost = SecretsManagerUtil.getSecretValue("hms/database/credentials", "host");
```

### Example: Updating DBConnection.java
To migrate database connection to use AWS Secrets Manager:

```java
package com.hms.db;

import java.sql.Connection;
import java.sql.DriverManager;
import com.hms.util.SecretsManagerUtil;

public class DBConnection {
    private static Connection conn;
    
    public static Connection getConn() {
        try {
            // Load the driver class
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Retrieve credentials from AWS Secrets Manager
            String dbHost = SecretsManagerUtil.getSecretValue("hms/database/credentials", "host");
            String dbPort = SecretsManagerUtil.getSecretValue("hms/database/credentials", "port");
            String dbName = SecretsManagerUtil.getSecretValue("hms/database/credentials", "database");
            String dbUser = SecretsManagerUtil.getSecretValue("hms/database/credentials", "username");
            String dbPassword = SecretsManagerUtil.getSecretValue("hms/database/credentials", "password");
            
            // Build connection URL
            String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s", dbHost, dbPort, dbName);
            
            // Create connection
            conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }
}
```

## IAM Permissions

### Required IAM Policy
Attach this policy to your application's IAM role:

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
        "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:hms/*"
      ]
    }
  ]
}
```

### For EC2 Instances
1. Create an IAM role with the above policy
2. Attach the role to your EC2 instance
3. No additional configuration needed in the application

### For ECS Tasks
1. Create an IAM role with the above policy
2. Assign the role as the task execution role in your ECS task definition
3. No additional configuration needed in the application

## Automatic Secret Rotation

Enable automatic rotation for enhanced security:

```bash
aws secretsmanager rotate-secret \
  --secret-id hms/database/credentials \
  --rotation-lambda-arn arn:aws:lambda:REGION:ACCOUNT_ID:function:rotation-function \
  --rotation-rules AutomaticallyAfterDays=30
```

## Security Best Practices

1. **Never commit secrets to source control**
   - Use `.gitignore` to exclude any files containing secrets
   - Use AWS Secrets Manager for all sensitive data

2. **Use IAM roles in production**
   - Avoid using access keys when running in AWS
   - Attach IAM roles to EC2, ECS, or Lambda resources

3. **Enable CloudTrail logging**
   - Monitor all secret access for security auditing
   - Set up alerts for unauthorized access attempts

4. **Implement secret rotation**
   - Rotate database passwords regularly (recommended: 30-90 days)
   - Use AWS Lambda functions for automated rotation

5. **Use least privilege IAM policies**
   - Grant only necessary permissions
   - Restrict access to specific secret ARNs

6. **Encrypt secrets at rest**
   - AWS Secrets Manager encrypts secrets by default using AWS KMS
   - Use customer-managed KMS keys for additional control

## Troubleshooting

### Issue: "Unable to load credentials from any provider"
**Solution**: Ensure AWS credentials are configured properly. For local development, run:
```bash
aws configure
```

### Issue: "Access Denied" when retrieving secrets
**Solution**: Verify that the IAM role/user has `secretsmanager:GetSecretValue` permission for the secret ARN.

### Issue: "Secret not found"
**Solution**: Verify the secret name and region. Ensure the secret exists in the same region specified by `AWS_REGION`.

### Issue: "Connection timeout"
**Solution**: Ensure your application has network access to AWS Secrets Manager endpoints. Check security groups and network ACLs.

## Migration Checklist

- [x] Added AWS Secrets Manager SDK dependency
- [x] Created SecretsManagerUtil utility class
- [x] Removed password from User.toString() method
- [ ] Update DBConnection.java to use SecretsManagerUtil (recommended)
- [ ] Create secrets in AWS Secrets Manager
- [ ] Configure IAM permissions
- [ ] Set AWS_REGION environment variable
- [ ] Test secret retrieval in development environment
- [ ] Enable CloudTrail logging for audit
- [ ] Configure automatic secret rotation
- [ ] Update deployment documentation

## Additional Resources

- [AWS Secrets Manager Documentation](https://docs.aws.amazon.com/secretsmanager/)
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [Secret Rotation Best Practices](https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets.html)
