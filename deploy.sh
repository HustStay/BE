#!/bin/bash
# Deploy script for HustStay Backend to Google Cloud Run
set -e

# Configuration
PROJECT_ID=${PROJECT_ID:-"huststay-datn"}
REGION=${REGION:-"asia-southeast1"}
REPO="$REGION-docker.pkg.dev/$PROJECT_ID/huststay-repo"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Services in deployment order
SERVICES=(
  "eureka-server"
  "user-service"
  "hotel-service"
  "room-service"
  "booking-service"
  "payment-service"
  "review-service"
  "chat-service"
  "api-gateway"
)

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  HustStay GCP Deployment Script${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""
echo "Project ID: $PROJECT_ID"
echo "Region: $REGION"
echo "Repository: $REPO"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI is not installed${NC}"
    exit 1
fi

# Check if docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

# Function to build and push a service
build_and_push() {
    local service=$1
    echo -e "${YELLOW}Building $service...${NC}"
    
    if [ ! -d "$service" ]; then
        echo -e "${RED}Error: Directory $service not found${NC}"
        return 1
    fi
    
    cd "$service"
    docker build -t "$REPO/$service:latest" .
    docker push "$REPO/$service:latest"
    cd ..
    
    echo -e "${GREEN}✓ $service built and pushed successfully${NC}"
}

# Main deployment
case "${1:-build}" in
    "build")
        echo -e "${YELLOW}Building and pushing all services...${NC}"
        echo ""
        
        for service in "${SERVICES[@]}"; do
            build_and_push "$service"
            echo ""
        done
        
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN}  All images built and pushed!${NC}"
        echo -e "${GREEN}========================================${NC}"
        ;;
        
    "deploy")
        echo -e "${YELLOW}Deploying services to Cloud Run...${NC}"
        echo ""
        
        # Get Eureka URL first
        EUREKA_URL=$(gcloud run services describe eureka-server --region=$REGION --format='value(status.url)' 2>/dev/null || echo "")
        
        if [ -z "$EUREKA_URL" ]; then
            echo -e "${RED}Eureka server not deployed yet. Deploy eureka-server first.${NC}"
            exit 1
        fi
        
        echo "Eureka URL: $EUREKA_URL"
        
        # Deploy services (example for user-service)
        echo -e "${YELLOW}Deploying user-service...${NC}"
        gcloud run deploy user-service \
            --image="$REPO/user-service:latest" \
            --platform=managed \
            --region=$REGION \
            --port=8080 \
            --memory=512Mi \
            --set-env-vars="SPRING_PROFILES_ACTIVE=prod" \
            --set-env-vars="EUREKA_URL=$EUREKA_URL/eureka/" \
            --allow-unauthenticated
            
        echo -e "${GREEN}✓ Deployment completed${NC}"
        ;;
        
    "eureka")
        echo -e "${YELLOW}Deploying Eureka Server...${NC}"
        build_and_push "eureka-server"
        
        gcloud run deploy eureka-server \
            --image="$REPO/eureka-server:latest" \
            --platform=managed \
            --region=$REGION \
            --port=8761 \
            --memory=512Mi \
            --min-instances=1 \
            --max-instances=1 \
            --allow-unauthenticated
            
        echo -e "${GREEN}✓ Eureka Server deployed${NC}"
        ;;
        
    *)
        echo "Usage: $0 {build|deploy|eureka}"
        echo ""
        echo "Commands:"
        echo "  build   - Build and push all Docker images"
        echo "  deploy  - Deploy services to Cloud Run"
        echo "  eureka  - Deploy only Eureka Server"
        exit 1
        ;;
esac
