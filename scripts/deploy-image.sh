#!/bin/bash
set -e
set -o pipefail

# =============================================================
# deploy-image.sh – Deploy Doctor-Patient-Portal to AWS EKS
# =============================================================

APP_NAME="doctor-patient-portal"
NAMESPACE="doctor-patient-portal"
K8S_DIR="kubernetes"

echo "=============================================="
echo "  Doctor-Patient-Portal – EKS Deployment"
echo "=============================================="

# ---- Collect deployment parameters ----
read -rp "Enter AWS region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
  echo "ERROR: AWS region is required."
  exit 1
fi

read -rp "Enter EKS cluster name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
  echo "ERROR: EKS cluster name is required."
  exit 1
fi

read -rp "Enter full Docker image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/doctor-patient-portal:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
  echo "ERROR: Docker image URI is required."
  exit 1
fi

echo ""
echo "---- Optional: Application Environment Variables ----"
echo "(Press Enter to skip any variable)"

read -rp "Enter DB_HOST (MySQL/RDS endpoint): " DB_HOST
read -rp "Enter DB_PORT (default: 3306): " DB_PORT
read -rp "Enter DB_NAME (default: hospital): " DB_NAME
read -rp "Enter DB_USER: " DB_USER
read -rp "Enter DB_PASSWORD: " DB_PASSWORD
read -rp "Enter REDIS_HOST (ElastiCache endpoint): " REDIS_HOST
read -rp "Enter REDIS_PORT (default: 6379): " REDIS_PORT

# Apply defaults
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-hospital}"
REDIS_PORT="${REDIS_PORT:-6379}"

echo ""
echo "Configuring kubectl for cluster: $CLUSTER_NAME in $AWS_REGION ..."
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to EKS cluster."; exit 1; }

echo ""
echo "Updating Kubernetes manifests with deployment values..."

# Work on copies so originals stay as templates
cp -r "$K8S_DIR" /tmp/k8s-deploy-$$

# Replace placeholders using pipe delimiter
sed -i "s|{{IMAGE_URI}}|${IMAGE_URI}|g"       /tmp/k8s-deploy-$$/deployment.yaml
sed -i "s|{{DB_HOST}}|${DB_HOST}|g"           /tmp/k8s-deploy-$$/deployment.yaml
sed -i "s|{{DB_PORT}}|${DB_PORT}|g"           /tmp/k8s-deploy-$$/deployment.yaml
sed -i "s|{{DB_NAME}}|${DB_NAME}|g"           /tmp/k8s-deploy-$$/deployment.yaml
sed -i "s|{{DB_USER}}|${DB_USER}|g"           /tmp/k8s-deploy-$$/deployment.yaml
sed -i "s|{{DB_PASSWORD}}|${DB_PASSWORD}|g"   /tmp/k8s-deploy-$$/deployment.yaml
sed -i "s|{{REDIS_HOST}}|${REDIS_HOST}|g"     /tmp/k8s-deploy-$$/deployment.yaml
sed -i "s|{{REDIS_PORT}}|${REDIS_PORT}|g"     /tmp/k8s-deploy-$$/deployment.yaml

echo ""
echo "Applying Kubernetes manifests..."

echo "  [1/4] Applying namespace..."
kubectl apply -f /tmp/k8s-deploy-$$/namespace.yaml

echo "  [2/4] Applying deployment..."
kubectl apply -f /tmp/k8s-deploy-$$/deployment.yaml

echo "  [3/4] Applying service..."
kubectl apply -f /tmp/k8s-deploy-$$/service.yaml

echo "  [4/4] Applying ingress..."
kubectl apply -f /tmp/k8s-deploy-$$/ingress.yaml

# Clean up temp copies
rm -rf /tmp/k8s-deploy-$$

echo ""
echo "Waiting for deployment rollout..."
kubectl rollout status deployment/"$APP_NAME" -n "$NAMESPACE" --timeout=300s

echo ""
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n "$NAMESPACE"

echo ""
echo "Fetching application URL..."
INGRESS_HOST=$(kubectl get ingress "${APP_NAME}-ingress" -n "$NAMESPACE" \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")

echo ""
echo "=============================================="
echo "  Deployment complete!"
echo "  Application URL: http://${INGRESS_HOST}"
echo "  Namespace      : $NAMESPACE"
echo "  Image          : $IMAGE_URI"
echo "=============================================="
echo ""
echo "Rollback command (if needed):"
echo "  kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE"
