from typing import Optional, Dict, Any, List
from pydantic import BaseModel, EmailStr, Field

class SignupRequest(BaseModel):
    email: Optional[EmailStr] = None
    phone: Optional[str] = None
    password: str = Field(..., min_length=6)
    full_name: str = "Traveler"
    emergency_recovery_pin: Optional[str] = None

class LoginRequest(BaseModel):
    email: Optional[str] = None
    phone: Optional[str] = None
    password: str

class PhoneOtpSendRequest(BaseModel):
    phone: str = Field(..., description="Phone number with country code, e.g. +919876543210")

class PhoneOtpVerifyRequest(BaseModel):
    phone: str
    otp_code: str = Field(..., min_length=4, max_length=6)

class GoogleLoginRequest(BaseModel):
    id_token: str

class RefreshTokenRequest(BaseModel):
    refresh_token: str

class PasswordRecoveryRequest(BaseModel):
    email: Optional[EmailStr] = None
    phone: Optional[str] = None

class PasswordRecoveryConfirm(BaseModel):
    identifier: str
    recovery_code: str
    new_password: str = Field(..., min_length=6)

class DeviceRegisterRequest(BaseModel):
    fcm_token: str = Field(..., description="Firebase Cloud Messaging device registration token")
    device_id: Optional[str] = None
    device_name: Optional[str] = "Mobile Device"

class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int
    user: Dict[str, Any]

class SplashInitResponse(BaseModel):
    app_name: str
    version: str
    supported_currencies: List[str]
    supported_languages: List[str]
    emergency_baseline: Dict[str, str]
    feature_flags: Dict[str, bool]
