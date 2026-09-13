# Backend Contracts

This document outlines the contracts for APIs expected by the Android Ops Leader app, particularly for endpoints that might not be fully implemented yet on the backend.

## Auth

### POST `/api/auth/signup`
Creates a new Ops Leader account. Account creation must be gated behind a `referenceId`.

**Request Body:**
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com", // can be phone as well
  "password": "SecurePassword123!",
  "referenceId": "1326",
  "role": "OPS_LEADER" // optional
}
```

**Success Response (200 OK or 201 Created):**
```json
{
  "success": true,
  "message": "Account created and is pending admin approval.",
  "user": {
    "id": "user-uuid",
    "name": "Jane Doe",
    "email": "jane@example.com",
    "role": "PENDING"
  }
}
```

**Error Response - Invalid Reference ID (400 Bad Request):**
```json
{
  "success": false,
  "errorCode": "REFERENCE_ID_INVALID",
  "message": "The reference ID provided is invalid or has expired."
}
```

> **Note to Backend Developers**: 
> The reference ID `1326` is the current seed value. Please seed this in the backend's reference-code table. The Android client will explicitly check for the `REFERENCE_ID_INVALID` error code to display a specific UI error, distinguishing it from generic network failures or "email already in use" errors. New accounts should default to a `PENDING` approval state.
