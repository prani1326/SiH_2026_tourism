import hashlib
import os
import secrets
import re
from datetime import datetime, timedelta, timezone
from typing import Optional, Dict, Any, List, Set
import logging
import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from app.core.config import settings
from app.core.redis import redis_client

logger = logging.getLogger(__name__)

oauth2_scheme = OAuth2PasswordBearer(tokenUrl=f"{settings.API_V1_STR}/auth/login", auto_error=False)

def validate_password_strength(password: str) -> bool:
    """
    Enterprise password security validator:
    - Minimum 8 characters
    - At least 1 number
    - At least 1 letter
    """
    if len(password) < 8:
        return False
    has_letter = bool(re.search(r"[a-zA-Z]", password))
    has_digit = bool(re.search(r"[0-9]", password))
    return has_letter and has_digit

def hash_password(password: str) -> str:
    """Hash a password using PBKDF2 HMAC SHA-256 with a unique salt."""
    salt = secrets.token_hex(16)
    key = hashlib.pbkdf2_hmac(
        'sha256',
        password.encode('utf-8'),
        salt.encode('utf-8'),
        100000
    )
    return f"{salt}${key.hex()}"

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify password against stored salt and key."""
    if not hashed_password or '$' not in hashed_password:
        return False
    try:
        salt, key_hex = hashed_password.split('$', 1)
        expected_key = hashlib.pbkdf2_hmac(
            'sha256',
            plain_password.encode('utf-8'),
            salt.encode('utf-8'),
            100000
        )
        return secrets.compare_digest(expected_key.hex(), key_hex)
    except Exception:
        return False

def create_access_token(data: Dict[str, Any], expires_delta: Optional[timedelta] = None) -> str:
    """Create signed JWT access token with unique JTI and role claims to support durable revocation."""
    to_encode = data.copy()
    now = datetime.now(timezone.utc)
    if expires_delta:
        expire = now + expires_delta
    else:
        expire = now + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
        
    jti = secrets.token_hex(16)
    to_encode.update({
        "exp": expire,
        "iat": now.timestamp(),
        "jti": jti,
        "type": "access"
    })
    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt

def create_refresh_token(data: Dict[str, Any]) -> str:
    """Create signed JWT refresh token with unique JTI."""
    to_encode = data.copy()
    now = datetime.now(timezone.utc)
    expire = now + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS)
    jti = secrets.token_hex(16)
    to_encode.update({
        "exp": expire,
        "iat": now.timestamp(),
        "jti": jti,
        "type": "refresh"
    })
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)

def revoke_token(token: str):
    """Blacklist a token in Redis so it cannot be used again."""
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM], options={"verify_exp": False})
        jti = payload.get("jti")
        exp = payload.get("exp")
        ttl = 86400
        if exp:
            now_ts = datetime.now(timezone.utc).timestamp()
            ttl = max(1, int(exp - now_ts))
        if jti:
            redis_client.revoke_token(jti, ttl_seconds=ttl)
    except Exception:
        pass

def revoke_all_user_tokens(user_id: str):
    """Revoke all active tokens for a specific user in Redis (e.g. Lost Phone Mode or Password Reset)."""
    redis_client.invalidate_user_sessions(user_id)

def decode_token(token: str) -> Dict[str, Any]:
    """Decode, validate and verify that the JWT token is not revoked."""
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        
        # 1. Check if token JTI is in Redis revocation blacklist
        jti = payload.get("jti")
        if jti and redis_client.is_token_revoked(jti):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Token has been revoked. Please login again.",
                headers={"WWW-Authenticate": "Bearer"},
            )
            
        # 2. Check if user's tokens were globally invalidated after token issuance
        user_id = payload.get("sub")
        iat = payload.get("iat")
        if user_id and iat:
            if redis_client.is_user_session_revoked(user_id, float(iat)):
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Session invalidated. Please login again.",
                    headers={"WWW-Authenticate": "Bearer"},
                )
                
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has expired. Please login again.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except jwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token credentials.",
            headers={"WWW-Authenticate": "Bearer"},
        )

def get_current_user_optional(
    token: Optional[str] = Depends(oauth2_scheme)
) -> Optional[Any]:
    """Dependency returning the current user or None if unauthenticated."""
    if not token:
        return None
    try:
        from app.repositories.user_repo import user_repo

        # 1. Try local platform JWT first
        try:
            payload = decode_token(token)
            user_id = payload.get("sub")
            if user_id:
                user = user_repo.get_by_id(str(user_id))
                if user:
                    return user
        except Exception:
            pass

        # 2. Try Firebase ID token verification
        try:
            from app.core.firebase import firebase_service
            fb_res = firebase_service.verify_id_token(token)
            if fb_res and fb_res.get("uid"):
                uid = fb_res.get("uid")
                email = fb_res.get("email")
                user = user_repo.get_by_firebase_uid(uid) if uid else None
                if not user and email:
                    user = user_repo.get_by_email(email)
                if user:
                    return user
        except Exception:
            pass

        return None
    except Exception:
        return None

def get_current_user(
    token: Optional[str] = Depends(oauth2_scheme)
):
    """Dependency requiring a valid, non-revoked authenticated user from Firestore."""
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication credentials were not provided.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    from app.repositories.user_repo import user_repo

    # 1. Try standard platform JWT first (instant local verification)
    try:
        payload = decode_token(token)
        user_id = payload.get("sub")
        if user_id:
            user = user_repo.get_by_id(str(user_id))
            if not user:
                user = user_repo.get_by_firebase_uid(str(user_id))
            if not user and payload.get("email"):
                user = user_repo.get_by_email(payload.get("email"))
            if user:
                if hasattr(user, "is_active") and not user.is_active:
                    raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="User account is deactivated.")
                return user
    except HTTPException:
        raise
    except Exception:
        pass

    # 2. Try Firebase ID token verification
    try:
        from app.core.firebase import firebase_service
        fb_res = firebase_service.verify_id_token(token)
        if fb_res and (fb_res.get("uid") or fb_res.get("user_id") or fb_res.get("sub")):
            uid = fb_res.get("uid") or fb_res.get("user_id") or fb_res.get("sub")
            email = fb_res.get("email")
            user = user_repo.get_by_firebase_uid(uid) if uid else None
            if not user and uid:
                user = user_repo.get_by_id(uid)
            if not user and email:
                user = user_repo.get_by_email(email)
                if user and uid and getattr(user, "firebase_uid", None) != uid:
                    user_repo.update(user.id, {"firebase_uid": uid})
                    user.firebase_uid = uid

            if not user and uid:
                from app.models.firestore_models import User
                user = User(
                    id=uid,
                    firebase_uid=uid,
                    email=email or f"{uid[:8]}@touristapp.com",
                    full_name=fb_res.get("name") or "Tourist Traveler",
                    role="traveler",
                    is_verified=True,
                    is_active=True
                )
                user_repo.create(user)

            if user:
                if hasattr(user, "is_active") and not user.is_active:
                    raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="User account is deactivated.")
                return user
    except HTTPException:
        raise
    except Exception as e:
        logger.debug(f"Firebase token verification bypass: {e}")

    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid authentication token credentials.",
        headers={"WWW-Authenticate": "Bearer"},
    )

# ------------------------------------------------------------------------------
# Role-Based Access Control (RBAC) & Ownership Dependencies
# ------------------------------------------------------------------------------
def require_roles(allowed_roles: List[str]):
    """Enforces that the current authenticated user has one of the allowed roles."""
    def role_checker(current_user = Depends(get_current_user)):
        if current_user.role not in allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Access forbidden: requires one of the following roles: {allowed_roles}"
            )
        return current_user
    return role_checker

require_admin = require_roles(["admin"])
require_support_or_admin = require_roles(["admin", "support_agent"])
require_partner_or_admin = require_roles(["admin", "partner"])

def verify_ownership(current_user, resource_owner_id: str, resource_name: str = "resource"):
    """
    Verifies that the current user owns the target resource, or is an admin.
    Raises HTTP 403 Forbidden if not authorized.
    """
    if current_user.role == "admin":
        return True
    if current_user.id != resource_owner_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"You do not have permission to access or modify this {resource_name}."
        )
    return True
