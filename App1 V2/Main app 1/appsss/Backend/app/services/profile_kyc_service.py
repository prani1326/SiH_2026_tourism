import uuid
from datetime import datetime, timezone
from typing import Dict, Any, List, Optional
from app.models.firestore_models import User, UserProfile, UserAddress, KycData
from app.repositories.user_repo import user_repo

def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()

class ProfileKycService:
    """
    Centralized Profile Completion Engine and Trip Planning Eligibility Service (Section 15, 19).
    Calculates exact profile completion and enforces KYC verification rules.
    """

    REQUIRED_PROFILE_FIELDS = [
        "full_name",
        "date_of_birth",
        "nationality",
        "language",
        "email",
        "phone",
        "address_line1",
        "city",
        "state",
        "country",
        "postal_code"
    ]

    def calculate_profile_completion(self, user: User) -> float:
        """Calculate profile completion percentage based on required fields."""
        if not user:
            return 0.0

        profile = getattr(user, "profile", None)
        address = getattr(user, "address", None)

        full_name = getattr(user, "full_name", "") or ""
        dob = getattr(profile, "date_of_birth", "") or getattr(user, "date_of_birth", "") or ""
        nationality = getattr(profile, "nationality", "") or ""
        language = getattr(profile, "language", "") or getattr(profile, "preferred_language", "") or ""
        email = getattr(user, "email", "") or ""
        phone = getattr(user, "phone", "") or ""
        
        addr1 = getattr(address, "address_line1", "") if address else ""
        city = getattr(address, "city", "") if address else ""
        state = getattr(address, "state", "") if address else ""
        country = getattr(address, "country", "") if address else ""
        postal_code = getattr(address, "postal_code", "") if address else ""

        checks = [
            bool(full_name.strip()),
            bool(dob.strip()),
            bool(nationality.strip()),
            bool(language.strip()),
            bool(email.strip()),
            bool(phone.strip()),
            bool(addr1.strip()),
            bool(city.strip()),
            bool(state.strip()),
            bool(country.strip()),
            bool(postal_code.strip())
        ]

        completed = sum(1 for c in checks if c)
        total = len(checks)
        return round((completed / float(total)) * 100.0, 1)

    def check_trip_eligibility(self, user_id: str) -> Dict[str, Any]:
        """
        Evaluate if traveler meets both criteria:
        1. Profile Required Fields Complete (profileCompletion == 100%)
        2. KYC Successfully Verified (kycStatus == 'VERIFIED' and kycVerified == True)
        """
        user = user_repo.get_by_id(user_id)
        if not user:
            return {
                "eligible": False,
                "profile_complete": False,
                "kyc_verified": False,
                "profile_completion": 0.0,
                "kyc_status": "NOT_STARTED",
                "missing_requirements": ["USER_NOT_FOUND"],
                "message": "User not found. Please log in again."
            }

        profile_completion = self.calculate_profile_completion(user)
        profile_complete = profile_completion >= 100.0
        kyc_status = getattr(user, "kyc_status", "NOT_STARTED") or "NOT_STARTED"
        kyc_verified = bool(getattr(user, "kyc_verified", False)) and kyc_status == "VERIFIED"

        missing = []
        profile = getattr(user, "profile", None)
        address = getattr(user, "address", None)

        if not (getattr(user, "full_name", None) and getattr(profile, "date_of_birth", None) and getattr(profile, "nationality", None)):
            missing.append("PERSONAL_DETAILS")

        if not (getattr(user, "phone", None) and getattr(user, "email", None)):
            missing.append("CONTACT_DETAILS")

        if not (address and getattr(address, "address_line1", None) and getattr(address, "city", None) and getattr(address, "postal_code", None)):
            missing.append("ADDRESS_DETAILS")

        if not kyc_verified:
            missing.append("KYC_VERIFICATION")

        eligible = profile_complete and kyc_verified

        return {
            "eligible": eligible,
            "profile_complete": profile_complete,
            "kyc_verified": kyc_verified,
            "profile_completion": profile_completion,
            "kyc_status": kyc_status,
            "missing_requirements": missing,
            "message": "Trip Planning is unlocked." if eligible else "Complete your profile and KYC to unlock Trip Planning."
        }

    def save_onboarding_step(self, user_id: str, step: int, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Save onboarding step data (Step 1: Personal, Step 2: Address, Step 3: KYC/Skip)."""
        user = user_repo.get_by_id(user_id)
        if not user:
            return {"success": False, "message": "User not found"}

        updates: Dict[str, Any] = {"onboarding_step": step, "updated_at": _now_iso()}
        profile_updates: Dict[str, Any] = {}
        address_updates: Dict[str, Any] = {}

        if step == 1:
            if "full_name" in payload:
                updates["full_name"] = payload["full_name"]
            if "first_name" in payload:
                profile_updates["first_name"] = payload["first_name"]
            if "last_name" in payload:
                profile_updates["last_name"] = payload["last_name"]
            if "date_of_birth" in payload:
                profile_updates["date_of_birth"] = payload["date_of_birth"]
            if "gender" in payload:
                profile_updates["gender"] = payload["gender"]
            if "nationality" in payload:
                profile_updates["nationality"] = payload["nationality"]
            if "language" in payload:
                profile_updates["language"] = payload["language"]
                profile_updates["preferred_language"] = payload["language"]
            if "avatar_url" in payload:
                profile_updates["avatar_url"] = payload["avatar_url"]

        elif step == 2:
            if "phone" in payload:
                updates["phone"] = payload["phone"]
                updates["phone_verified"] = True
            if "email" in payload:
                updates["email"] = payload["email"]
                updates["email_verified"] = True
            
            for k in ["address_line1", "address_line2", "city", "state", "country", "postal_code"]:
                if k in payload:
                    address_updates[k] = payload[k]

        # Apply updates
        if profile_updates:
            user_repo.update_profile(user_id, profile_updates)
        if address_updates:
            # Update user address in repository
            user_addr = getattr(user, "address", None)
            cur_addr = user_addr.to_dict() if user_addr and hasattr(user_addr, "to_dict") else {}
            cur_addr.update(address_updates)
            updates["address"] = cur_addr

        # Update root attributes
        user_repo.update(user_id, updates)

        # Recalculate completion
        refreshed_user = user_repo.get_by_id(user_id)
        completion = self.calculate_profile_completion(refreshed_user) if refreshed_user else 0.0
        
        status = "PROFILE_COMPLETE" if completion >= 100.0 else "PROFILE_INCOMPLETE"
        user_repo.update(user_id, {"profile_completion": completion, "profile_status": status})

        return {
            "success": True,
            "step": step,
            "profile_completion": completion,
            "profile_status": status,
            "message": f"Step {step} saved successfully."
        }

    def submit_kyc(self, user_id: str, document_type: str, document_number: str) -> Dict[str, Any]:
        """Submit traveler KYC documents for identity verification."""
        user = user_repo.get_by_id(user_id)
        if not user:
            return {"success": False, "message": "User not found"}

        now = _now_iso()
        kyc_data = {
            "status": "PENDING",
            "document_type": document_type,
            "document_number": document_number,
            "verification_provider": "National Identity Verification Service",
            "verification_reference": f"KYC-{uuid.uuid4().hex[:8].upper()}",
            "submitted_at": now
        }

        user_repo.update(user_id, {
            "kyc_status": "PENDING",
            "kyc_verified": False,
            "kyc": kyc_data,
            "updated_at": now
        })

        return {
            "success": True,
            "kyc_status": "PENDING",
            "verification_reference": kyc_data["verification_reference"],
            "message": "KYC submitted. Verification in progress."
        }

    def verify_kyc(self, user_id: str, document_type: str = "Passport", document_number: str = "P12345678") -> Dict[str, Any]:
        """
        Verify KYC upon successful government document validation.
        Transitions state to VERIFIED and unlocks Trip Planning if profile is complete.
        """
        user = user_repo.get_by_id(user_id)
        if not user:
            return {"success": False, "message": "User not found"}

        now = _now_iso()
        kyc_data = {
            "status": "VERIFIED",
            "document_type": document_type,
            "document_number": document_number,
            "verification_provider": "Government Identity Service (Verified)",
            "verification_reference": f"KYC-VERIFIED-{uuid.uuid4().hex[:6].upper()}",
            "submitted_at": getattr(getattr(user, "kyc", None), "submitted_at", now) or now,
            "verified_at": now
        }

        completion = self.calculate_profile_completion(user)
        user_repo.update(user_id, {
            "kyc_status": "VERIFIED",
            "kyc_verified": True,
            "kyc": kyc_data,
            "profile_status": "PROFILE_COMPLETE" if completion >= 100.0 else "KYC_VERIFIED",
            "updated_at": now
        })

        return {
            "success": True,
            "kyc_status": "VERIFIED",
            "kyc_verified": True,
            "verification_reference": kyc_data["verification_reference"],
            "message": "Identity successfully verified. Trip Planning is now unlocked."
        }

    def skip_kyc(self, user_id: str) -> Dict[str, Any]:
        """Skip KYC during initial onboarding. Saves state as SKIPPED and profile as INCOMPLETE."""
        user = user_repo.get_by_id(user_id)
        if not user:
            return {"success": False, "message": "User not found"}

        now = _now_iso()
        kyc_data = {
            "status": "SKIPPED",
            "verification_provider": "None",
            "verification_reference": "",
            "submitted_at": None,
            "verified_at": None
        }

        user_repo.update(user_id, {
            "kyc_status": "SKIPPED",
            "kyc_verified": False,
            "kyc": kyc_data,
            "profile_status": "KYC_SKIPPED",
            "onboarding_step": 3,
            "updated_at": now
        })

        return {
            "success": True,
            "kyc_status": "SKIPPED",
            "kyc_verified": False,
            "message": "KYC skipped. You can complete verification later from your Profile screen."
        }

profile_kyc_service = ProfileKycService()
