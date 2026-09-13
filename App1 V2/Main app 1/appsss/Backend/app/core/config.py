import os
import sys
from typing import Any, List, Optional, Union
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import field_validator, ValidationInfo

class Settings(BaseSettings):
    # Environment & Service Identification
    PROJECT_NAME: str = "Tourist App Platform API"
    VERSION: str = "2.0.0"
    DESCRIPTION: str = "Enterprise Tourist & Travel Platform Backend API"
    API_V1_STR: str = "/api/v1"
    ENVIRONMENT: str = "development"  # development, staging, production, test
    DEBUG: bool = False

    # Security & Cryptography
    SECRET_KEY: str = "INSECURE_DEV_SECRET_KEY_CHANGE_IN_PRODUCTION_09f26e402586e2d"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    # Primary Database: Cloud Firestore (Single Source of Truth)
    USE_FIRESTORE: bool = True
    FIRESTORE_PROJECT_ID: Optional[str] = "trip-planner-version-1"
    FIRESTORE_DATABASE_ID: str = "(default)"
    FIRESTORE_EMULATOR_HOST: Optional[str] = None

    # Firebase Services (Auth, Storage, FCM Notifications)
    FIREBASE_CREDENTIALS_PATH: Optional[str] = None
    FIREBASE_PROJECT_ID: Optional[str] = "trip-planner-version-1"
    FIREBASE_STORAGE_BUCKET: Optional[str] = "trip-planner-version-1.appspot.com"
    FIREBASE_MOCK_MODE: bool = False  # Strict real Firebase in production

    # Redis Cache & Distributed State
    REDIS_ENABLED: bool = False
    REDIS_URL: str = "redis://localhost:6379/0"
    REDIS_MAX_CONNECTIONS: int = 50
    REDIS_SOCKET_TIMEOUT: float = 3.0
    REDIS_CONNECT_TIMEOUT: float = 2.0

    # Payment Gateway: Razorpay
    RAZORPAY_KEY_ID: str = ""
    RAZORPAY_KEY_SECRET: str = ""
    RAZORPAY_WEBHOOK_SECRET: str = ""
    RAZORPAY_CURRENCY: str = "INR"

    # Maps & Geocoding: Google Maps Platform
    GOOGLE_MAPS_API_KEY: str = ""

    # Weather: OpenWeather API
    OPENWEATHER_API_KEY: str = ""

    # Generative AI Trip Planning (Gemini)
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-2.5-flash"
    AI_MODEL_NAME: str = "gemini-2.5-flash"
    AI_MAX_OUTPUT_TOKENS: int = 4096
    AI_TEMPERATURE: float = 0.4

    # Communications: Email & SMS
    SMTP_HOST: str = ""
    SMTP_PORT: int = 587
    SMTP_USER: str = ""
    SMTP_PASSWORD: str = ""
    SMTP_FROM_EMAIL: str = "alerts@touristplatform.gov.in"
    TWILIO_ACCOUNT_SID: str = ""
    TWILIO_AUTH_TOKEN: str = ""
    TWILIO_FROM_PHONE: str = ""

    # Rate Limiting & Idempotency
    RATE_LIMIT_PER_MINUTE: int = 120
    IDEMPOTENCY_EXPIRY_SECONDS: int = 86400

    # Globalization
    DEFAULT_CURRENCY: str = "INR"
    SUPPORTED_CURRENCIES: List[str] = ["INR", "USD", "EUR", "GBP", "AED", "SGD", "JPY", "AUD"]
    SUPPORTED_LANGUAGES: List[str] = ["en", "hi", "es", "fr", "de", "ja", "ar", "ru"]

    # SOS Emergency default dispatch contacts (India baseline)
    EMERGENCY_POLICE: str = "112"
    EMERGENCY_AMBULANCE: str = "102"
    EMERGENCY_TOURIST_HELPLINE: str = "1363"
    EMERGENCY_WOMEN_SAFETY: str = "1091"

    # CORS
    BACKEND_CORS_ORIGINS: List[str] = [
        "http://localhost:3000",
        "http://localhost:5173",
        "http://localhost:8000",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:8000",
        "http://10.0.2.2:8000",
        "http://172.20.10.8:8000",
        "http://172.20.10.8:3000"
    ]
    ALLOWED_ORIGINS: List[str] = ["*"]

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=True
    )

    @field_validator("GEMINI_API_KEY")
    @classmethod
    def validate_gemini_api_key(cls, v: str) -> str:
        if not v:
            v = os.environ.get("GOOGLE_API_KEY", "")
        return v

    @field_validator("BACKEND_CORS_ORIGINS", mode="before")
    @classmethod
    def assemble_cors_origins(cls, v: Any) -> List[str]:
        if isinstance(v, str):
            if v.strip().startswith("[") and v.strip().endswith("]"):
                import json
                try:
                    return json.loads(v)
                except Exception:
                    pass
            return [i.strip() for i in v.split(",") if i.strip()]
        elif isinstance(v, (list, tuple)):
            return [str(i).strip() for i in v if str(i).strip()]
        return v

    @field_validator("BACKEND_CORS_ORIGINS")
    @classmethod
    def validate_cors_origins_production(cls, v: List[str], info: ValidationInfo) -> List[str]:
        env = info.data.get("ENVIRONMENT", "development")
        if env == "production" and "*" in v:
            raise ValueError(
                "Wildcard '*' origin is strictly prohibited for BACKEND_CORS_ORIGINS in production mode! "
                "Specify explicit allowed domain origins."
            )
        return v

    @field_validator("SECRET_KEY")
    @classmethod
    def validate_secret_key(cls, v: str, info: ValidationInfo) -> str:
        env = info.data.get("ENVIRONMENT", "development")
        if env == "production":
            if "INSECURE_DEV_SECRET" in v or "change-in-prod" in v.lower():
                raise ValueError("Default insecure SECRET_KEY cannot be used in production! Generate a secure secret.")
            if len(v) < 32:
                raise ValueError("Production SECRET_KEY must be at least 32 characters long.")
        return v

    @field_validator("DEBUG")
    @classmethod
    def validate_debug_production(cls, v: bool, info: ValidationInfo) -> bool:
        env = info.data.get("ENVIRONMENT", "development")
        if env == "production" and v is True:
            raise ValueError("DEBUG mode must be disabled (False) in production!")
        return v

    @field_validator("FIREBASE_MOCK_MODE")
    @classmethod
    def validate_firebase_mock_mode(cls, v: bool, info: ValidationInfo) -> bool:
        env = info.data.get("ENVIRONMENT", "development")
        if env == "production" and v is True:
            raise ValueError("FIREBASE_MOCK_MODE cannot be enabled in production! Real Firebase is mandatory.")
        return v

settings = Settings()
