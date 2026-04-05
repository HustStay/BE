# 🚀 Hướng dẫn Setup CI/CD từ GitHub đến Google Cloud

## ✅ Đã hoàn thành

1. **Sửa tất cả application.yaml** - Thay hardcoded secrets bằng environment variables
2. **Tạo cloudbuild.yaml** - Cấu hình CI/CD tự động
3. **Cập nhật .gitignore** - Bỏ qua file credentials.json

## 📋 Các bước cần thực hiện

### Bước 1: Tạo Secrets trong Google Secret Manager

Chạy các lệnh sau trong terminal (thay YOUR_xxx bằng giá trị thật):

```bash
# Database password
echo -n "YOUR_DB_PASSWORD" | gcloud secrets create db-password --data-file=-

# JWT Secret
echo -n "YOUR_JWT_SECRET" | gcloud secrets create jwt-secret --data-file=-

# Stripe keys
echo -n "YOUR_STRIPE_PUBLISHABLE_KEY" | gcloud secrets create stripe-publishable-key --data-file=-

echo -n "YOUR_STRIPE_SECRET_KEY" | gcloud secrets create stripe-secret-key --data-file=-

# Google OAuth
echo -n "YOUR_GOOGLE_CLIENT_ID" | gcloud secrets create google-client-id --data-file=-

echo -n "YOUR_GOOGLE_CLIENT_SECRET" | gcloud secrets create google-client-secret --data-file=-
```

### Bước 2: Cấp quyền cho Cloud Build truy cập Secret Manager

```bash
# Lấy project number
PROJECT_NUMBER=$(gcloud projects describe YOUR_PROJECT_ID --format='value(projectNumber)')

# Cấp quyền Secret Accessor cho Cloud Build service account
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Cấp quyền Cloud Run Admin cho Cloud Build
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/run.admin"

# Cấp quyền Service Account User
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com" \
  --role="roles/iam.serviceAccountUser"
```

### Bước 3: Kết nối GitHub với Cloud Build

1. Truy cập: https://console.cloud.google.com/cloud-build/triggers

2. Click **"Connect Repository"**

3. Chọn **"GitHub (Cloud Build GitHub App)"**

4. Authorize và chọn repository

5. Click **"Create Trigger"** với cấu hình:
   - Name: `deploy-main`
   - Event: Push to branch
   - Branch: `^main$`
   - Configuration: Cloud Build configuration file
   - Location: `/cloudbuild.yaml`

### Bước 4: Push code lên GitHub

```bash
git add .
git commit -m "Setup CI/CD"
git push origin main
```

## 🔑 Danh sách Secrets cần tạo

| Secret Name | Service | Mô tả |
|-------------|---------|-------|
| db-password | All services | Database password |
| jwt-secret | user-service, api-gateway | JWT signing key |
| stripe-publishable-key | payment-service | Stripe public key |
| stripe-secret-key | payment-service | Stripe secret key |
| google-client-id | booking-service | Google OAuth client ID |
| google-client-secret | booking-service | Google OAuth client secret |

## ⚠️ Lưu ý

1. **Local Development**: Code vẫn chạy local bình thường với default values
2. **Secrets trên Cloud**: Tự động lấy từ Secret Manager
3. **Không commit secrets**: credentials.json đã được thêm vào .gitignore
4. **Auto Deploy**: Mỗi lần push lên main sẽ tự động build và deploy
