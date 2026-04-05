# 🚀 Hướng dẫn Deploy HustStay lên Google Cloud

## 📋 Mục lục
1. [Chuẩn bị](#1-chuẩn-bị)
2. [Thiết lập GCP](#2-thiết-lập-gcp)
3. [Tạo Cloud SQL](#3-tạo-cloud-sql)
4. [Dockerize Backend](#4-dockerize-backend)
5. [Deploy Backend](#5-deploy-backend-lên-cloud-run)
6. [Deploy Frontend](#6-deploy-frontend)
7. [Cấu hình Domain](#7-cấu-hình-domain--ssl)

---

## 1. Chuẩn bị

### 1.1 Cài đặt công cụ
```bash
# Cài đặt Google Cloud SDK
# Windows: Download từ https://cloud.google.com/sdk/docs/install

# Sau khi cài, chạy:
gcloud init
gcloud auth login
```

### 1.2 Cài đặt Docker Desktop
- Download từ: https://www.docker.com/products/docker-desktop/

---

## 2. Thiết lập GCP

### 2.1 Tạo Project
```bash
# Tạo project mới
gcloud projects create huststay-datn --name="HustStay DATN"

# Set project hiện tại
gcloud config set project huststay-datn

# Enable billing (cần làm trên Console)
# https://console.cloud.google.com/billing
```

### 2.2 Enable APIs cần thiết
```bash
gcloud services enable \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  sqladmin.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com
```

### 2.3 Tạo Artifact Registry (lưu Docker images)
```bash
gcloud artifacts repositories create huststay-repo \
  --repository-format=docker \
  --location=asia-southeast1 \
  --description="HustStay Docker images"
```

---

## 3. Tạo Cloud SQL

### 3.1 Tạo PostgreSQL instance
```bash
gcloud sql instances create huststay-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=asia-southeast1 \
  --root-password=YOUR_STRONG_PASSWORD \
  --storage-size=10GB \
  --storage-type=SSD
```

### 3.2 Tạo databases cho mỗi service
```bash
# Kết nối vào Cloud SQL
gcloud sql connect huststay-db --user=postgres

# Trong psql, tạo databases:
CREATE DATABASE user_service;
CREATE DATABASE hotel_service;
CREATE DATABASE room_service;
CREATE DATABASE booking_service;
CREATE DATABASE payment_service;
CREATE DATABASE review_service;
CREATE DATABASE chat_service;

# Tạo user
CREATE USER huststay WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE user_service TO huststay;
GRANT ALL PRIVILEGES ON DATABASE hotel_service TO huststay;
-- ... repeat cho các database khác
```

---

## 4. Dockerize Backend

### 4.1 Dockerfile mẫu cho Spring Boot service

Tạo file `Dockerfile` trong mỗi service folder:

```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.2 Tạo Dockerfiles cho tất cả services

**eureka-server/Dockerfile:**
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**api-gateway/Dockerfile:**
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.3 Cấu hình application-prod.yaml

Tạo `application-prod.yaml` trong mỗi service:

**user-service/src/main/resources/application-prod.yaml:**
```yaml
server:
  port: 8080

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:postgresql:///${DB_NAME}?cloudSqlInstance=${CLOUD_SQL_INSTANCE}&socketFactory=com.google.cloud.sql.postgres.SocketFactory
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://eureka-server:8761/eureka/}
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true

application:
  security:
    jwt:
      secret-key: ${JWT_SECRET}
      expiration: 1209600000
```

### 4.4 Thêm Cloud SQL dependency vào pom.xml
```xml
<dependency>
    <groupId>com.google.cloud.sql</groupId>
    <artifactId>postgres-socket-factory</artifactId>
    <version>1.15.0</version>
</dependency>
```

---

## 5. Deploy Backend lên Cloud Run

### 5.1 Build và push Docker images

```bash
# Authenticate Docker với GCP
gcloud auth configure-docker asia-southeast1-docker.pkg.dev

# Set variables
PROJECT_ID=huststay-datn
REGION=asia-southeast1
REPO=asia-southeast1-docker.pkg.dev/$PROJECT_ID/huststay-repo

# Build và push từng service
cd eureka-server
docker build -t $REPO/eureka-server:latest .
docker push $REPO/eureka-server:latest

cd ../api-gateway
docker build -t $REPO/api-gateway:latest .
docker push $REPO/api-gateway:latest

# Repeat cho các services khác...
```

### 5.2 Tạo secrets trong Secret Manager
```bash
# Tạo secrets
echo -n "your_db_password" | gcloud secrets create db-password --data-file=-
echo -n "your_jwt_secret" | gcloud secrets create jwt-secret --data-file=-
echo -n "sk_test_xxx" | gcloud secrets create stripe-secret-key --data-file=-
echo -n "pk_test_xxx" | gcloud secrets create stripe-publishable-key --data-file=-
```

### 5.3 Deploy Eureka Server (đầu tiên)
```bash
gcloud run deploy eureka-server \
  --image=$REPO/eureka-server:latest \
  --platform=managed \
  --region=asia-southeast1 \
  --port=8761 \
  --memory=512Mi \
  --min-instances=1 \
  --max-instances=1 \
  --allow-unauthenticated
```

### 5.4 Deploy các Microservices
```bash
# Lấy URL của Eureka Server
EUREKA_URL=$(gcloud run services describe eureka-server --region=asia-southeast1 --format='value(status.url)')

# Deploy user-service
gcloud run deploy user-service \
  --image=$REPO/user-service:latest \
  --platform=managed \
  --region=asia-southeast1 \
  --port=8080 \
  --memory=512Mi \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod" \
  --set-env-vars="EUREKA_URL=$EUREKA_URL/eureka/" \
  --set-env-vars="DB_NAME=user_service" \
  --set-env-vars="CLOUD_SQL_INSTANCE=$PROJECT_ID:asia-southeast1:huststay-db" \
  --set-secrets="DB_PASSWORD=db-password:latest" \
  --set-secrets="JWT_SECRET=jwt-secret:latest" \
  --add-cloudsql-instances=$PROJECT_ID:asia-southeast1:huststay-db \
  --allow-unauthenticated

# Deploy payment-service (có thêm Stripe secrets)
gcloud run deploy payment-service \
  --image=$REPO/payment-service:latest \
  --platform=managed \
  --region=asia-southeast1 \
  --port=8080 \
  --memory=512Mi \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod" \
  --set-env-vars="EUREKA_URL=$EUREKA_URL/eureka/" \
  --set-env-vars="DB_NAME=payment_service" \
  --set-env-vars="CLOUD_SQL_INSTANCE=$PROJECT_ID:asia-southeast1:huststay-db" \
  --set-secrets="DB_PASSWORD=db-password:latest" \
  --set-secrets="STRIPE_SECRET_KEY=stripe-secret-key:latest" \
  --set-secrets="STRIPE_PUBLISHABLE_KEY=stripe-publishable-key:latest" \
  --add-cloudsql-instances=$PROJECT_ID:asia-southeast1:huststay-db \
  --allow-unauthenticated

# Repeat cho các services khác...
```

### 5.5 Deploy API Gateway (cuối cùng)
```bash
gcloud run deploy api-gateway \
  --image=$REPO/api-gateway:latest \
  --platform=managed \
  --region=asia-southeast1 \
  --port=8080 \
  --memory=512Mi \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod" \
  --set-env-vars="EUREKA_URL=$EUREKA_URL/eureka/" \
  --set-secrets="JWT_SECRET=jwt-secret:latest" \
  --allow-unauthenticated

# Lấy URL API Gateway
API_GATEWAY_URL=$(gcloud run services describe api-gateway --region=asia-southeast1 --format='value(status.url)')
echo "API Gateway URL: $API_GATEWAY_URL"
```

---

## 6. Deploy Frontend

### 6.1 Cài đặt Firebase CLI
```bash
npm install -g firebase-tools
firebase login
```

### 6.2 Khởi tạo Firebase Hosting
```bash
cd d:/20252/DATN/FE/Customer
firebase init hosting

# Chọn:
# - Use existing project: huststay-datn
# - Public directory: dist
# - Single-page app: Yes
# - GitHub Actions: No
```

### 6.3 Cấu hình API URL cho Production

**Customer/.env.production:**
```env
VITE_API_URL=https://api-gateway-xxx-as.a.run.app
```

### 6.4 Build và Deploy
```bash
# Customer
cd d:/20252/DATN/FE/Customer
npm run build
firebase deploy --only hosting:customer

# Hotel
cd d:/20252/DATN/FE/Hotel
npm run build
firebase deploy --only hosting:hotel

# Admin
cd d:/20252/DATN/FE/Admin
npm run build
firebase deploy --only hosting:admin
```

---

## 7. Cấu hình Domain & SSL

### 7.1 Custom Domain cho Cloud Run
```bash
# Map custom domain
gcloud beta run domain-mappings create \
  --service=api-gateway \
  --domain=api.huststay.com \
  --region=asia-southeast1
```

### 7.2 Custom Domain cho Firebase Hosting
- Vào Firebase Console → Hosting → Add custom domain
- Thêm DNS records theo hướng dẫn

---

## 📝 Script tự động deploy

Tạo file `deploy.sh` trong thư mục BE:

```bash
#!/bin/bash
set -e

PROJECT_ID=huststay-datn
REGION=asia-southeast1
REPO=asia-southeast1-docker.pkg.dev/$PROJECT_ID/huststay-repo

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

echo "🔨 Building and pushing Docker images..."
for service in "${SERVICES[@]}"; do
  echo "Building $service..."
  cd $service
  docker build -t $REPO/$service:latest .
  docker push $REPO/$service:latest
  cd ..
done

echo "✅ All images pushed successfully!"
echo "🚀 Now deploy using gcloud run deploy commands..."
```

---

## ⚠️ Lưu ý quan trọng

1. **Eureka trên Cloud Run**: Cloud Run là stateless, nên Eureka có thể không hoạt động tốt. Cân nhắc:
   - Sử dụng **Cloud Service Directory** thay thế
   - Hoặc deploy Eureka trên **GKE** (Kubernetes)
   - Hoặc hardcode service URLs trong API Gateway

2. **WebSocket (Chat Service)**: Cloud Run hỗ trợ WebSocket nhưng có timeout. Cân nhắc sử dụng **Firebase Realtime Database** hoặc **Pub/Sub**.

3. **Chi phí**: Bắt đầu với `--min-instances=0` để tiết kiệm (cold start ~5s).

4. **Secrets**: KHÔNG commit secrets vào git. Luôn sử dụng Secret Manager.

---

## 🔗 Links hữu ích

- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Cloud SQL Documentation](https://cloud.google.com/sql/docs)
- [Firebase Hosting](https://firebase.google.com/docs/hosting)
- [Secret Manager](https://cloud.google.com/secret-manager/docs)
