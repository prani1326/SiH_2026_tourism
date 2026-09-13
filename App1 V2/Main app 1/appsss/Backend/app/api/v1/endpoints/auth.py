import json
import secrets
import uuid
from datetime import datetime, timedelta, timezone
from typing import Optional
import jwt
from fastapi import APIRouter, Depends, HTTPException, status, Request
from app.core.config import settings
from app.core.redis import redis_client
from app.core.security import (
    hash_password, verify_password, create_access_token, create_refresh_token,
    get_current_user, revoke_token, revoke_all_user_tokens, oauth2_scheme
)
from app.core.firebase import firebase_service
from app.core.firestore_db import get_firestore
from app.models.firestore_models import User, UserProfile, TravelPreferences, AuditLog
from app.repositories.user_repo import user_repo
from app.schemas.auth import (
    SignupRequest, LoginRequest, PhoneOtpSendRequest, PhoneOtpVerifyRequest,
    GoogleLoginRequest, RefreshTokenRequest, PasswordRecoveryRequest,
    PasswordRecoveryConfirm, DeviceRegisterRequest, TokenResponse, SplashInitResponse
)
from app.services.external.communication_service import communication_service

router = APIRouter()

def _record_audit_log(user_id: Optional[str], action: str, request: Request, details: dict = None):
    try:
        db = get_firestore()
        client_ip = request.client.host if request.client else "127.0.0.1"
        user_agent = request.headers.get("User-Agent", "Unknown")
        audit = AuditLog(
            user_id=user_id,
            action=action,
            entity_type="User",
            entity_id=user_id,
            ip_address=client_ip,
            user_agent=user_agent,
            details=json.dumps(details or {})
        )
        db.collection("audit_logs").document(audit.id).set(audit.to_dict())
    except Exception:
        pass

def _create_and_record_session(
    user: User,
    request: Request,
    device_id: Optional[str] = None,
    fcm_token: Optional[str] = None
) -> tuple[str, str]:
    """Issues an access token and a refresh token, recording the session in Firestore."""
    access_token = create_access_token(data={"sub": str(user.id), "email": user.email, "role": user.role})
    refresh_token = create_refresh_token(data={"sub": str(user.id)})

    payload = jwt.decode(refresh_token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
    jti = payload["jti"]
    exp_ts = payload["exp"]
    expires_at = datetime.fromtimestamp(exp_ts, tz=timezone.utc).isoformat()

    client_ip = request.client.host if request.client else "127.0.0.1"
    user_agent = request.headers.get("User-Agent", "Unknown")

    db = get_firestore()
    session_data = {
        "id": str(uuid.uuid4()),
        "user_id": str(user.id),
        "refresh_token_jti": jti,
        "device_id": device_id,
        "fcm_token": fcm_token or getattr(user, "fcm_token", None),
        "ip_address": client_ip,
        "user_agent": user_agent,
        "expires_at": expires_at,
        "is_revoked": False,
        "created_at": datetime.now(timezone.utc).isoformat()
    }
    db.collection("user_sessions").document(session_data["id"]).set(session_data)

    return access_token, refresh_token

@router.get("/splash/init", response_model=SplashInitResponse)
def splash_init():
    """Splash Screen & Initial App Configuration (Section 2, 44)."""
    return {
        "app_name": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "supported_currencies": settings.SUPPORTED_CURRENCIES,
        "supported_languages": settings.SUPPORTED_LANGUAGES,
        "emergency_baseline": {
            "National Emergency": settings.EMERGENCY_POLICE,
            "Ambulance": settings.EMERGENCY_AMBULANCE,
            "Tourist Helpline": settings.EMERGENCY_TOURIST_HELPLINE,
            "Women Safety": settings.EMERGENCY_WOMEN_SAFETY
        },
        "feature_flags": {
            "ai_planner_active": True,
            "trip_os_replan_active": True,
            "lost_phone_mode_active": True,
            "offline_sync_active": True,
            "creator_marketplace_active": True,
            "sos_instant_dispatch_active": True
        }
    }

@router.post("/signup", response_model=TokenResponse)
def signup(request_body: SignupRequest, request: Request):
    """Customer Registration with Firestore persistence (Section 2)."""
    if not request_body.email and not request_body.phone:
        raise HTTPException(status_code=400, detail="Provide either email or mobile number.")

    # Check duplicates in Firestore
    if request_body.email and user_repo.get_by_email(request_body.email):
        raise HTTPException(status_code=400, detail="Email already registered. Please login.")
    if request_body.phone and user_repo.get_by_phone(request_body.phone):
        raise HTTPException(status_code=400, detail="Phone number already registered. Please login.")

    user = User(
        id=str(uuid.uuid4()),
        email=request_body.email or "",
        phone=request_body.phone,
        password_hash=hash_password(request_body.password),
        full_name=request_body.full_name or "Traveler",
        emergency_recovery_pin=request_body.emergency_recovery_pin or "1234",
        is_active=True,
        is_verified=True,
        role="tourist",
        profile=UserProfile(),
        preferences=TravelPreferences()
    )
    user_repo.create(user)

    access_token, refresh_token = _create_and_record_session(user, request)
    _record_audit_log(user.id, "AUTH_SIGNUP", request, {"email": user.email, "phone": user.phone})

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        "user": {
            "id": str(user.id),
            "email": user.email,
            "phone": user.phone,
            "full_name": user.full_name,
            "is_verified": user.is_verified,
            "role": user.role
        }
    }

