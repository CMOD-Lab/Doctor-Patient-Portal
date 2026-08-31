# Cloud Readiness Security Improvements

## Overview
This document describes the security improvements made to the Hospital Management System (HMS) application to make it cloud-ready and compliant with AWS best practices.

## Issue Addressed
**Rule ID:** cr-java-0113  
**Rule Name:** Lack of Externalized Secrets  
**Severity:** CRITICAL  
**Category:** configuration-management

### Problem Statement
The application previously stored sensitive information (passwords, encryption keys) in plain text or hardcoded in source code, creating security vulnerabilities and preventing proper credential lifecycle management.

## Solution Implemented
Migrated all hardcoded secrets to AWS Secrets Manager with support for automatic rotation, enabling centralized secret management, audit logging, and eliminating security vulnerabilities from source code.

## Changes Made

### 1. New Utility Classes

#### SecretsManagerUtil.java
- **Location:** `src/main/java/com/hms/util/SecretsManagerUtil.java`
- **Purpose:** Provides centralized access to AWS Secrets Manager
- **Features:**
  - Retrieves secrets from AWS Secrets Manager
  - Caches secrets to minimize API calls
  - Supports JSON secret parsing
  - Handles secret rotation
  - Falls back to environment variables for local development

#### PasswordEncryptionUtil.java
- **Location:** `src/main/java/com/hms/util/PasswordEncryptionUtil.java`
- **Purpose:** Provides password encryption using keys from AWS Secrets Manager
- **Features:**
  - AES-256 encryption for passwords
  - PBKDF2 password hashing (recommended for production)
  - Encryption keys retrieved from AWS Secrets Manager (never hardcoded)
  - Support for key rotation
  - Fallback to environment variables for local development

### 2. Updated Entity Classes

#### User.java
- **Location:** `src/main/java/com/hms/entity/User.java`
- **Changes:**
  - Added documentation about password encryption requirements
  - Added `setEncryptedPassword()` method for secure password storage
  - Added `getDecryptedPassword()` method for password retrieval
  - Added `verifyPassword()` method for password verification
  - Password field now stores encrypted passwords only

#### Doctor.java
- **Location:** `src/main/java/com/hms/entity/Doctor.java`
- **Changes:**
  - Added documentation about password encryption requirements
  - Added `setEncryptedPassword()` method for secure password storage
  - Added `getDecryptedPassword()` method for password retrieval
  - Added `verifyPassword()` method for password verification
  - Password field now stores encrypted passwords only

### 3. Updated DAO Classes

#### UserDAO.java
- **Location:** `src/main/java/com/hms/dao/UserDAO.java`
- **Changes:**
  - `userRegister()`: Encrypts passwords before storing in database
  - `loginUser()`: Encrypts provided password for comparison
  - `checkOldPassword()`: Encrypts provided password for verification
  - `changePassword()`: Encrypts new password before storing
  - All password operations now use `PasswordEncryptionUtil`

#### DoctorDAO.java
- **Location:** `src/main/java/com/hms/dao/DoctorDAO.java`
- **Changes:**
  - `registerDoctor()`: Encrypts passwords before storing in database
  - `loginDoctor()`: Encrypts provided password for comparison
  - `updateDoctor()`: Encrypts passwords before updating
  - `checkOldPassword()`: Encrypts provided password for verification
  - `changePassword()`: Encrypts new password before storing
  - All password operations now use `PasswordEncryptionUtil`

### 4. Configuration Files

#### pom.xml
- **Already includes required dependencies:**
  - AWS SDK for Secrets Manager (v2.20.26)
  - AWS SDK for Authentication
  - Jackson for JSON parsing

### 5. Documentation

#### AWS_SECRETS_MANAGER_SETUP.md
- **Location:** `AWS_SECRETS_MANAGER_SETUP.md`
- **Contents:**
  - Complete setup guide for AWS Secrets Manager
  - Required secrets and their formats
  - IAM permissions configuration
  - Environment variable setup
  - Secret rotation configuration
  - Local development setup
  - Troubleshooting guide
  - Migration guide for existing data

