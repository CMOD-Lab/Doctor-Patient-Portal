# AWS Deployment Guide - Hospital Management System

## Overview

This guide provides step-by-step instructions for deploying the Hospital Management System to AWS with Redis-backed session management using Amazon ElastiCache.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         AWS Cloud                            │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    VPC (10.0.0.0/16)                   │ │
│  │                                                         │ │
│  │  ┌──────────────────┐      ┌──────────────────┐       │ │
│  │  │  Public Subnet   │      │  Private Subnet  │       │ │
│  │  │  (10.0.1.0/24)   │      │  (10.0.2.0/24)   │       │ │
│  │  │                  │      │                  │       │ │
│  │  │  ┌────────────┐  │      │  ┌────────────┐ │       │ │
│  │  │  │    ALB     │  │      │  │    ECS     │ │       │ │
│  │  │  │            │  │      │  │  Fargate   │ │       │ │
│  │  │  └─────┬──────┘  │      │  │  Tasks     │ │       │ │
│  │  │        │         │      │  └─────┬──────┘ │       │ │
│  │  └────────┼─────────┘      │        │        │       │ │
│  │           │                │        │        │       │ │
│  │           └────────────────┼────────┘        │       │ │
│  │                            │                 │       │ │
│  │                            │  ┌────────────┐ │       │ │
│  │                            │  │ ElastiCache│ │       │ │
│  │                            │  │   Redis    │ │       │ │
│  │                            │  └────────────┘ │       │ │
│  │                            │                 │       │ │
│  │                            │  ┌────────────┐ │       │ │
│  │                            │  │    RDS     │ │       │ │
│  │                            │  │   MySQL    │ │       │ │
│  │                            │  └────────────┘ │       │ │
│  │                                              │       │ │
│  └──────────────────────────────────────────────┘       │ │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

## Prerequisites

### 1. AWS Account Setup
- AWS Account with appropriate permissions
- AWS CLI installed and configured
- IAM user with permissions for:
  - ECS/Fargate
  - ElastiCache
  - RDS
  - VPC
  - CloudWatch
  - Secrets Manager

### 2. Local Development Tools
- Docker installed
- Maven 3.6+
- Java 8+
- Git

## Step 1: Create VPC and Network Infrastructure

### 1.1 Create VPC

```bash
# Create VPC
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=hms-vpc}]'

# Note the VPC ID from output
export VPC_ID=vpc-xxxxxxxxx
```

### 1.2 Create Subnets

```bash
# Create public subnet (for ALB)
aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=hms-public-subnet-1a}]'

export PUBLIC_SUBNET_1A=subnet-xxxxxxxxx

# Create another public subnet in different AZ (for ALB high availability)
aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.3.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=hms-public-subnet-1b}]'

export PUBLIC_SUBNET_1B=subnet-yyyyyyyyy

# Create private subnet (for ECS, ElastiCache, RDS)
aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=hms-private-subnet-1a}]'

export PRIVATE_SUBNET_1A=subnet-zzzzzzzzz

# Create another private subnet in different AZ
aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.4.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=hms-private-subnet-1b}]'

export PRIVATE_SUBNET_1B=subnet-aaaaaaaaa
```

### 1.3 Create Internet Gateway

```bash
# Create Internet Gateway
aws ec2 create-internet-gateway \
  --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=hms-igw}]'

export IGW_ID=igw-xxxxxxxxx

# Attach to VPC
aws ec2 attach-internet-gateway \
  --vpc-id $VPC_ID \
  --internet-gateway-id $IGW_ID
```

### 1.4 Create NAT Gateway (for private subnet internet access)

```bash
# Allocate Elastic IP
aws ec2 allocate-address --domain vpc

export EIP_ALLOC_ID=eipalloc-xxxxxxxxx

# Create NAT Gateway in public subnet
aws ec2 create-nat-gateway \
  --subnet-id $PUBLIC_SUBNET_1A \
  --allocation-id $EIP_ALLOC_ID \
  --tag-specifications 'ResourceType=natgateway,Tags=[{Key=Name,Value=hms-nat-gw}]'

export NAT_GW_ID=nat-xxxxxxxxx
```

