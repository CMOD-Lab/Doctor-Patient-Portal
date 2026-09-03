#!/bin/bash
set -e
set -o pipefail

APP_NAME="docportal"
NAMESPACE="docportal"

echo "============================================"
echo "  DocPortal - Deploy to AWS EKS"
echo "============================================"
echo ""

# Prompt for AWS configuration
read -p "Enter AWS Region (e.g. us-east-1): " AWS_REGION
if [ -z "$AWS_REGION" ]; then
    echo "ERROR: AWS Region is required."
    exit 1
fi

read -p "Enter EKS Cluster Name: " CLUSTER_NAME
if [ -z "$CLUSTER_NAME" ]; then
    echo "ERROR: EKS Cluster Name is required."
    exit 1
fi

read -p "Enter Docker Image URI (e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com/docportal:latest): " IMAGE_URI
if [ -z "$IMAGE_URI" ]; then
    echo "ERROR: Docker Image URI is required."
    exit 1
fi

echo ""
echo "--- Application Environment Variables ---"
echo "Press Enter to skip any variable (placeholder will remain in manifest)."
echo ""

read -p "Enter DB_HOST (MySQL host, e.g. mysql-service or RDS endpoint): " DB_HOST
read -p "Enter DB_PORT (default: 3306): " DB_PORT
DB_PORT="${DB_PORT:-3306}"
read -p "Enter DB_NAME (default: hospital): " DB_NAME
DB_NAME="${DB_NAME:-hospital}"
read -p "Enter DB_USER (default: root): " DB_USER
DB_USER="${DB_USER:-root}"
read -sp "Enter DB_PASSWORD: " DB_PASSWORD
echo ""

echo ""
echo "Configuring kubectl for EKS cluster: $CLUSTER_NAME in $AWS_REGION ..."
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

echo "Verifying cluster connectivity..."
kubectl cluster-info || { echo "ERROR: Cannot connect to EKS cluster."; exit 1; }

echo ""
echo "Updating Kubernetes manifests with provided values..."

# Work on copies to avoid modifying originals permanently
cp kubernetes/deployment.yaml kubernetes/deployment.yaml.bak

sed -i 's|{{IMAGE_URI}}|'"$IMAGE_URI"'|g' kubernetes/deployment.yaml

if [ -n "$DB_HOST" ]; then
    sed -i 's|{{DB_HOST}}|'"$DB_HOST"'|g' kubernetes/deployment.yaml
fi
if [ -n "$DB_PORT" ]; then
    sed -i 's|{{DB_PORT}}|'"$DB_PORT"'|g' kubernetes/deployment.yaml
fi
if [ -n "$DB_NAME" ]; then
    sed -i 's|{{DB_NAME}}|'"$DB_NAME"'|g' kubernetes/deployment.yaml
fi
if [ -n "$DB_USER" ]; then
    sed -i 's|{{DB_USER}}|'"$DB_USER"'|g' kubernetes/deployment.yaml
fi
if [ -n "$DB_PASSWORD" ]; then
    sed -i 's|{{DB_PASSWORD}}|'"$DB_PASSWORD"'|g' kubernetes/deployment.yaml
fi

echo ""
echo "Applying Kubernetes manifests..."

echo "  [1/4] Applying namespace..."
kubectl apply -f kubernetes/namespace.yaml

echo "  [2/4] Applying deployment..."
kubectl apply -f kubernetes/deployment.yaml

echo "  [3/4] Applying service..."
kubectl apply -f kubernetes/service.yaml

echo "  [4/4] Applying ingress..."
kubectl apply -f kubernetes/ingress.yaml

echo ""
echo "Waiting for deployment rollout..."
kubectl rollout status deployment/"$APP_NAME" -n "$NAMESPACE" --timeout=300s

echo ""
echo "Verifying deployed resources..."
kubectl get pods,svc,ingress -n "$NAMESPACE"

echo ""
echo "Retrieving application URL..."
INGRESS_HOST=$(kubectl get ingress docportal-ingress -n "$NAMESPACE" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
if [ "$INGRESS_HOST" != "pending" ] && [ -n "$INGRESS_HOST" ]; then
    echo "Application URL: http://$INGRESS_HOST"
else
    echo "Ingress hostname is still provisioning. Run the following to check:"
    echo "  kubectl get ingress -n $NAMESPACE"
fi

# Restore original deployment.yaml
mv kubernetes/deployment.yaml.bak kubernetes/deployment.yaml 2>/dev/null || true

echo ""
echo "============================================"
echo "  Deployment Complete!"
echo "  Namespace : $NAMESPACE"
echo "  App       : $APP_NAME"
echo "  Image     : $IMAGE_URI"
echo "============================================"
echo ""
echo "Rollback command (if needed):"
echo "  kubectl rollout undo deployment/$APP_NAME -n $NAMESPACE"