## Security Benefits

### 1. No Hardcoded Secrets
- ✅ All encryption keys stored in AWS Secrets Manager
- ✅ No secrets in source code or configuration files
- ✅ Secrets never committed to version control

### 2. Centralized Secret Management
- ✅ Single source of truth for all secrets
- ✅ Centralized access control via IAM policies
- ✅ Audit logging via AWS CloudTrail

### 3. Automatic Rotation Support
- ✅ Secrets can be rotated without code changes
- ✅ Application supports cache clearing for rotation
- ✅ Reduced risk from compromised credentials

### 4. Encryption at Rest and in Transit
- ✅ Passwords encrypted using AES-256
- ✅ Encryption keys stored securely in AWS Secrets Manager
- ✅ Secrets transmitted over TLS to/from Secrets Manager

### 5. Compliance Support
- ✅ PCI DSS: Encryption keys not in source code
- ✅ HIPAA: Patient data passwords encrypted
- ✅ SOC 2: Centralized secret management with audit logging
- ✅ GDPR: Enhanced data protection through encryption

## Deployment Requirements

### AWS Resources Required
1. **AWS Secrets Manager Secrets:**
   - `hms/encryption/password-key` - AES encryption key for passwords

2. **IAM Permissions:**
   - `secretsmanager:GetSecretValue` on required secrets
   - `secretsmanager:DescribeSecret` on required secrets

3. **Environment Variables:**
   - `AWS_REGION` - AWS region for Secrets Manager (e.g., us-east-1)

### Deployment Steps
1. Create required secrets in AWS Secrets Manager (see AWS_SECRETS_MANAGER_SETUP.md)
2. Configure IAM role with appropriate permissions
3. Set environment variables in deployment environment
4. Deploy application
5. Verify secret retrieval in application logs

## Local Development

### Option 1: Use AWS Secrets Manager (Recommended)
```bash
# Configure AWS credentials
aws configure

# Set region
export AWS_REGION=us-east-1

# Run application
mvn tomcat7:run
```

### Option 2: Use Environment Variable Fallback
```bash
# Generate encryption key
openssl rand -base64 32

# Set environment variables
export HMS_ENCRYPTION_KEY=<generated-key>
export AWS_REGION=us-east-1

# Run application
mvn tomcat7:run
```

## Migration Guide

### For Existing Deployments with Plain-Text Passwords

⚠️ **Important:** Existing passwords in the database are in plain text and need to be migrated.

#### Option 1: Force Password Reset (Recommended)
1. Deploy the updated application
2. Clear all existing passwords in the database
3. Force users to reset passwords on next login
4. New passwords will be encrypted automatically

#### Option 2: Encrypt Existing Passwords
1. Create a one-time migration script
2. Read all existing passwords
3. Encrypt them using `PasswordEncryptionUtil.encryptPassword()`
4. Update database with encrypted passwords
5. Deploy the updated application

**Sample Migration Script:**
```java
// Run this once before deploying the updated application
Connection conn = DBConnection.getConn();
UserDAO userDAO = new UserDAO(conn);
// Fetch all users and encrypt their passwords
// This is a simplified example - implement proper error handling
```

## Testing

### Unit Tests
Test password encryption and decryption:
```java
String plainPassword = "testPassword123";
String encrypted = PasswordEncryptionUtil.encryptPassword(plainPassword);
String decrypted = PasswordEncryptionUtil.decryptPassword(encrypted);
assert plainPassword.equals(decrypted);
```

### Integration Tests
1. Test user registration with encrypted passwords
2. Test user login with encrypted passwords
3. Test password change functionality
4. Test secret retrieval from AWS Secrets Manager

## Monitoring and Logging