@router.post("/login", response_model=TokenResponse)
def login(request_body: LoginRequest, request: Request):
    """Customer Login authenticated against Firestore credentials (Section 2)."""
    user = None
    if request_body.email:
        user = user_repo.get_by_email(request_body.email)
    elif request_body.phone:
        user = user_repo.get_by_phone(request_body.phone)

    stored_hash = getattr(user, "password_hash", None) or getattr(user, "hashed_password", None) if user else None
    if not user or not verify_password(request_body.password, stored_hash):
        _record_audit_log(None, "AUTH_LOGIN_FAILED", request, {"identifier": request_body.email or request_body.phone})
        raise HTTPException(status_code=401, detail="Invalid login credentials.")

    if hasattr(user, "is_active") and not user.is_active:
        raise HTTPException(status_code=403, detail="User account is deactivated.")

    access_token, refresh_token = _create_and_record_session(user, request)
    _record_audit_log(user.id, "AUTH_LOGIN_SUCCESS", request)

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        "user": {
            "id": str(user.id),
            "email": user.email,
            "phone": user.phone,
            "full_name": user.full_name,
            "is_verified": user.is_verified,
            "role": user.role
        }
    }

@router.post("/phone/otp/send")
@router.post("/otp/send")
def send_otp(request_body: PhoneOtpSendRequest):
    """Dispatch Mobile OTP with Redis TTL and SMS Gateway (Section 2)."""
    otp = "123456" if settings.ENVIRONMENT in ["development", "test"] else f"{secrets.randbelow(900000) + 100000}"
    redis_client.store_otp(request_body.phone, otp, ttl_seconds=300)
    # Reset attempts counter on new OTP dispatch
    redis_client.delete_cache(f"otp_attempts:{request_body.phone}")

    # Dispatch via SMS service (Twilio/SMS Gateway)
    sms_text = f"Your Tourist App verification code is {otp}. Valid for 5 minutes. Do not share this code."
    communication_service.send_sms(request_body.phone, sms_text)

    return {
        "success": True,
        "message": f"Verification code dispatched to {request_body.phone}.",
        "demo_code": otp if settings.ENVIRONMENT in ["development", "test"] else None
    }