### 1.5 Configure Route Tables

```bash
# Create route table for public subnets
aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --tag-specifications 'ResourceType=route-table,Tags=[{Key=Name,Value=hms-public-rt}]'

export PUBLIC_RT_ID=rtb-xxxxxxxxx

# Add route to Internet Gateway
aws ec2 create-route \
  --route-table-id $PUBLIC_RT_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $IGW_ID

# Associate public subnets with public route table
aws ec2 associate-route-table \
  --subnet-id $PUBLIC_SUBNET_1A \
  --route-table-id $PUBLIC_RT_ID

aws ec2 associate-route-table \
  --subnet-id $PUBLIC_SUBNET_1B \
  --route-table-id $PUBLIC_RT_ID

# Create route table for private subnets
aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --tag-specifications 'ResourceType=route-table,Tags=[{Key=Name,Value=hms-private-rt}]'

export PRIVATE_RT_ID=rtb-yyyyyyyyy

# Add route to NAT Gateway
aws ec2 create-route \
  --route-table-id $PRIVATE_RT_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --nat-gateway-id $NAT_GW_ID

# Associate private subnets with private route table
aws ec2 associate-route-table \
  --subnet-id $PRIVATE_SUBNET_1A \
  --route-table-id $PRIVATE_RT_ID

aws ec2 associate-route-table \
  --subnet-id $PRIVATE_SUBNET_1B \
  --route-table-id $PRIVATE_RT_ID
```

## Step 2: Create Security Groups

### 2.1 ALB Security Group

```bash
aws ec2 create-security-group \
  --group-name hms-alb-sg \
  --description "Security group for HMS Application Load Balancer" \
  --vpc-id $VPC_ID

export ALB_SG_ID=sg-xxxxxxxxx

# Allow HTTP from anywhere
aws ec2 authorize-security-group-ingress \
  --group-id $ALB_SG_ID \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

# Allow HTTPS from anywhere
aws ec2 authorize-security-group-ingress \
  --group-id $ALB_SG_ID \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0
```

### 2.2 ECS Tasks Security Group

```bash
aws ec2 create-security-group \
  --group-name hms-ecs-sg \
  --description "Security group for HMS ECS tasks" \
  --vpc-id $VPC_ID

export ECS_SG_ID=sg-yyyyyyyyy

# Allow traffic from ALB
aws ec2 authorize-security-group-ingress \
  --group-id $ECS_SG_ID \
  --protocol tcp \
  --port 8080 \
  --source-group $ALB_SG_ID
```

### 2.3 ElastiCache Security Group

```bash
aws ec2 create-security-group \
  --group-name hms-redis-sg \
  --description "Security group for HMS ElastiCache Redis" \
  --vpc-id $VPC_ID

export REDIS_SG_ID=sg-zzzzzzzzz

# Allow Redis traffic from ECS tasks
aws ec2 authorize-security-group-ingress \
  --group-id $REDIS_SG_ID \
  --protocol tcp \
  --port 6379 \
  --source-group $ECS_SG_ID
```

### 2.4 RDS Security Group

```bash
aws ec2 create-security-group \
  --group-name hms-rds-sg \
  --description "Security group for HMS RDS MySQL" \
  --vpc-id $VPC_ID

export RDS_SG_ID=sg-aaaaaaaaa

# Allow MySQL traffic from ECS tasks
aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG_ID \
  --protocol tcp \
  --port 3306 \
  --source-group $ECS_SG_ID
```

## Step 3: Create ElastiCache Redis Cluster

### 3.1 Create Subnet Group

```bash
aws elasticache create-cache-subnet-group \
  --cache-subnet-group-name hms-redis-subnet-group \
  --cache-subnet-group-description "Subnet group for HMS Redis" \
  --subnet-ids $PRIVATE_SUBNET_1A $PRIVATE_SUBNET_1B
```

### 3.2 Create Redis Cluster

