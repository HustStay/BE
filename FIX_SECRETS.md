# Hướng dẫn sửa lỗi secrets và push lên GitHub

## Vấn đề
GitHub chặn push vì phát hiện secrets trong:
1. `booking-service/target/classes/credentials.json` - Google OAuth
2. `payment-service/src/main/resources/application.yaml` - Stripe API keys

## Đã thực hiện
- ✅ Tạo `.gitignore` để bỏ qua credentials và target folder
- ✅ Sửa `payment-service/src/main/resources/application.yaml` - thay secrets bằng environment variables
- ✅ Tạo `credentials.json.example` làm template

## Các bước cần chạy trong Git Bash / Terminal

### Bước 1: Xóa files khỏi git tracking
```bash
cd d:/20252/DATN/BE

# Xóa credentials.json khỏi tracking (cả src và target)
git rm --cached booking-service/src/main/resources/credentials.json
git rm --cached booking-service/target/classes/credentials.json

# Xóa toàn bộ target folders
git rm -r --cached "**/target/"
```

### Bước 2: Reset commit chứa secrets
```bash
# Xem commit history
git log --oneline -10

# Soft reset về commit trước commit chứa secrets (f347a06)
git reset --soft HEAD~1
```

### Bước 3: Stage lại các file (không có secrets)
```bash
git add .
git status
```

### Bước 4: Commit lại
```bash
git commit -m "feat: Update payment-service with env vars, add gitignore

- Replace hardcoded Stripe keys with environment variables
- Add .gitignore to exclude credentials and target folders
- Add credentials.json.example as template

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

### Bước 5: Push với force (vì đã thay đổi history)
```bash
git push origin main --force
```

## Tạo file môi trường (.env) - KHÔNG commit file này
Tạo file `.env` trong root hoặc từng service để lưu secrets:

```bash
# payment-service/.env
STRIPE_PUBLISHABLE_KEY=pk_test_51TDTTGF5kDrDuaAa...
STRIPE_SECRET_KEY=sk_test_51TDTTGF5kDrDuaAa...

# booking-service - tạo credentials.json từ template
# Copy credentials.json.example -> credentials.json và điền secrets thật
```

## Kiểm tra FE
- ✅ Cloudinary config sử dụng placeholder `your-api-key` - an toàn
- ✅ Không có secrets bị hardcode trong FE

## Lưu ý quan trọng
1. **KHÔNG BAO GIỜ** commit secrets vào git
2. Sử dụng environment variables hoặc secret managers
3. Thêm các file chứa secrets vào `.gitignore`
4. Sau khi fix, cần **xoay (rotate)** các secrets đã bị lộ:
   - Tạo mới Google OAuth credentials tại Google Cloud Console
   - Tạo mới Stripe API keys tại Stripe Dashboard