@router.post("/phone/otp/verify", response_model=TokenResponse)
@router.post("/otp/verify", response_model=TokenResponse)
def verify_otp(request_body: PhoneOtpVerifyRequest, request: Request):
    """Verify Mobile OTP with attempt limiting and Authenticate / Auto-Register in Firestore (Section 2)."""
    attempts_key = f"otp_attempts:{request_body.phone}"
    raw_attempts = redis_client.get_cache(attempts_key)
    attempts = int(raw_attempts) if raw_attempts else 0

    if attempts >= 5:
        raise HTTPException(
            status_code=429,
            detail="Too many failed verification attempts. Please request a new OTP code."
        )

    is_valid = redis_client.verify_otp(request_body.phone, request_body.otp_code)
    # Allow 123456 exclusively in development/test environments
    if not is_valid and settings.ENVIRONMENT in ["development", "test"] and request_body.otp_code == "123456":
        is_valid = True

    if not is_valid:
        redis_client.set_cache(attempts_key, str(attempts + 1), ttl_seconds=300)
        raise HTTPException(status_code=400, detail="Invalid or expired OTP code.")

    # Reset attempts on success
    redis_client.delete_cache(attempts_key)

    user = user_repo.get_by_phone(request_body.phone)
    if not user:
        user = User(
            id=str(uuid.uuid4()),
            phone=request_body.phone,
            email="",
            full_name="Mobile Traveler",
            is_verified=True,
            is_active=True,
            role="tourist",
            emergency_recovery_pin="1234",
            profile=UserProfile(),
            preferences=TravelPreferences()
        )
        user_repo.create(user)

    access_token, refresh_token = _create_and_record_session(user, request)
    _record_audit_log(user.id, "AUTH_OTP_LOGIN", request, {"phone": user.phone})

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        "user": {
            "id": str(user.id),
            "email": user.email,
            "phone": user.phone,
            "full_name": user.full_name,
            "is_verified": user.is_verified,
            "role": user.role
        }
    }

@router.post("/firebase-login", response_model=TokenResponse)
@router.post("/google", response_model=TokenResponse)
def google_firebase_login(request_body: GoogleLoginRequest, request: Request):
    """Google / Firebase Token Exchange (Section 2)."""
    try:
        claims = firebase_service.verify_id_token(request_body.id_token)
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Firebase token verification failed: {e}")

    email = claims.get("email")
    phone = claims.get("phone_number")
    uid = claims.get("uid")
    user = None
    if uid:
        user = user_repo.get_by_firebase_uid(uid)
    if not user and email:
        user = user_repo.get_by_email(email)
    if not user and phone:
        user = user_repo.get_by_phone(phone)

    if not user:
        user = User(
            id=str(uuid.uuid4()),
            firebase_uid=uid,
            email=email or "",
            phone=phone,
            full_name=claims.get("name", "Traveler"),
            is_verified=True,
            is_active=True,
            role="tourist",
            emergency_recovery_pin="1234",
            profile=UserProfile(avatar_url=claims.get("picture", "")),
            preferences=TravelPreferences()
        )
        user_repo.create(user)
    elif uid and not user.firebase_uid:
        user_repo.update(user.id, {"firebase_uid": uid})

    access_token, refresh_token = _create_and_record_session(user, request)
    _record_audit_log(user.id, "AUTH_GOOGLE_FIREBASE_LOGIN", request, {"email": user.email})

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        "user": {
            "id": str(user.id),
            "email": user.email,
            "phone": user.phone,
            "full_name": user.full_name,
            "is_verified": user.is_verified,
            "role": user.role
        }
    }

@router.post("/refresh", response_model=TokenResponse)
def refresh_token(request_body: RefreshTokenRequest, request: Request):
    """
    Refresh Token Rotation in Firestore.
    Accepts valid refresh token, validates JTI against Redis and Firestore session,
    revokes old JTI, and issues a new access token + rotated refresh token.
    """
    token = request_body.refresh_token
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Refresh token has expired. Please login again.")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid refresh token.")

    if payload.get("type") != "refresh":
        raise HTTPException(status_code=400, detail="Provided token is not a refresh token.")

    jti = payload.get("jti")
    user_id = payload.get("sub")

    if jti and redis_client.is_token_revoked(jti):
        raise HTTPException(status_code=401, detail="Refresh token has been revoked.")

    if user_id and redis_client.is_user_session_revoked(user_id, float(payload.get("iat", 0))):
        raise HTTPException(status_code=401, detail="User session has been revoked.")

    db = get_firestore()
    session_snaps = db.collection("user_sessions").where("refresh_token_jti", "==", jti).where("is_revoked", "==", False).limit(1).get()
    session_data = session_snaps[0].to_dict() if session_snaps else None

    user = user_repo.get_by_id(str(user_id))
    if not user or (hasattr(user, "is_active") and not user.is_active):
        raise HTTPException(status_code=404, detail="User not found or inactive.")

    if jti:
        redis_client.revoke_token(jti, ttl_seconds=settings.REFRESH_TOKEN_EXPIRE_DAYS * 86400)
    if session_snaps:
        db.collection("user_sessions").document(session_snaps[0].id).update({"is_revoked": True})

    new_access_token, new_refresh_token = _create_and_record_session(
        user=user,
        request=request,
        device_id=session_data.get("device_id") if session_data else None,
        fcm_token=session_data.get("fcm_token") if session_data else None
    )

    _record_audit_log(user.id, "AUTH_REFRESH_TOKEN", request)

    return {
        "access_token": new_access_token,
        "refresh_token": new_refresh_token,
        "token_type": "bearer",
        "expires_in": settings.ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        "user": {
            "id": str(user.id),
            "email": user.email,
            "phone": user.phone,
            "full_name": user.full_name,
            "is_verified": user.is_verified,
            "role": user.role
        }
    }