```bash
aws elasticache create-cache-cluster \
  --cache-cluster-id hms-session-cache \
  --engine redis \
  --cache-node-type cache.t3.micro \
  --num-cache-nodes 1 \
  --engine-version 7.0 \
  --cache-subnet-group-name hms-redis-subnet-group \
  --security-group-ids $REDIS_SG_ID \
  --preferred-availability-zone us-east-1a \
  --tags Key=Name,Value=hms-session-cache

# Wait for cluster to be available (takes 5-10 minutes)
aws elasticache wait cache-cluster-available \
  --cache-cluster-id hms-session-cache

# Get Redis endpoint
aws elasticache describe-cache-clusters \
  --cache-cluster-id hms-session-cache \
  --show-cache-node-info \
  --query 'CacheClusters[0].CacheNodes[0].Endpoint.Address' \
  --output text

export REDIS_HOST=hms-session-cache.abc123.0001.use1.cache.amazonaws.com
```

### 3.3 Enable AUTH (Optional but Recommended)

```bash
# Modify cluster to enable AUTH
aws elasticache modify-cache-cluster \
  --cache-cluster-id hms-session-cache \
  --auth-token "YourSecureAuthToken123!" \
  --auth-token-update-strategy SET \
  --apply-immediately

# Store AUTH token in Secrets Manager
aws secretsmanager create-secret \
  --name hms/redis/auth-token \
  --secret-string '{"token":"YourSecureAuthToken123!"}'
```

## Step 4: Create RDS MySQL Database

### 4.1 Create DB Subnet Group

```bash
aws rds create-db-subnet-group \
  --db-subnet-group-name hms-db-subnet-group \
  --db-subnet-group-description "Subnet group for HMS RDS" \
  --subnet-ids $PRIVATE_SUBNET_1A $PRIVATE_SUBNET_1B
```

### 4.2 Create RDS Instance

```bash
aws rds create-db-instance \
  --db-instance-identifier hms-database \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0.35 \
  --master-username admin \
  --master-user-password "YourSecurePassword123!" \
  --allocated-storage 20 \
  --db-subnet-group-name hms-db-subnet-group \
  --vpc-security-group-ids $RDS_SG_ID \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "mon:04:00-mon:05:00" \
  --publicly-accessible false \
  --storage-encrypted \
  --tags Key=Name,Value=hms-database

# Wait for database to be available (takes 10-15 minutes)
aws rds wait db-instance-available \
  --db-instance-identifier hms-database

# Get database endpoint
aws rds describe-db-instances \
  --db-instance-identifier hms-database \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text

export DB_HOST=hms-database.abc123.us-east-1.rds.amazonaws.com
```

### 4.3 Store Database Credentials in Secrets Manager

```bash
aws secretsmanager create-secret \
  --name hms/database/credentials \
  --secret-string '{
    "username": "admin",
    "password": "YourSecurePassword123!",
    "host": "'$DB_HOST'",
    "port": "3306",
    "database": "hospital_db"
  }'
```

## Step 5: Build and Push Docker Image

### 5.1 Create ECR Repository

```bash
aws ecr create-repository \
  --repository-name hms-app \
  --image-scanning-configuration scanOnPush=true

export ECR_REPO_URI=$(aws ecr describe-repositories \
  --repository-names hms-app \
  --query 'repositories[0].repositoryUri' \
  --output text)
```

### 5.2 Build Application

```bash
# Build WAR file
mvn clean package

# Verify WAR file
ls -lh target/Doctor-Patient-Portal.war
```

### 5.3 Create Dockerfile

Create `Dockerfile` in project root:

```dockerfile
FROM tomcat:9.0-jdk8-openjdk

# Remove default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy WAR file
COPY target/Doctor-Patient-Portal.war /usr/local/tomcat/webapps/ROOT.war

# Expose port
EXPOSE 8080

# Start Tomcat
CMD ["catalina.sh", "run"]
```

### 5.4 Build and Push Docker Image

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $ECR_REPO_URI

# Build image
docker build -t hms-app .

