# Doctor-Patient-Portal – Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Local Development with Docker Compose](#local-development-with-docker-compose)
5. [Building and Pushing the Docker Image](#building-and-pushing-the-docker-image)
6. [AWS EKS Deployment](#aws-eks-deployment)
7. [Configuration Management](#configuration-management)
8. [Scaling and Management](#scaling-and-management)
9. [Troubleshooting](#troubleshooting)
10. [Security Considerations](#security-considerations)

---

## Overview

**Application**: Doctor-Patient-Portal  
**Technology**: Java 8, Maven, Servlet/JSP, Spring Session (Redis-backed), MySQL  
**Package**: WAR deployed on Apache Tomcat 9  
**Target Platform**: AWS EKS (Elastic Kubernetes Service)  
**Health Endpoint**: `GET /health` → `{"status":"UP","application":"Doctor-Patient-Portal"}`  
**Base Image**: `eclipse-temurin:8-jre` (runtime), `maven:3.8.6-openjdk-8-slim` (builder)

---

## Prerequisites

### Local Development
| Tool | Version | Purpose |
|------|---------|---------|
| Docker | 20.10+ | Container build & run |
| Docker Compose | 2.x | Local multi-service orchestration |
| Java JDK 8 | 1.8+ | Local compilation (optional) |
| Maven | 3.8+ | Local build (optional) |

### AWS EKS Deployment
| Tool | Version | Purpose |
|------|---------|---------|
| AWS CLI | 2.x | AWS authentication & ECR |
| kubectl | 1.27+ | Kubernetes cluster management |
| eksctl | 0.150+ | EKS cluster creation (optional) |

### AWS IAM Permissions Required
```
ecr:GetAuthorizationToken
ecr:BatchCheckLayerAvailability
ecr:GetDownloadUrlForLayer
ecr:BatchGetImage
ecr:CreateRepository
ecr:DescribeRepositories
ecr:PutImage
eks:DescribeCluster
eks:ListClusters
```

---

## Project Structure

```
Test-AI_Ag/
├── Dockerfile                    # Multi-stage build (maven:3.8.6-openjdk-8-slim → eclipse-temurin:8-jre + Tomcat 9)
├── docker-compose.yml            # Local development stack (application only)
├── .dockerignore                 # Docker build exclusions
├── pom.xml                       # Maven build descriptor
├── src/
│   └── main/
│       ├── java/com/hms/         # Application source code
│       └── webapp/               # JSP pages, WEB-INF/web.xml
├── kubernetes/
│   ├── namespace.yaml            # Kubernetes namespace
│   ├── deployment.yaml           # Application deployment (2 replicas)
│   ├── service.yaml              # ClusterIP service (port 80 → 8080)
│   └── ingress.yaml              # AWS ALB Ingress
├── scripts/
│   ├── build-push.sh             # Linux/macOS build & push
│   ├── build-push.bat            # Windows build & push
│   ├── deploy-image.sh           # Linux/macOS EKS deploy
│   └── deploy-image.bat          # Windows EKS deploy
└── docs/
    └── DEPLOYMENT.md             # This file
```

---

## Local Development with Docker Compose

### 1. Configure Environment Variables

Create a `.env` file in the project root:

```env
# MySQL connection (provide your own MySQL instance)
DB_HOST=your-mysql-host
DB_PORT=3306
DB_NAME=hospital
DB_USER=root
DB_PASSWORD=your-password

# Redis connection (provide your own Redis instance)
REDIS_HOST=your-redis-host
REDIS_PORT=6379
```

> **Note**: The `docker-compose.yml` contains only the application container.  
> MySQL and Redis must be provided externally (local install, Docker, or cloud service).

### 2. Build and Start

```bash
# Build the image and start the container
docker-compose up --build

# Run in background
docker-compose up --build -d

# View logs
docker-compose logs -f doctor-patient-portal

# Stop
docker-compose down
```

### 3. Verify

```bash
# Health check
curl http://localhost:8080/health
# Expected: {"status":"UP","application":"Doctor-Patient-Portal"}

# Application
open http://localhost:8080
```

---

## Building and Pushing the Docker Image

### Linux / macOS

```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

The script will prompt you to:
1. Choose registry (AWS ECR or Docker Hub)
2. Enter registry credentials / details
3. Enter an image tag (defaults to `latest`)

### Windows

```cmd
scripts\build-push.bat
```

### Manual Build (ECR example)

```bash
# Authenticate
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# Create repository (first time only)
aws ecr create-repository --repository-name doctor-patient-portal --region us-east-1

# Build
docker build -t 123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest .

# Push
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest
```

---

## AWS EKS Deployment

### Step 1: Create / Configure EKS Cluster

```bash
# Create a new cluster (if needed)
eksctl create cluster \
  --name doctor-patient-portal-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4

# Configure kubectl
aws eks update-kubeconfig \
  --region us-east-1 \
  --name doctor-patient-portal-cluster
```

### Step 2: Install AWS Load Balancer Controller

The Ingress resource uses the AWS Load Balancer Controller (ALB Ingress Controller).

```bash
# Add the EKS chart repo
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install the controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=doctor-patient-portal-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

> See [AWS Load Balancer Controller docs](https://kubernetes-sigs.github.io/aws-load-balancer-controller/) for full IAM setup.

### Step 3: Run the Deployment Script

**Linux / macOS:**
```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

**Windows:**
```cmd
scripts\deploy-image.bat
```

The script will prompt for:
- AWS region
- EKS cluster name
- Full Docker image URI (e.g. `123456789012.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest`)
- Database connection details (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD)
- Redis / ElastiCache connection details (REDIS_HOST, REDIS_PORT)

### Step 4: Manual Deployment (Alternative)

```bash
# 1. Update deployment.yaml placeholders
sed -i 's|{{IMAGE_URI}}|YOUR_IMAGE_URI|g' kubernetes/deployment.yaml
sed -i 's|{{DB_HOST}}|your-rds-endpoint|g' kubernetes/deployment.yaml
sed -i 's|{{DB_PORT}}|3306|g' kubernetes/deployment.yaml
sed -i 's|{{DB_NAME}}|hospital|g' kubernetes/deployment.yaml
sed -i 's|{{DB_USER}}|admin|g' kubernetes/deployment.yaml
sed -i 's|{{DB_PASSWORD}}|your-password|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_HOST}}|your-elasticache-endpoint|g' kubernetes/deployment.yaml
sed -i 's|{{REDIS_PORT}}|6379|g' kubernetes/deployment.yaml

# 2. Apply manifests in order
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# 3. Wait for rollout
kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal

# 4. Get application URL
kubectl get ingress -n doctor-patient-portal
```

### Step 5: Verify Deployment

```bash
# Check pods
kubectl get pods -n doctor-patient-portal

# Check services
kubectl get svc -n doctor-patient-portal

# Check ingress / ALB
kubectl get ingress -n doctor-patient-portal

# View pod logs
kubectl logs -l app=doctor-patient-portal -n doctor-patient-portal --tail=100

# Health check via port-forward
kubectl port-forward svc/doctor-patient-portal-service 8080:80 -n doctor-patient-portal
curl http://localhost:8080/health
```

---

## Configuration Management

### Environment Variables Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | MySQL / Amazon RDS endpoint |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `hospital` | Database name |
| `DB_USER` | `root` | Database username |
| `DB_PASSWORD` | `changeme` | Database password |
| `REDIS_HOST` | `localhost` | Redis / ElastiCache primary endpoint |
| `REDIS_PORT` | `6379` | Redis port |
| `JAVA_OPTS` | `-Xms256m -Xmx512m ...` | JVM tuning flags |
| `TZ` | `UTC` | Container timezone |

### Using Kubernetes Secrets (Recommended for Production)

```bash
# Create a secret for sensitive values
kubectl create secret generic doctor-patient-portal-secrets \
  --from-literal=DB_PASSWORD=your-db-password \
  -n doctor-patient-portal

# Reference in deployment.yaml
# env:
#   - name: DB_PASSWORD
#     valueFrom:
#       secretKeyRef:
#         name: doctor-patient-portal-secrets
#         key: DB_PASSWORD
```

### Amazon RDS Setup

```bash
# Create RDS MySQL instance (example)
aws rds create-db-instance \
  --db-instance-identifier doctor-patient-portal-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0 \
  --master-username admin \
  --master-user-password your-password \
  --db-name hospital \
  --allocated-storage 20
```

### Amazon ElastiCache (Redis) Setup

```bash
# Create ElastiCache Redis cluster
aws elasticache create-cache-cluster \
  --cache-cluster-id doctor-patient-portal-redis \
  --cache-node-type cache.t3.micro \
  --engine redis \
  --num-cache-nodes 1
```

---

## Scaling and Management

### Horizontal Scaling

```bash
# Scale manually
kubectl scale deployment doctor-patient-portal \
  --replicas=4 -n doctor-patient-portal

# Configure Horizontal Pod Autoscaler
kubectl autoscale deployment doctor-patient-portal \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n doctor-patient-portal
```

### Rolling Updates

```bash
# Update image
kubectl set image deployment/doctor-patient-portal \
  doctor-patient-portal=NEW_IMAGE_URI \
  -n doctor-patient-portal

# Monitor rollout
kubectl rollout status deployment/doctor-patient-portal -n doctor-patient-portal
```

### Rollback

```bash
# Rollback to previous version
kubectl rollout undo deployment/doctor-patient-portal -n doctor-patient-portal

# Rollback to specific revision
kubectl rollout history deployment/doctor-patient-portal -n doctor-patient-portal
kubectl rollout undo deployment/doctor-patient-portal \
  --to-revision=2 -n doctor-patient-portal
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Describe pod for events
kubectl describe pod -l app=doctor-patient-portal -n doctor-patient-portal

# Check logs
kubectl logs -l app=doctor-patient-portal -n doctor-patient-portal --previous
```

**Common causes:**
- `ImagePullBackOff` → ECR authentication issue or wrong image URI
- `CrashLoopBackOff` → Application startup failure; check logs for DB/Redis connection errors
- `OOMKilled` → Increase memory limits in `deployment.yaml`

### Health Check Failing

```bash
# Port-forward and test health endpoint
kubectl port-forward svc/doctor-patient-portal-service 8080:80 -n doctor-patient-portal
curl -v http://localhost:8080/health
```

Expected response: `{"status":"UP","application":"Doctor-Patient-Portal"}`

### Database Connection Issues

```bash
# Verify DB_HOST is reachable from pod
kubectl exec -it $(kubectl get pod -l app=doctor-patient-portal \
  -n doctor-patient-portal -o jsonpath='{.items[0].metadata.name}') \
  -n doctor-patient-portal -- sh -c "nc -zv $DB_HOST $DB_PORT"
```

### Redis Connection Issues

```bash
# Verify Redis is reachable
kubectl exec -it $(kubectl get pod -l app=doctor-patient-portal \
  -n doctor-patient-portal -o jsonpath='{.items[0].metadata.name}') \
  -n doctor-patient-portal -- sh -c "nc -zv $REDIS_HOST $REDIS_PORT"
```

### Ingress / ALB Not Provisioning

```bash
# Check ALB controller logs
kubectl logs -n kube-system \
  -l app.kubernetes.io/name=aws-load-balancer-controller

# Verify ingress annotations
kubectl describe ingress doctor-patient-portal-ingress -n doctor-patient-portal
```

---

## Security Considerations

1. **Non-root container**: The application runs as the `tomcat` user (UID 1000), not root.
2. **Secrets management**: Use Kubernetes Secrets or AWS Secrets Manager for DB/Redis credentials.
3. **Network policies**: Restrict pod-to-pod traffic using Kubernetes NetworkPolicy.
4. **Image scanning**: Enable ECR image scanning to detect vulnerabilities.
5. **IRSA (IAM Roles for Service Accounts)**: Use IRSA for fine-grained AWS permissions instead of node-level IAM roles.
6. **TLS termination**: Configure HTTPS on the ALB by adding the `alb.ingress.kubernetes.io/certificate-arn` annotation.
7. **Resource limits**: CPU and memory limits are set to prevent noisy-neighbour issues.
8. **Read-only filesystem**: Consider adding `readOnlyRootFilesystem: true` to the security context after validating Tomcat temp directory requirements.

---

## Java-Specific Notes

- **Base image**: Runtime uses `eclipse-temurin:8-jre` (explicitly specified); builder uses `maven:3.8.6-openjdk-8-slim`.
- **JVM flags**: `UseContainerSupport` and `MaxRAMPercentage=75.0` ensure the JVM respects container memory limits.
- **Startup time**: Tomcat + Spring Session initialisation takes ~30–60 seconds; liveness probe `initialDelaySeconds` is set to 60 accordingly.
- **Session persistence**: Spring Session with Redis ensures sessions survive pod restarts and work correctly across multiple replicas.
- **WAR deployment**: The WAR is deployed as `ROOT.war` so the application is accessible at the context root `/`.
- **Java 8 EOL**: Consider upgrading to Java 11 or 17 for long-term support and improved container awareness.
- **Build tool**: Maven system command (`mvn`) is used exclusively; Maven wrapper (`mvnw`) is excluded from Docker builds via `.dockerignore`.
