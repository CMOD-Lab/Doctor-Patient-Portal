# DocPortal - Deployment Guide

## Overview

**Application**: DocPortal (Doctor-Patient Portal)  
**Technology**: Java 8, Maven, Servlet/JSP (WAR), Apache Tomcat 9  
**Target Platform**: AWS EKS (Elastic Kubernetes Service)  
**Container Registry**: AWS ECR or Docker Hub  
**Health Endpoint**: `GET /health`  
**Application Port**: `8080`

---

## Prerequisites

### Local Development
- Docker Desktop 20.10+
- Docker Compose 2.x
- Java 8 JDK
- Maven 3.8+

### AWS EKS Deployment
- AWS CLI v2 configured (`aws configure`)
- `kubectl` 1.24+
- `eksctl` (optional, for cluster creation)
- IAM permissions: `ecr:*`, `eks:*`, `ec2:*`, `iam:PassRole`
- AWS Load Balancer Controller installed on EKS cluster

---

## Project Structure

```
DocPortal/
├── Dockerfile                  # Multi-stage Docker build
├── docker-compose.yml          # Local development compose
├── .dockerignore               # Docker build exclusions
├── pom.xml                     # Maven build descriptor
├── src/
│   └── main/
│       ├── java/com/hms/       # Java source code
│       └── webapp/             # JSP pages and web resources
├── kubernetes/
│   ├── namespace.yaml          # Kubernetes namespace
│   ├── deployment.yaml         # Application deployment
│   ├── service.yaml            # ClusterIP service
│   └── ingress.yaml            # AWS ALB ingress
├── scripts/
│   ├── build-push.sh           # Linux/macOS build & push
│   ├── build-push.bat          # Windows build & push
│   ├── deploy-image.sh         # Linux/macOS EKS deploy
│   └── deploy-image.bat        # Windows EKS deploy
└── docs/
    └── DEPLOYMENT.md           # This file
```

---

## Local Development with Docker Compose

### 1. Configure Environment Variables

Create a `.env` file in the project root:

```env
DB_HOST=<your-mysql-host>
DB_PORT=3306
DB_NAME=hospital
DB_USER=root
DB_PASSWORD=<your-password>
```

> **Note**: The application requires an external MySQL database. Ensure your MySQL instance is accessible from the container.

### 2. Build and Start the Application

```bash
docker-compose up --build
```

### 3. Access the Application

