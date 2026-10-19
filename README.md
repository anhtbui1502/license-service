# vOCS License Manager

Service quản lý và sinh file license vOCS cho các thị trường (trừ Việt Nam). Mỗi lần sinh license mới đều phải có **OTP gửi tới email người duyệt (giám đốc)**, và toàn bộ lịch sử file đã cấp được lưu lại.

## Luồng nghiệp vụ

```
Người cấu hình                     License Manager                          Người duyệt
  1. Chọn thị trường, nhập ─────▶ 2. Kiểm tra, lưu yêu cầu PENDING_OTP,
     maxSub, maxActiveSub, lý do     sinh OTP (DB chỉ lưu HMAC) ─── 3. Email: thị trường, số cũ → mới,
                                                                       chênh lệch, OTP ──────────────▶
  ◀──────────────────── 4. Người duyệt đồng ý thì đọc OTP cho người cấu hình ────────────────────────
  5. Nhập OTP ──────────────────▶ 6. Đúng OTP → mã hóa → lưu file (version +1, SHA-256) → tải file
```

## Tính năng (MVP)

- **Thị trường**: thêm/sửa, cờ `Quản lý license` (Việt Nam = tắt, không tạo được yêu cầu).
- **Tạo license mới**: chọn thị trường, hệ thống hiện số hiện hành và chênh lệch, nhập 2 con số + lý do, rồi gửi OTP.
- **Nhập OTP**: có đếm ngược thời hạn, số lần nhập còn lại, nút gửi lại (có cooldown) và nút hủy yêu cầu.
- **Lịch sử file license**: theo từng thị trường hoặc toàn bộ; có version, mã license, SHA-256, người sinh, và cho tải lại đúng file cũ.
- **Phân quyền**: `ADMIN` (toàn quyền), `OPERATOR` (tạo yêu cầu, nhập OTP, tải file), `VIEWER` (chỉ xem).
- **Nhật ký hệ thống**: đăng nhập, tạo yêu cầu, gửi OTP, nhập sai OTP, sinh file, tải file, sửa cấu hình.

## Chống spam OTP

| Cơ chế | Mặc định | Biến môi trường |
|---|---|---|
| Mỗi thị trường chỉ **1 yêu cầu chờ OTP** (DB chặn bằng cột UNIQUE `pending_market_id` + khóa dòng market) | 1 | — |
| Thời hạn OTP (quá hạn thì yêu cầu tự đóng, giải phóng slot) | 30 phút | `OTP_TTL` |
| Khoảng cách tối thiểu giữa 2 lần gửi lại OTP | 60 giây | `OTP_RESEND_COOLDOWN` |
| Số lần gửi lại tối đa mỗi yêu cầu (gửi lại thì OTP cũ mất hiệu lực) | 3 | `OTP_MAX_RESENDS` |
| Số lần nhập sai tối đa mỗi yêu cầu (cộng dồn qua các lần gửi lại), vượt thì yêu cầu bị khóa | 5 | `OTP_MAX_ATTEMPTS` |
| Trần email OTP / giờ, toàn hệ thống | 30 | `OTP_MAX_EMAILS_PER_HOUR` |
| Trần email OTP / giờ, mỗi người dùng | 10 | `OTP_MAX_EMAILS_PER_HOUR_PER_USER` |

OTP 6 số được sinh bằng `SecureRandom`. DB chỉ lưu HMAC-SHA256 của OTP, và giá trị băm **gắn với mã yêu cầu + thị trường + 2 con số**, nên OTP của yêu cầu này không dùng được cho yêu cầu khác, và con số trong email luôn khớp với con số ghi vào file.

## Rate limit API

Rate limit dùng token bucket (Bucket4j) trong bộ nhớ và trả `429` kèm header `Retry-After`. Cấu hình nằm ở `app.rate-limit.rules` trong `application.yml`:

| Rule | Endpoint | Theo | Giới hạn |
|---|---|---|---|
| `login` | `POST /api/auth/login` | IP | 10 / phút |
| `request-create` | `POST /api/requests` | user | 5 / phút |
| `otp-resend` | `POST /api/requests/*/resend-otp` | user | 5 / phút |
| `otp-verify-user` | `POST /api/requests/*/verify` | user | 10 / phút |
| `otp-verify-ip` | `POST /api/requests/*/verify` | IP | 20 / phút |
| `api-default` | `/api/**` | user | 300 / phút |

