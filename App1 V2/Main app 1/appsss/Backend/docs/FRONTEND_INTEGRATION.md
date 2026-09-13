# Tourist Platform - Mobile & Web Frontend Integration Guide

This guide is for the mobile app (Flutter / React Native / iOS / Android) and frontend engineering teams integrating with the Tourist Platform API.

---

## 1. Base URL & Protocol
- **Local Development**: `http://localhost:8000/api/v1`
- **Staging**: `https://staging-api.touristplatform.gov.in/api/v1`
- **Production**: `https://api.touristplatform.gov.in/api/v1`
- **Interactive OpenAPI Documentation**: `/docs` (Swagger UI) and `/redoc`

---

## 2. Authentication & Session Lifecycle

### Flow Overview
1. Client logs in via Email/Password, Mobile OTP, or Google/Firebase.
2. Server returns:
   - `access_token` (Short-lived JWT, expires in 60 minutes)
   - `refresh_token` (Long-lived JWT, expires in 30 days)
3. Client registers device FCM push token via `POST /auth/devices/register`.
4. When `access_token` expires (HTTP 401), client calls `POST /auth/refresh` with `refresh_token` to receive a new pair.

### A. Login (`POST /auth/login`)
```json
// Request
{
  "email": "traveler@example.com",
  "password": "SecretPassword123"
}

// Response (HTTP 200)
{
  "access_token": "eyJhbGciOi...",
  "refresh_token": "eyJhbGciOi...",
  "token_type": "bearer",
  "expires_in": 3600,
  "user": {
    "id": "usr_98124",
    "email": "traveler@example.com",
    "phone": "+919876543210",
    "full_name": "Aarav Sharma",
    "is_verified": true,
    "role": "tourist"
  }
}
```

### B. Refresh Token Rotation (`POST /auth/refresh`)
> [!IMPORTANT]
> When refreshing, the previous refresh token is immediately blacklisted. Always store the new `access_token` AND new `refresh_token`.
```json
// Request
{
  "refresh_token": "eyJhbGciOi..."
}

// Response (HTTP 200)
{
  "access_token": "eyJhbGciOi...new",
  "refresh_token": "eyJhbGciOi...new",
  "token_type": "bearer",
  "expires_in": 3600,
  "user": { ... }
}
```

### C. Register Device FCM Token (`POST /auth/devices/register`)
Send this immediately upon login and whenever the FCM token rotates on the mobile OS:
```json
// Headers: Authorization: Bearer <access_token>
// Request
{
  "fcm_token": "fcm_mobile_device_token_string_here",
  "device_id": "pixel-8-imei-or-uuid",
  "device_name": "Google Pixel 8 Pro"
}
```

### D. Logout (`POST /auth/logout`)
Blacklists access token in Redis cache and invalidates user session.

---

## 3. Razorpay Payment Gateway Integration

### Payment Lifecycle
```
[Client]                [Backend API]             [Razorpay Gateway]
   |                          |                           |
   |-- 1. Create Order ------>|                           |
   |   (booking_id, idemp_key)|--- 2. rzp.order.create -->|
   |<-- Order ID & Key ID ----|<-- Order ID --------------|
   |                          |                           |
   |-- 3. Open Razorpay Checkout SDK (UPI/Cards/Netbank)->|
   |<-- 4. Payment Success (payment_id, signature) -------|
   |                          |                           |
   |-- 5. POST /payments/verify (order_id, pay_id, sig) ->|
   |<-- 6. Booking Confirmed + Voucher QR + Invoice ------|
```

### Step 1: Create Order (`POST /payments/order`)
```json
// Request
{
  "booking_id": "bkg_48921",
  "idempotency_key": "uuid-client-generated-key"
}

// Response
{
  "order_id": "order_N8s92Jskd92",
  "amount": 14500.0,
  "currency": "INR",
  "key_id": "rzp_live_xxxxxxxx",
  "transaction_ref": "TXN-RAZ-98A1B2",
  "status": "Pending",
  "is_simulated": false,
  "booking_id": "bkg_48921"
}
```

### Step 2: Client SDK Launch (Mobile)
Pass `order_id`, `key_id`, `amount * 100`, and `currency` into Razorpay's iOS/Android/Flutter SDK options.

### Step 3: Server-Side Signature Verification (`POST /payments/verify`)
```json
// Request
{
  "booking_id": "bkg_48921",
  "razorpay_order_id": "order_N8s92Jskd92",
  "razorpay_payment_id": "pay_N8s9A82ks91",
  "razorpay_signature": "9a823bf8912..."
}

// Response
{
  "success": true,
  "status": "Confirmed",
  "booking_reference": "BKG-HTL-A8F2",
  "invoice_number": "INV-2026-9812",
  "voucher_qr_data": "VOUCHER:BKG-HTL-A8F2|TXN:TXN-RAZ-98A1B2|INV:INV-2026-9812",
  "message": "Payment verified and booking successfully confirmed."
}
```

---

## 4. Digital Trip Card & Offline Syncing

### Get Offline Package (`GET /trip-card/{trip_id}/offline-package`)
Returns complete JSON package of itinerary, emergency contacts, local hospitals, police stations, and offline map waypoints for offline storage in SQLite / Hive / Room.

### Digital Trip Card Passport (`GET /trip-card/{trip_id}/card`)
Returns base64 PNG QR code containing encrypted offline verification token:
```json
{
  "card_id": "TC-354C7A0F",
  "qr_code_base64": "data:image/png;base64,...",
  "hotel_info": { ... },
  "insurance_policy": { ... },
  "emergency_contacts": [ ... ],
  "offline_sync_token": "..."
}
```

---

## 5. Standard Error Format & Rate Limits

All error responses adhere to standard JSON error format:
```json
{
  "success": false,
  "error": "Too Many Requests",
  "detail": "Rate limit exceeded. Please wait 60 seconds before retrying.",
  "retry_after_seconds": 60
}
```

### Rate Limit Response Headers
- `X-RateLimit-Limit`: Maximum requests per minute allowed
- `X-RateLimit-Remaining`: Remaining allowed requests in active sliding window
- `Retry-After`: Seconds to back off if HTTP 429 is received
- `X-Request-ID`: Distributed request tracing UUID