# Tag image
docker tag hms-app:latest $ECR_REPO_URI:latest

# Push image
docker push $ECR_REPO_URI:latest
```

## Step 6: Create ECS Cluster and Service

### 6.1 Create ECS Cluster

```bash
aws ecs create-cluster \
  --cluster-name hms-cluster \
  --capacity-providers FARGATE \
  --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1
```

### 6.2 Create Task Execution Role

```bash
# Create trust policy
cat > ecs-task-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create role
aws iam create-role \
  --role-name hmsEcsTaskExecutionRole \
  --assume-role-policy-document file://ecs-task-trust-policy.json

# Attach AWS managed policy
aws iam attach-role-policy \
  --role-name hmsEcsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

# Create policy for Secrets Manager access
cat > secrets-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:*:secret:hms/*"
      ]
    }
  ]
}
EOF

aws iam create-policy \
  --policy-name hmsSecretsManagerAccess \
  --policy-document file://secrets-policy.json

export SECRETS_POLICY_ARN=$(aws iam list-policies \
  --query 'Policies[?PolicyName==`hmsSecretsManagerAccess`].Arn' \
  --output text)

aws iam attach-role-policy \
  --role-name hmsEcsTaskExecutionRole \
  --policy-arn $SECRETS_POLICY_ARN

export TASK_EXECUTION_ROLE_ARN=$(aws iam get-role \
  --role-name hmsEcsTaskExecutionRole \
  --query 'Role.Arn' \
  --output text)
```

### 6.3 Create Task Definition

Create `task-definition.json`:

```json
{
  "family": "hms-task",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "TASK_EXECUTION_ROLE_ARN",
  "containerDefinitions": [
    {
      "name": "hms-app",
      "image": "ECR_REPO_URI:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "REDIS_HOST",
          "value": "REDIS_HOST"
        },
        {
          "name": "REDIS_PORT",
          "value": "6379"
        },
        {
          "name": "REDIS_SSL",
          "value": "false"
        }
      ],
      "secrets": [
        {
          "name": "DB_USERNAME",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:hms/database/credentials:username::"
        },
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:hms/database/credentials:password::"
        },
        {
          "name": "DB_HOST",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:hms/database/credentials:host::"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/hms-app",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

Replace placeholders and register:

```bash
# Replace placeholders
sed -i "s|TASK_EXECUTION_ROLE_ARN|$TASK_EXECUTION_ROLE_ARN|g" task-definition.json
sed -i "s|ECR_REPO_URI|$ECR_REPO_URI|g" task-definition.json
sed -i "s|REDIS_HOST|$REDIS_HOST|g" task-definition.json
sed -i "s|ACCOUNT_ID|$(aws sts get-caller-identity --query Account --output text)|g" task-definition.json

# Create CloudWatch log group
aws logs create-log-group --log-group-name /ecs/hms-app

# Register task definition
aws ecs register-task-definition \
  --cli-input-json file://task-definition.json
```

### 6.4 Create Application Load Balancer

```bash
# Create ALB
aws elbv2 create-load-balancer \
  --name hms-alb \
  --subnets $PUBLIC_SUBNET_1A $PUBLIC_SUBNET_1B \
  --security-groups $ALB_SG_ID \
  --scheme internet-facing \
  --type application

export ALB_ARN=$(aws elbv2 describe-load-balancers \
  --names hms-alb \
  --query 'LoadBalancers[0].LoadBalancerArn' \
  --output text)

export ALB_DNS=$(aws elbv2 describe-load-balancers \
  --names hms-alb \
  --query 'LoadBalancers[0].DNSName' \
  --output text)

# Create target group
aws elbv2 create-target-group \
  --name hms-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --health-check-path /index.jsp \
  --health-check-interval-seconds 30 \
  --health-check-timeout-seconds 5 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3

export TG_ARN=$(aws elbv2 describe-target-groups \
  --names hms-tg \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text)

# Create listener
aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=forward,TargetGroupArn=$TG_ARN
```

### 6.5 Create ECS Service

```bash
aws ecs create-service \
  --cluster hms-cluster \
  --service-name hms-service \
  --task-definition hms-task \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={
    subnets=[$PRIVATE_SUBNET_1A,$PRIVATE_SUBNET_1B],
    securityGroups=[$ECS_SG_ID],
    assignPublicIp=DISABLED
  }" \
  --load-balancers "targetGroupArn=$TG_ARN,containerName=hms-app,containerPort=8080" \
  --health-check-grace-period-seconds 60

# Wait for service to stabilize
aws ecs wait services-stable \
  --cluster hms-cluster \
  --services hms-service
```

## Step 7: Initialize Database

### 7.1 Connect to RDS

```bash
# Use bastion host or AWS Systems Manager Session Manager
mysql -h $DB_HOST -u admin -p
```

### 7.2 Create Database and Tables

```sql
CREATE DATABASE IF NOT EXISTS hospital_db;
USE hospital_db;

-- Create tables (use your existing schema)
-- Example:
CREATE TABLE users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(255),
  email VARCHAR(255) UNIQUE,
  password VARCHAR(255)
);

-- Add other tables...
```

## Step 8: Verify Deployment

### 8.1 Check Service Status

```bash
# Check ECS service
aws ecs describe-services \
  --cluster hms-cluster \
  --services hms-service

# Check tasks
aws ecs list-tasks \
  --cluster hms-cluster \
  --service-name hms-service

# Check task logs
aws logs tail /ecs/hms-app --follow
```

### 8.2 Test Application

```bash
# Access application
echo "Application URL: http://$ALB_DNS"

# Test health endpoint
curl http://$ALB_DNS/index.jsp

# Test login
curl -X POST http://$ALB_DNS/userLogin \
  -d "email=test@example.com&password=test123"
```

### 8.3 Verify Redis Session

```bash
# Connect to Redis (from within VPC)
redis-cli -h $REDIS_HOST

# List session keys
KEYS hms:session:*

# View session data
GET hms:session:sessions:<session-id>
```

## Step 9: Configure Auto Scaling

### 9.1 Create Auto Scaling Target

```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/hms-cluster/hms-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10
```

### 9.2 Create Scaling Policies

```bash
# Scale up policy
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/hms-cluster/hms-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name hms-scale-up \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    },
    "ScaleOutCooldown": 60,
    "ScaleInCooldown": 60
  }'
```

## Step 10: Configure Monitoring and Alerts

### 10.1 Create CloudWatch Dashboard

```bash
aws cloudwatch put-dashboard \
  --dashboard-name HMS-Dashboard \
  --dashboard-body file://dashboard.json
```

### 10.2 Create Alarms

```bash
# High CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name hms-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=ServiceName,Value=hms-service Name=ClusterName,Value=hms-cluster

# Redis memory alarm
aws cloudwatch put-metric-alarm \
  --alarm-name hms-redis-memory \
  --alarm-description "Alert when Redis memory exceeds 80%" \
  --metric-name DatabaseMemoryUsagePercentage \
  --namespace AWS/ElastiCache \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=CacheClusterId,Value=hms-session-cache
```

## Step 11: Configure Backup and Disaster Recovery

### 11.1 Enable RDS Automated Backups

```bash
aws rds modify-db-instance \
  --db-instance-identifier hms-database \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --apply-immediately
```

### 11.2 Enable ElastiCache Snapshots

```bash
aws elasticache modify-cache-cluster \
  --cache-cluster-id hms-session-cache \
  --snapshot-retention-limit 7 \
  --snapshot-window "02:00-03:00" \
  --apply-immediately
```

## Cost Estimation

### Monthly Costs (us-east-1)

| Service | Configuration | Monthly Cost |
|---------|--------------|--------------|
| ECS Fargate | 2 tasks (0.5 vCPU, 1GB) | ~$30 |
| ElastiCache | cache.t3.micro | ~$12 |
| RDS MySQL | db.t3.micro | ~$15 |
| ALB | Standard | ~$20 |
| NAT Gateway | 1 gateway | ~$32 |
| Data Transfer | ~100GB | ~$9 |
| **Total** | | **~$118/month** |

### Cost Optimization Tips

1. Use Reserved Instances for RDS and ElastiCache (save 30-50%)
2. Use Spot Instances for non-production environments
3. Enable auto-scaling to scale down during low traffic
4. Use S3 for static assets instead of serving from ECS
5. Enable CloudWatch Logs retention policies

## Security Best Practices

1. **Enable encryption at rest** for RDS and ElastiCache
2. **Use AWS Secrets Manager** for all credentials
3. **Enable VPC Flow Logs** for network monitoring
4. **Use AWS WAF** with ALB for web application firewall
5. **Enable CloudTrail** for audit logging
6. **Implement least privilege IAM policies**
7. **Enable MFA** for AWS console access
8. **Regular security patching** of container images
9. **Use AWS Security Hub** for compliance monitoring
10. **Enable GuardDuty** for threat detection

## Troubleshooting

### Issue: Tasks failing to start

```bash
# Check task logs
aws ecs describe-tasks \
  --cluster hms-cluster \
  --tasks <task-id>

# Check CloudWatch logs
aws logs tail /ecs/hms-app --follow
```

### Issue: Cannot connect to Redis

```bash
# Verify security group rules
aws ec2 describe-security-groups --group-ids $REDIS_SG_ID

# Test connectivity from ECS task
aws ecs execute-command \
  --cluster hms-cluster \
  --task <task-id> \
  --container hms-app \
  --interactive \
  --command "telnet $REDIS_HOST 6379"
```

### Issue: Database connection errors

```bash
# Verify RDS status
aws rds describe-db-instances \
  --db-instance-identifier hms-database

# Check security group rules
aws ec2 describe-security-groups --group-ids $RDS_SG_ID

# Verify secrets
aws secretsmanager get-secret-value \
  --secret-id hms/database/credentials
```

## Cleanup

To delete all resources:

```bash
# Delete ECS service
aws ecs delete-service --cluster hms-cluster --service hms-service --force

# Delete ECS cluster
aws ecs delete-cluster --cluster hms-cluster

# Delete ALB
aws elbv2 delete-load-balancer --load-balancer-arn $ALB_ARN
aws elbv2 delete-target-group --target-group-arn $TG_ARN

# Delete ElastiCache
aws elasticache delete-cache-cluster --cache-cluster-id hms-session-cache

# Delete RDS
aws rds delete-db-instance \
  --db-instance-identifier hms-database \
  --skip-final-snapshot

# Delete NAT Gateway
aws ec2 delete-nat-gateway --nat-gateway-id $NAT_GW_ID

# Release Elastic IP
aws ec2 release-address --allocation-id $EIP_ALLOC_ID

# Delete Internet Gateway
aws ec2 detach-internet-gateway --internet-gateway-id $IGW_ID --vpc-id $VPC_ID
aws ec2 delete-internet-gateway --internet-gateway-id $IGW_ID

# Delete subnets
aws ec2 delete-subnet --subnet-id $PUBLIC_SUBNET_1A
aws ec2 delete-subnet --subnet-id $PUBLIC_SUBNET_1B
aws ec2 delete-subnet --subnet-id $PRIVATE_SUBNET_1A
aws ec2 delete-subnet --subnet-id $PRIVATE_SUBNET_1B

# Delete VPC
aws ec2 delete-vpc --vpc-id $VPC_ID
```

## Next Steps

1. **Configure custom domain** with Route 53
2. **Enable HTTPS** with ACM certificate
3. **Implement CI/CD** with CodePipeline
4. **Add monitoring** with X-Ray for distributed tracing
5. **Implement caching** with CloudFront CDN
6. **Add WAF rules** for security
7. **Configure backup automation** with AWS Backup
8. **Implement disaster recovery** with multi-region setup

## Support

For issues or questions:
- AWS Support: https://console.aws.amazon.com/support/
- AWS Documentation: https://docs.aws.amazon.com/
- Application logs: CloudWatch Logs `/ecs/hms-app`