> Bucket lưu trong bộ nhớ nên chỉ đúng khi chạy **1 instance backend**. Nếu chạy nhiều instance thì chuyển sang Bucket4j + Redis.
> nginx ghi đè `X-Forwarded-For` bằng IP thật của client, nên client không giả mạo được IP để né giới hạn.

## Cấu trúc

```
backend/   Spring Boot 3.3 (Java 21), PostgreSQL, Flyway
  src/main/java/com/vht/license/
    auth/ user/ audit/        đăng nhập (session + CSRF), người dùng, nhật ký
    market/ license/          thị trường, file license
    request/                  luồng yêu cầu: tạo / gửi lại OTP / nhập OTP / hủy / tự hết hạn
    otp/                      sinh + băm OTP, gửi email (SMTP)
    crypto/                   LicenseCryptoProvider — điểm cắm thuật toán mã hóa
    ratelimit/                rate limit filter
  src/main/resources/db/migration/V1__init.sql
frontend/  React 18 + TypeScript + Ant Design (Vite), nginx khi deploy
docker-compose.yml           postgres + backend + frontend (+ mailpit cho dev)
```

## Chạy bằng Docker Compose

```bash
cp .env.example .env    # điền DB_PASSWORD, APPROVER_EMAIL, OTP_HMAC_SECRET, BOOTSTRAP_ADMIN_PASSWORD
docker compose up -d --build
```

- Giao diện: http://localhost:8088 (đăng nhập bằng tài khoản ADMIN khai báo trong `.env`)
- Mail dev (mailpit): http://localhost:8025. Khi lên production, trỏ `SMTP_*` sang SMTP nội bộ và bỏ service `mailpit`.

## Chạy dev

```bash
# Backend (cần PostgreSQL ở localhost:5432, db/user/pass = license)
cd backend
APPROVER_EMAIL=giamdoc@vht.com.vn OTP_HMAC_SECRET=$(openssl rand -hex 32) \
BOOTSTRAP_ADMIN_PASSWORD=Admin@12345 mvn spring-boot:run

# Frontend (proxy /api sang :8080)
cd frontend && npm install && npm run dev     # http://localhost:5173

# Test backend (H2, không cần PostgreSQL)
cd backend && mvn test
```

## Tích hợp thuật toán mã hóa thật

Hiện tại hệ thống dùng `MockLicenseCryptoProvider`: file **không được mã hóa thật**, chỉ để thử nghiệm, và khi khởi động sẽ log cảnh báo. Khi có thuật toán của VHT:

1. Tạo class implements `crypto/LicenseCryptoProvider` (`encrypt`, `decrypt`, `name`, `fileExtension`).
2. Thêm nhánh tương ứng trong `crypto/CryptoConfig` rồi đặt `LICENSE_CRYPTO=<tên>`.
3. Khóa bí mật đọc từ biến môi trường hoặc file mount riêng, **không** commit vào repo hay lưu trong DB.

## API chính

| Method | Path | Quyền |
|---|---|---|
| POST | `/api/auth/login`, `/api/auth/logout`; GET `/api/auth/me` | — |
| GET | `/api/markets`, `/api/markets/{id}` | mọi user |
| POST/PUT | `/api/markets`, `/api/markets/{id}` | ADMIN |
| GET | `/api/requests`, `/api/requests/{id}` | mọi user |
| POST | `/api/requests` | ADMIN, OPERATOR |
| POST | `/api/requests/{id}/resend-otp`, `/verify`, `/cancel` | người tạo yêu cầu hoặc ADMIN |
| GET | `/api/licenses`, `/api/licenses/{id}` | mọi user |
| GET | `/api/licenses/{id}/download` | ADMIN, OPERATOR |
| GET/POST/PUT | `/api/users` | ADMIN |
| GET | `/api/audit-logs` | ADMIN |

## Để sau (ngoài phạm vi MVP)

Import file license cũ, biểu đồ lịch sử, link duyệt/từ chối trong email, nút "thay thế yêu cầu", outbox gửi lại email, đo mức sử dụng thực tế từ thị trường, ký số file (RSA/ECDSA), duyệt nhiều cấp, đăng nhập LDAP/SSO.
