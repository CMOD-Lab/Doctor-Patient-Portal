@echo off
setlocal enabledelayedexpansion

:: =============================================================
:: deploy-image.bat – Deploy Doctor-Patient-Portal to AWS EKS
:: =============================================================

set "APP_NAME=doctor-patient-portal"
set "NAMESPACE=doctor-patient-portal"
set "K8S_DIR=kubernetes"

echo ==============================================
echo   Doctor-Patient-Portal - EKS Deployment
echo ==============================================

:: ---- Collect deployment parameters ----
set /p AWS_REGION="Enter AWS region (e.g. us-east-1): "
if "!AWS_REGION!"=="" (
    echo ERROR: AWS region is required.
    exit /b 1
)

set /p CLUSTER_NAME="Enter EKS cluster name: "
if "!CLUSTER_NAME!"=="" (
    echo ERROR: EKS cluster name is required.
    exit /b 1
)

set /p IMAGE_URI="Enter full Docker image URI: "
if "!IMAGE_URI!"=="" (
    echo ERROR: Docker image URI is required.
    exit /b 1
)

echo.
echo ---- Optional: Application Environment Variables ----
echo (Press Enter to skip any variable)

set /p DB_HOST="Enter DB_HOST (MySQL/RDS endpoint): "
set /p DB_PORT="Enter DB_PORT (default: 3306): "
set /p DB_NAME="Enter DB_NAME (default: hospital): "
set /p DB_USER="Enter DB_USER: "
set /p DB_PASSWORD="Enter DB_PASSWORD: "
set /p REDIS_HOST="Enter REDIS_HOST (ElastiCache endpoint): "
set /p REDIS_PORT="Enter REDIS_PORT (default: 6379): "

if "!DB_PORT!"=="" set "DB_PORT=3306"
if "!DB_NAME!"=="" set "DB_NAME=hospital"
if "!REDIS_PORT!"=="" set "REDIS_PORT=6379"

echo.
echo Configuring kubectl for cluster: !CLUSTER_NAME! in !AWS_REGION! ...
aws eks update-kubeconfig --region !AWS_REGION! --name !CLUSTER_NAME!
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl.
    exit /b 1
)

echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to EKS cluster.
    exit /b 1
)

:: ---- Copy manifests to temp directory ----
set "TEMP_K8S=%TEMP%\k8s-deploy-%RANDOM%"
xcopy /E /I /Q "!K8S_DIR!" "!TEMP_K8S!" >nul

echo.
echo Updating Kubernetes manifests with deployment values...

:: Replace placeholders using PowerShell
powershell -Command "(Get-Content '!TEMP_K8S!\deployment.yaml') -replace '\{\{IMAGE_URI\}\}', '!IMAGE_URI!' -replace '\{\{DB_HOST\}\}', '!DB_HOST!' -replace '\{\{DB_PORT\}\}', '!DB_PORT!' -replace '\{\{DB_NAME\}\}', '!DB_NAME!' -replace '\{\{DB_USER\}\}', '!DB_USER!' -replace '\{\{DB_PASSWORD\}\}', '!DB_PASSWORD!' -replace '\{\{REDIS_HOST\}\}', '!REDIS_HOST!' -replace '\{\{REDIS_PORT\}\}', '!REDIS_PORT!' | Set-Content '!TEMP_K8S!\deployment.yaml'"
if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to update deployment manifest.
    exit /b 1
)

echo.
echo Applying Kubernetes manifests...

echo   [1/4] Applying namespace...
kubectl apply -f "!TEMP_K8S!\namespace.yaml"
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply namespace. & exit /b 1 )

echo   [2/4] Applying deployment...
kubectl apply -f "!TEMP_K8S!\deployment.yaml"
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply deployment. & exit /b 1 )

echo   [3/4] Applying service...
kubectl apply -f "!TEMP_K8S!\service.yaml"
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply service. & exit /b 1 )

echo   [4/4] Applying ingress...
kubectl apply -f "!TEMP_K8S!\ingress.yaml"
if !ERRORLEVEL! neq 0 ( echo ERROR: Failed to apply ingress. & exit /b 1 )

:: Clean up temp directory
rmdir /S /Q "!TEMP_K8S!"

echo.
echo Waiting for deployment rollout...
kubectl rollout status deployment/!APP_NAME! -n !NAMESPACE! --timeout=300s
if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed.
    echo Rollback command: kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!
    exit /b 1
)

echo.
echo Verifying deployed resources...
kubectl get pods,svc,ingress -n !NAMESPACE!

echo.
echo ==============================================
echo   Deployment complete!
echo   Namespace : !NAMESPACE!
echo   Image     : !IMAGE_URI!
echo ==============================================
echo.
echo Rollback command (if needed):
echo   kubectl rollout undo deployment/!APP_NAME! -n !NAMESPACE!

endlocal
