@echo off
REM Deploy script for HustStay Backend to Google Cloud Run (Windows)

SET PROJECT_ID=huststay-datn
SET REGION=asia-southeast1
SET REPO=%REGION%-docker.pkg.dev/%PROJECT_ID%/huststay-repo

echo ========================================
echo   HustStay GCP Deployment Script
echo ========================================
echo.
echo Project ID: %PROJECT_ID%
echo Region: %REGION%
echo Repository: %REPO%
echo.

REM Check if gcloud is installed
where gcloud >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
    echo Error: gcloud CLI is not installed
    exit /b 1
)

REM Check if docker is installed
where docker >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
    echo Error: Docker is not installed
    exit /b 1
)

IF "%1"=="build" GOTO BUILD
IF "%1"=="eureka" GOTO EUREKA
IF "%1"=="" GOTO BUILD
GOTO USAGE

:BUILD
echo Building and pushing all services...
echo.

FOR %%s IN (eureka-server user-service hotel-service room-service booking-service payment-service review-service chat-service api-gateway) DO (
    echo Building %%s...
    cd %%s
    docker build -t %REPO%/%%s:latest .
    docker push %REPO%/%%s:latest
    cd ..
    echo %%s built and pushed successfully
    echo.
)

echo ========================================
echo   All images built and pushed!
echo ========================================
GOTO END

:EUREKA
echo Deploying Eureka Server...
cd eureka-server
docker build -t %REPO%/eureka-server:latest .
docker push %REPO%/eureka-server:latest
cd ..

gcloud run deploy eureka-server ^
    --image=%REPO%/eureka-server:latest ^
    --platform=managed ^
    --region=%REGION% ^
    --port=8761 ^
    --memory=512Mi ^
    --min-instances=1 ^
    --max-instances=1 ^
    --allow-unauthenticated

echo Eureka Server deployed!
GOTO END

:USAGE
echo Usage: deploy.bat {build^|eureka}
echo.
echo Commands:
echo   build   - Build and push all Docker images
echo   eureka  - Deploy only Eureka Server
GOTO END

:END