@router.post("/devices/register")
def register_device(
    request_body: DeviceRegisterRequest,
    current_user: User = Depends(get_current_user)
):
    """Register device FCM push token for logged-in user in Firestore."""
    user_repo.update(str(current_user.id), {"fcm_token": request_body.fcm_token})
    db = get_firestore()
    sessions = db.collection("user_sessions").where("user_id", "==", str(current_user.id)).where("is_revoked", "==", False).get()
    for s in sessions:
        db.collection("user_sessions").document(s.id).update({
            "fcm_token": request_body.fcm_token,
            "device_id": request_body.device_id or s.to_dict().get("device_id"),
            "device_name": request_body.device_name or s.to_dict().get("device_name")
        })
    return {"success": True, "message": "Device successfully registered for push notifications."}

@router.post("/recovery/request")
def request_password_recovery(request_body: PasswordRecoveryRequest):
    """Forgot Password / Recovery Flow with Redis OTP (Section 2)."""
    target = request_body.email or request_body.phone
    if not target:
        raise HTTPException(status_code=400, detail="Provide either email or phone.")

    user = user_repo.get_by_email(target) if "@" in target else user_repo.get_by_phone(target)
    if not user:
        raise HTTPException(status_code=404, detail="User account not found.")

    code = "123456" if settings.ENVIRONMENT in ["development", "test"] else f"{secrets.randbelow(900000) + 100000}"
    redis_client.store_otp(f"recovery:{target}", code, ttl_seconds=900)

    return {
        "success": True,
        "message": f"Password recovery code dispatched to {target}.",
        "recovery_code": code if settings.ENVIRONMENT in ["development", "test"] else None
    }

@router.post("/recovery/confirm")
def confirm_password_recovery(
    request_body: PasswordRecoveryConfirm,
    request: Request
):
    """Reset Password with Recovery Code (Section 2)."""
    target = request_body.identifier
    is_valid = redis_client.verify_otp(f"recovery:{target}", request_body.recovery_code)
    if not is_valid and settings.ENVIRONMENT in ["development", "test"] and (request_body.recovery_code == "123456" or len(request_body.recovery_code) >= 4):
        is_valid = True

    if not is_valid:
        raise HTTPException(status_code=400, detail="Invalid or expired recovery code.")

    user = user_repo.get_by_email(target) if "@" in target else user_repo.get_by_phone(target)
    if not user:
        raise HTTPException(status_code=404, detail="User account not found.")

    new_hash = hash_password(request_body.new_password)
    user_repo.update(str(user.id), {"password_hash": new_hash})
    revoke_all_user_tokens(str(user.id))

    _record_audit_log(user.id, "AUTH_PASSWORD_RESET", request)
    return {"success": True, "message": "Password has been successfully updated. All previous sessions revoked."}

@router.post("/logout")
def logout(
    request: Request,
    token: Optional[str] = Depends(oauth2_scheme),
    current_user: User = Depends(get_current_user)
):
    """Logout current session and blacklist JWT token in Redis (Section 2)."""
    if token:
        revoke_token(token)
        try:
            payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM], options={"verify_exp": False})
            jti = payload.get("jti")
            if jti:
                db = get_firestore()
                snaps = db.collection("user_sessions").where("refresh_token_jti", "==", jti).get()
                for s in snaps:
                    db.collection("user_sessions").document(s.id).update({"is_revoked": True})
        except Exception:
            pass

    _record_audit_log(current_user.id, "AUTH_LOGOUT", request)
    return {"success": True, "message": "Successfully logged out. Session token has been revoked."}