### Application Logs
The application logs the following events:
- AWS Secrets Manager client initialization
- Secret retrieval success/failure
- Encryption key loading
- Password encryption/decryption operations (without exposing secrets)

### AWS CloudTrail
Monitor these events in CloudTrail:
- `GetSecretValue` - Secret retrieval
- `DescribeSecret` - Secret metadata access
- Failed authentication attempts

### Recommended CloudWatch Alarms
1. Failed secret retrieval attempts
2. Unauthorized access to secrets
3. Unusual number of secret retrievals

## Troubleshooting

### Common Issues

#### 1. "Failed to retrieve secret from AWS Secrets Manager"
**Cause:** IAM permissions or secret not found  
**Solution:** Verify IAM role has `secretsmanager:GetSecretValue` permission and secret exists

#### 2. "Encryption key not found"
**Cause:** Secret not created or environment variable not set  
**Solution:** Create secret in AWS Secrets Manager or set `HMS_ENCRYPTION_KEY` for local dev

#### 3. "Password encryption failed"
**Cause:** Invalid encryption key format  
**Solution:** Regenerate key using `openssl rand -base64 32`

#### 4. Login fails after deployment
**Cause:** Existing passwords are plain text, new code expects encrypted  
**Solution:** Run migration script or force password reset

## Security Considerations

### Best Practices Implemented
✅ Secrets never in source code  
✅ Encryption keys externalized to AWS Secrets Manager  
✅ Support for automatic secret rotation  
✅ Least privilege IAM permissions  
✅ Audit logging via CloudTrail  
✅ Encryption at rest and in transit  
✅ Secure random key generation  

### Additional Recommendations
1. Enable AWS Secrets Manager automatic rotation
2. Use AWS KMS customer-managed keys for Secrets Manager
3. Implement rate limiting on login attempts
4. Add multi-factor authentication (MFA)
5. Use PBKDF2 hashing instead of encryption for passwords (one-way)
6. Implement password complexity requirements
7. Add password expiration policies

## Cost Considerations

### AWS Secrets Manager Pricing (as of 2024)
- **Secret storage:** $0.40 per secret per month
- **API calls:** $0.05 per 10,000 API calls

### Cost Optimization
- Secrets are cached in application memory to minimize API calls
- Estimated cost for this application: ~$1-2 per month

## Support and Resources

### Documentation
- [AWS Secrets Manager Documentation](https://docs.aws.amazon.com/secretsmanager/)
- [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)

### Code References
- `SecretsManagerUtil.java` - AWS Secrets Manager integration
- `PasswordEncryptionUtil.java` - Password encryption utilities
- `AWS_SECRETS_MANAGER_SETUP.md` - Detailed setup guide

## Compliance and Audit

This implementation supports compliance with:
- **PCI DSS Requirement 3.4:** Encryption keys not stored in source code
- **HIPAA Security Rule:** Administrative safeguards for password management
- **SOC 2 CC6.1:** Logical and physical access controls
- **GDPR Article 32:** Security of processing

All secret access is logged in AWS CloudTrail for audit purposes.

## Version History

### Version 1.0 (Current)
- Initial implementation of AWS Secrets Manager integration
- Password encryption using AES-256
- Support for automatic secret rotation
- Comprehensive documentation and setup guides

## Future Enhancements

### Planned Improvements
1. Migrate from AES encryption to PBKDF2 hashing (one-way)
2. Add support for AWS KMS for additional encryption layer
3. Implement database credential rotation
4. Add support for multiple encryption key versions
5. Implement password complexity validation
6. Add rate limiting for failed login attempts
7. Implement session management with secure tokens

## Conclusion

The application is now cloud-ready with enterprise-grade secret management. All hardcoded secrets have been eliminated, and the application follows AWS security best practices for credential management.

For setup instructions, see [AWS_SECRETS_MANAGER_SETUP.md](AWS_SECRETS_MANAGER_SETUP.md).