- Application: [http://localhost:8080](http://localhost:8080)
- Health Check: [http://localhost:8080/health](http://localhost:8080/health)

### 4. Stop the Application

```bash
docker-compose down
```

---

## Building the Docker Image

### Build Locally

```bash
docker build -t docportal:latest .
```

### Build and Push to Registry

**Linux/macOS:**
```bash
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

**Windows:**
```cmd
scripts\build-push.bat
```

The script will prompt you to:
1. Select registry type (AWS ECR or Docker Hub)
2. Enter registry credentials/details
3. Specify an image tag (defaults to `latest`)

---

## AWS EKS Deployment

### Step 1: AWS CLI Configuration

```bash
aws configure
# Enter: AWS Access Key ID, Secret Access Key, Region, Output format
```

### Step 2: Configure kubectl for EKS

```bash
aws eks update-kubeconfig --region <AWS_REGION> --name <CLUSTER_NAME>
kubectl cluster-info
```

### Step 3: Install AWS Load Balancer Controller (if not installed)

```bash
# Add the EKS chart repo
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Install the controller
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=<CLUSTER_NAME> \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### Step 4: Build and Push Docker Image

```bash
./scripts/build-push.sh
# Select AWS ECR, enter region and account ID
# Note the full image URI output (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/docportal:latest)
```

### Step 5: Deploy to EKS

**Linux/macOS:**
```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

**Windows:**
```cmd
scripts\deploy-image.bat
```

The deploy script will prompt for:
- AWS Region
- EKS Cluster Name
- Docker Image URI (full path with tag)
- Database connection details (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD)

### Step 6: Verify Deployment

```bash
# Check all resources in the namespace
kubectl get all -n docportal

# Check pod logs
kubectl logs -l app=docportal -n docportal --tail=100

# Check ingress and get ALB hostname
kubectl get ingress -n docportal
```

---

## Kubernetes Manifest Details

### namespace.yaml
Creates the `docportal` Kubernetes namespace to isolate all application resources.

### deployment.yaml
- **Replicas**: 2 (high availability)
- **Image**: Pulled from `{{IMAGE_URI}}` (replaced at deploy time)
- **Resources**: 250m CPU / 512Mi memory (requests), 500m CPU / 1Gi memory (limits)
- **Liveness Probe**: `GET /health` — restarts container if unhealthy
- **Readiness Probe**: `GET /health` — removes pod from load balancer if not ready
- **Environment Variables**: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD

### service.yaml
- **Type**: ClusterIP — internal cluster access only
- **Port**: 80 → 8080 (container port)

### ingress.yaml
- **Class**: AWS ALB (Application Load Balancer)
- **Scheme**: internet-facing
- **Host**: `docportal.example.com` (update to your actual domain)
- **Health Check Path**: `/health`

---

## Configuration Management

### Environment Variables

| Variable     | Description                  | Default     |
|--------------|------------------------------|-------------|
| DB_HOST      | MySQL database hostname      | localhost   |
| DB_PORT      | MySQL database port          | 3306        |
| DB_NAME      | MySQL database name          | hospital    |
| DB_USER      | MySQL database username      | root        |
| DB_PASSWORD  | MySQL database password      | (empty)     |
| JAVA_OPTS    | JVM options                  | -Xmx512m... |
| TZ           | Timezone                     | UTC         |

### Using Kubernetes Secrets for Sensitive Data

For production, store database credentials as Kubernetes Secrets:

```bash
kubectl create secret generic docportal-db-secret \
  --from-literal=DB_USER=myuser \
  --from-literal=DB_PASSWORD=mypassword \
  -n docportal
```

Then reference in `deployment.yaml`:
```yaml
env:
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: docportal-db-secret
        key: DB_PASSWORD
```

---

## Scaling and Management

### Manual Scaling

```bash
kubectl scale deployment docportal --replicas=3 -n docportal
```

### Horizontal Pod Autoscaler (HPA)

```bash
kubectl autoscale deployment docportal \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n docportal
```

### Rolling Update

```bash
# Update image
kubectl set image deployment/docportal \
  docportal=<NEW_IMAGE_URI> \
  -n docportal

# Monitor rollout
kubectl rollout status deployment/docportal -n docportal
```

### Rollback

```bash
kubectl rollout undo deployment/docportal -n docportal
```

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -n docportal

# Describe pod for events
kubectl describe pod <pod-name> -n docportal

# Check container logs
kubectl logs <pod-name> -n docportal
```

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `CrashLoopBackOff` | App fails to start | Check logs; verify DB_HOST is reachable |
| `ImagePullBackOff` | Cannot pull image | Verify ECR permissions and image URI |
| `Pending` pods | Insufficient resources | Check node capacity with `kubectl describe nodes` |
| Health check failing | DB not reachable | Verify DB_HOST, DB_PORT, DB_USER, DB_PASSWORD |
| Ingress not getting hostname | ALB controller not installed | Install AWS Load Balancer Controller |

### Database Connectivity

```bash
# Test DB connectivity from within a pod
kubectl exec -it <pod-name> -n docportal -- /bin/sh
# Then test: nc -zv $DB_HOST $DB_PORT
```

### View Application Logs

```bash
# Stream logs from all pods
kubectl logs -f -l app=docportal -n docportal

# Previous container logs (after crash)
kubectl logs <pod-name> -n docportal --previous
```

---

## Security Considerations

1. **Non-root container**: The application runs as `appuser` (non-root) inside the container.
2. **Secrets management**: Use Kubernetes Secrets or AWS Secrets Manager for DB credentials.
3. **Network policies**: Apply Kubernetes NetworkPolicies to restrict pod-to-pod communication.
4. **Image scanning**: Enable ECR image scanning to detect vulnerabilities.
5. **HTTPS**: Configure SSL/TLS termination at the ALB level using ACM certificates.
6. **IAM roles**: Use IRSA (IAM Roles for Service Accounts) for fine-grained AWS permissions.

### Enable HTTPS on ALB Ingress

Add annotations to `ingress.yaml`:
```yaml
annotations:
  alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
  alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:<region>:<account>:certificate/<cert-id>
  alb.ingress.kubernetes.io/ssl-redirect: '443'
```

---

## Java-Specific Notes

### JVM Configuration

The application uses the following JVM flags (set via `JAVA_OPTS`):
- `-Xmx512m` — Maximum heap size
- `-Xms256m` — Initial heap size
- `-XX:+UseContainerSupport` — Enables container-aware memory management
- `-XX:MaxRAMPercentage=75.0` — Use up to 75% of container memory for heap
- `-XX:+UnlockExperimentalVMOptions` — Enables experimental JVM features

### Tomcat Configuration

The application is deployed as a WAR file to Apache Tomcat 9 running on port 8080. The WAR is deployed as `ROOT.war` so the application is accessible at the root context path (`/`).

### Health Check Endpoint

The application exposes a custom health endpoint at `GET /health` implemented by `HealthCheckServlet`. This returns:
```json
{"status":"UP","application":"Doctor-Patient-Portal"}
```

This endpoint is used by both Kubernetes liveness and readiness probes.

---

## Cleanup

```bash
# Remove all application resources
kubectl delete namespace docportal

# Remove ECR repository (optional)
aws ecr delete-repository --repository-name docportal --region <AWS_REGION> --force
```
