import logging
from typing import Dict, Any, Optional
from app.core.config import settings

logger = logging.getLogger("tourist_app.firebase")

class FirebaseService:
    """
    Enterprise Firebase Admin SDK integration service.
    - Real token verification for Google Sign-In and Phone OTP
    - FCM push notification dispatch with invalid token detection
    - Strict production verification enforcement (zero mock bypass in production)
    - Test-isolated mock mode for automated test suites
    """

    def __init__(self):
        self.is_initialized: bool = False
        self._init_firebase()

    def _init_firebase(self):
        try:
            import os
            import firebase_admin
            from firebase_admin import credentials

            if firebase_admin._apps:
                self.is_initialized = True
                logger.info("Firebase Admin SDK already initialized.")
                return

            cred_path = settings.FIREBASE_CREDENTIALS_PATH or os.environ.get("FIREBASE_CREDENTIALS_PATH") or os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
            if cred_path:
                if not os.path.exists(cred_path):
                    msg = f"Firebase credentials file not found at path: {cred_path}"
                    logger.error(msg)
                    if settings.ENVIRONMENT == "production":
                        raise FileNotFoundError(msg)
                else:
                    cred = credentials.Certificate(cred_path)
                    firebase_admin.initialize_app(cred, {
                        "projectId": settings.FIREBASE_PROJECT_ID or None,
                        "storageBucket": settings.FIREBASE_STORAGE_BUCKET or None
                    })
                    self.is_initialized = True
                    logger.info(f"Firebase Admin SDK successfully initialized from certificate: {cred_path}")
                    return

            if settings.ENVIRONMENT == "production":
                # In production GCP/Cloud environment, use Application Default Credentials
                cred = credentials.ApplicationDefault()
                firebase_admin.initialize_app(cred, {
                    "projectId": settings.FIREBASE_PROJECT_ID or None,
                    "storageBucket": settings.FIREBASE_STORAGE_BUCKET or None
                })
                self.is_initialized = True
                logger.info("Firebase Admin SDK initialized using Application Default Credentials.")
            else:
                logger.info("No Firebase credentials provided. Operating in development/testing mode.")
                self.is_initialized = False
        except Exception as exc:
            self.is_initialized = False
            if settings.ENVIRONMENT == "production" and not settings.FIREBASE_MOCK_MODE:
                logger.error(f"FATAL: Firebase Admin SDK initialization failed in production: {exc}")
                raise RuntimeError(
                    f"Firebase Admin SDK initialization failure: {exc}. "
                    "Ensure valid FIREBASE_CREDENTIALS_PATH or GOOGLE_APPLICATION_CREDENTIALS is set."
                )
            logger.warning(f"Firebase Admin SDK not initialized: {exc}. Operating in test mode.")

    def verify_id_token(self, id_token: str) -> Dict[str, Any]:
        """
        Verify Firebase JWT token from Google Login or Firebase Phone Auth.
        Strictly requires real Firebase verification in production.
        """
        if not id_token:
            raise ValueError("Firebase ID token must not be empty.")

        # In non-production, check for mock/dummy/test tokens first to avoid blocking on live Google endpoints
        token_clean = id_token.strip().lower()
        if settings.ENVIRONMENT != "production" and (
            token_clean.startswith("mock") or "mock" in token_clean or "dummy" in token_clean or token_clean.startswith("test")
        ):
            return {
                "uid": f"mock_firebase_user_{abs(hash(id_token)) % 100000}",
                "email": "tourist.traveler@example.com",
                "phone_number": "+919123456789",
                "name": "Global Traveler",
                "picture": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
                "auth_time": 1700000000,
                "is_simulated": True
            }

        # Real verification if initialized
        if self.is_initialized and id_token.count(".") == 2:
            try:
                from firebase_admin import auth
                decoded = auth.verify_id_token(id_token, check_revoked=True)
                return {
                    "uid": decoded.get("uid"),
                    "email": decoded.get("email"),
                    "phone_number": decoded.get("phone_number"),
                    "name": decoded.get("name", "Traveler"),
                    "picture": decoded.get("picture"),
                    "auth_time": decoded.get("auth_time"),
                    "is_simulated": False
                }
            except Exception as e:
                logger.warning(f"Firebase Admin SDK token verification failed: {e}. Falling back to Google public keys.")

        # Real Google Auth verification for Firebase ID tokens (works with public keys, no service account needed)
        if id_token.count(".") == 2:
            try:
                from google.oauth2 import id_token as google_id_token
                from google.auth.transport import requests as google_requests
                claims = google_id_token.verify_firebase_token(
                    id_token,
                    google_requests.Request(),
                    audience=settings.FIREBASE_PROJECT_ID
                )
                if claims:
                    uid = claims.get("user_id") or claims.get("sub") or claims.get("uid")
                    return {
                        "uid": uid,
                        "email": claims.get("email"),
                        "phone_number": claims.get("phone_number"),
                        "name": claims.get("name", "Traveler"),
                        "picture": claims.get("picture"),
                        "auth_time": claims.get("auth_time"),
                        "is_simulated": False
                    }
            except Exception as e:
                logger.debug(f"Direct Google public key token verification failed: {e}")

        # Non-production JWT fallback check for Firebase tokens
        if settings.ENVIRONMENT != "production" and id_token.count(".") == 2:
            try:
                import jwt
                payload = jwt.decode(id_token, options={"verify_signature": False})
                iss = payload.get("iss", "")
                aud = payload.get("aud", "")
                if iss == f"https://securetoken.google.com/{settings.FIREBASE_PROJECT_ID}" or aud == settings.FIREBASE_PROJECT_ID:
                    uid = payload.get("user_id") or payload.get("sub") or payload.get("uid")
                    if uid:
                        return {
                            "uid": uid,
                            "email": payload.get("email"),
                            "phone_number": payload.get("phone_number"),
                            "name": payload.get("name", "Traveler"),
                            "picture": payload.get("picture"),
                            "auth_time": payload.get("auth_time"),
                            "is_simulated": False
                        }
            except Exception:
                pass

        # Production check: never allow mock tokens in production
        if settings.ENVIRONMENT == "production":
            raise ValueError("Firebase service is uninitialized and token could not be verified.")

        # Test mock mode verification (strictly isolated to test / local dev)
        if token_clean.startswith("mock-") or "google" in token_clean or "firebase" in token_clean or settings.FIREBASE_MOCK_MODE:
            return {
                "uid": f"mock_firebase_user_{abs(hash(id_token)) % 100000}",
                "email": "tourist.traveler@example.com",
                "phone_number": "+919123456789",
                "name": "Global Traveler",
                "picture": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
                "auth_time": 1700000000,
                "is_simulated": True
            }

        raise ValueError("Invalid Firebase token.")

    def send_push_notification(
        self,
        fcm_token: str,
        title: str,
        body: str,
        data: Optional[Dict[str, str]] = None
    ) -> Dict[str, Any]:
        """
        Dispatches an FCM push notification to a mobile device.
        Detects invalid/unregistered tokens so the caller can deactivate them.
        """
        if not fcm_token:
            return {"success": False, "error": "Missing FCM device token", "token_invalid": False, "is_simulated": False}

        if self.is_initialized:
            try:
                from firebase_admin import messaging
                from firebase_admin.exceptions import InvalidArgumentError

                message = messaging.Message(
                    notification=messaging.Notification(title=title, body=body),
                    data={str(k): str(v) for k, v in (data or {}).items()},
                    token=fcm_token
                )
                message_id = messaging.send(message)
                logger.info(f"FCM message dispatched successfully: {message_id}")
                return {
                    "success": True,
                    "message_id": message_id,
                    "token_invalid": False,
                    "is_simulated": False
                }
            except Exception as exc:
                exc_str = str(exc).lower()
                is_unregistered = "unregistered" in exc_str or "not-found" in exc_str or "invalid registration" in exc_str
                logger.warning(f"FCM push failed for token {fcm_token[:10]}...: {exc} (unregistered={is_unregistered})")
                return {
                    "success": False,
                    "error": str(exc),
                    "token_invalid": is_unregistered,
                    "is_simulated": False
                }

        # Simulated push notification for development/testing
        logger.info(f"[SIMULATED FCM PUSH] To: {fcm_token} | Title: '{title}' | Body: '{body}'")
        return {
            "success": True,
            "message_id": f"simulated_fcm_{abs(hash(fcm_token + title))}",
            "token_invalid": False,
            "is_simulated": True
        }

    def upload_file(
        self,
        file_bytes: bytes,
        destination_path: str,
        content_type: str = "application/octet-stream"
    ) -> Dict[str, Any]:
        """
        Upload a file to Firebase Storage bucket.
        Returns public / signed download URL.
        """
        if self.is_initialized:
            try:
                from firebase_admin import storage
                bucket_name = settings.FIREBASE_STORAGE_BUCKET
                bucket = storage.bucket(bucket_name)
                blob = bucket.blob(destination_path)
                blob.upload_from_string(file_bytes, content_type=content_type)
                blob.make_public()
                return {
                    "success": True,
                    "url": blob.public_url,
                    "storage_path": destination_path,
                    "is_simulated": False
                }
            except Exception as e:
                logger.error(f"Firebase Storage upload failed: {e}")
                if settings.ENVIRONMENT == "production":
                    raise RuntimeError(f"Production Firebase Storage upload failed: {e}")
                # Fallback to simulated path for non-production development
                return {
                    "success": True,
                    "url": f"https://firebasestorage.googleapis.com/v0/b/{settings.FIREBASE_STORAGE_BUCKET}/o/{destination_path.replace('/', '%2F')}?alt=media",
                    "storage_path": destination_path,
                    "is_simulated": True
                }

        if settings.ENVIRONMENT == "production":
            raise RuntimeError("Firebase Storage is not initialized in production. Cannot upload file.")

        logger.info(f"[SIMULATED STORAGE UPLOAD] Stored {len(file_bytes)} bytes at {destination_path}")
        return {
            "success": True,
            "url": f"https://storage.googleapis.com/{settings.FIREBASE_STORAGE_BUCKET}/{destination_path}",
            "storage_path": destination_path,
            "is_simulated": True
        }

    def get_firestore_client(self):
        """Retrieve Firestore Client via Firebase Admin or FirestoreManager."""
        from app.core.firestore_db import firestore_manager
        return firestore_manager.get_client()

firebase_service = FirebaseService()

