import uuid
from datetime import datetime, timezone
from typing import Optional, List, Dict, Any
from pydantic import BaseModel
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from app.core.security import get_current_user
from app.core.firebase import firebase_service
from app.models.firestore_models import User, EmergencyContact
from app.repositories.user_repo import user_repo
from app.services.profile_kyc_service import profile_kyc_service
from app.schemas.user import (
    UserOut, UserProfileUpdate, TravelPreferencesUpdate,
    EmergencyContactCreate, EmergencyContactOut
)

router = APIRouter()

class ProfileStepRequest(BaseModel):
    step: int
    data: Dict[str, Any]

class KycSubmitRequest(BaseModel):
    document_type: str = "Passport"
    document_number: str
    document_image_url: Optional[str] = None
    expiry_date: Optional[str] = None

class KycVerifyRequest(BaseModel):
    document_type: str = "Passport"
    document_number: str = "P12345678"

@router.get("/me")
def get_current_profile(current_user: User = Depends(get_current_user)):
    """Current User Profile & Preferences from Firestore (Section 42)."""
    user = user_repo.get_by_id(str(current_user.id)) or current_user
    profile = getattr(user, "profile", None)
    prefs = getattr(user, "preferences", None)
    address = getattr(user, "address", None)
    kyc = getattr(user, "kyc", None)
    contacts = getattr(user, "emergency_contacts", [])

    completion = profile_kyc_service.calculate_profile_completion(user)
    kyc_status = getattr(user, "kyc_status", "NOT_STARTED")
    kyc_verified = bool(getattr(user, "kyc_verified", False))

    return {
        "id": str(user.id),
        "email": user.email,
        "phone": user.phone,
        "full_name": user.full_name,
        "role": user.role,
        "is_verified": user.is_verified,
        "email_verified": getattr(user, "email_verified", True),
        "phone_verified": getattr(user, "phone_verified", True),
        "profile_status": getattr(user, "profile_status", "PROFILE_INCOMPLETE"),
        "profile_completion": completion,
        "kyc_status": kyc_status,
        "kyc_verified": kyc_verified,
        "onboarding_step": getattr(user, "onboarding_step", 1),
        "profile": {
            "avatar_url": getattr(profile, "avatar_url", None),
            "bio": getattr(profile, "bio", None),
            "first_name": getattr(profile, "first_name", None),
            "last_name": getattr(profile, "last_name", None),
            "date_of_birth": getattr(profile, "date_of_birth", None),
            "gender": getattr(profile, "gender", "Prefer not to say"),
            "nationality": getattr(profile, "nationality", "Indian"),
            "language": getattr(profile, "language", "English"),
            "preferred_language": getattr(profile, "preferred_language", "English"),
            "currency": getattr(profile, "currency", "INR"),
            "walking_tolerance": getattr(profile, "walking_tolerance", "Moderate"),
            "pace": getattr(profile, "pace", "Moderate")
        } if profile else None,
        "address": {
            "address_line1": getattr(address, "address_line1", "") if address else "",
            "address_line2": getattr(address, "address_line2", "") if address else "",
            "city": getattr(address, "city", "") if address else "",
            "state": getattr(address, "state", "") if address else "",
            "country": getattr(address, "country", "India") if address else "India",
            "postal_code": getattr(address, "postal_code", "") if address else ""
        },
        "kyc": {
            "status": getattr(kyc, "status", kyc_status) if kyc else kyc_status,
            "document_type": getattr(kyc, "document_type", "Passport") if kyc else "Passport",
            "verification_provider": getattr(kyc, "verification_provider", "Government Identity Services") if kyc else "Government Identity Services",
            "verification_reference": getattr(kyc, "verification_reference", "") if kyc else "",
            "submitted_at": getattr(kyc, "submitted_at", None) if kyc else None,
            "verified_at": getattr(kyc, "verified_at", None) if kyc else None
        },
        "preferences": {
            "preferred_destinations": getattr(prefs, "preferred_destinations", []),
            "budget_range": getattr(prefs, "budget_range", "Moderate"),
            "travel_styles": getattr(prefs, "travel_styles", ["Solo"]),
            "interests": getattr(prefs, "interests", ["Heritage", "Food"]),
            "dietary_preferences": getattr(prefs, "dietary_preferences", ["Vegetarian"])
        } if prefs else None,
        "emergency_contacts": [
            {
                "id": str(c.id if hasattr(c, "id") else c.get("id")),
                "contact_name": getattr(c, "contact_name", None) or (c.get("contact_name") if isinstance(c, dict) else ""),
                "relationship_type": getattr(c, "relationship_type", None) or (c.get("relationship_type") if isinstance(c, dict) else "Family"),
                "phone_number": getattr(c, "phone_number", None) or (c.get("phone_number") if isinstance(c, dict) else ""),
                "email": getattr(c, "email", None) or (c.get("email") if isinstance(c, dict) else None),
                "is_primary": getattr(c, "is_primary", False) or (c.get("is_primary") if isinstance(c, dict) else False)
            } for c in contacts
        ]
    }

@router.get("/trip-eligibility")
def get_trip_eligibility(current_user: User = Depends(get_current_user)):
    """Check Trip Planning eligibility based on Profile Completion + KYC Verification."""
    return profile_kyc_service.check_trip_eligibility(str(current_user.id))

@router.post("/onboarding/profile-step")
def save_onboarding_profile_step(
    request: ProfileStepRequest,
    current_user: User = Depends(get_current_user)
):
    """Save 3-Step Profile Onboarding (Step 1: Personal, Step 2: Address, Step 3: KYC/Skip)."""
    return profile_kyc_service.save_onboarding_step(str(current_user.id), request.step, request.data)

@router.post("/kyc/submit")
def submit_kyc(
    request: KycSubmitRequest,
    current_user: User = Depends(get_current_user)
):
    """Submit KYC details for identity verification."""
    return profile_kyc_service.submit_kyc(str(current_user.id), request.document_type, request.document_number)

@router.post("/kyc/verify")
def verify_kyc(
    request: KycVerifyRequest,
    current_user: User = Depends(get_current_user)
):
    """Verify traveler identity and unlock Trip Planning."""
    return profile_kyc_service.verify_kyc(str(current_user.id), request.document_type, request.document_number)

@router.post("/kyc/skip")
def skip_kyc(current_user: User = Depends(get_current_user)):
    """Skip KYC during onboarding, leaving profile incomplete for later completion."""
    return profile_kyc_service.skip_kyc(str(current_user.id))

@router.put("/profile")
def update_profile(
    request: UserProfileUpdate,
    current_user: User = Depends(get_current_user)
):
    """Update User Profile details in Firestore (Section 42)."""
    user_id = str(current_user.id)
    user = user_repo.get_by_id(user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found.")

    profile_updates = {}
    if request.avatar_url is not None:
        profile_updates["avatar_url"] = request.avatar_url
    if request.bio is not None:
        profile_updates["bio"] = request.bio
    if request.nationality is not None:
        profile_updates["nationality"] = request.nationality
    if request.language is not None:
        profile_updates["language"] = request.language
    if request.currency is not None:
        profile_updates["currency"] = request.currency
    if request.walking_tolerance is not None:
        profile_updates["walking_tolerance"] = request.walking_tolerance
    if request.pace is not None:
        profile_updates["pace"] = request.pace

    if profile_updates:
        user_repo.update_profile(user_id, profile_updates)

    if request.full_name:
        user_repo.update(user_id, {"full_name": request.full_name})

    return {"success": True, "message": "Profile updated successfully in Firestore."}

@router.post("/me/avatar/upload")
async def upload_avatar(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user)
):
    """Upload user avatar to local static storage and update Firestore profile."""
    import os
    import aiofiles
    
    static_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(__file__))))), "static", "avatars")
    os.makedirs(static_dir, exist_ok=True)
    
    filename = f"{uuid.uuid4()}_{file.filename}"
    file_path = os.path.join(static_dir, filename)
    
    async with aiofiles.open(file_path, 'wb') as out_file:
        content = await file.read()
        await out_file.write(content)

    # Use localhost or 10.0.2.2 depending on emulator/device, but we'll use a relative or fixed URL for now
    # Since we are using uvicorn on 10.65.4.139 for emulator, we'll construct the URL.
    avatar_url = f"http://10.65.4.139:8000/static/avatars/{filename}"
    
    user_repo.update_profile(str(current_user.id), {"avatar_url": avatar_url})
    return {
        "success": True,
        "avatar_url": avatar_url,
        "storage_path": f"/static/avatars/{filename}",
        "is_simulated": False
    }

@router.post("/onboarding/preferences")
def save_preferences(
    request: TravelPreferencesUpdate,
    current_user: User = Depends(get_current_user)
):
    """Travel Preference Onboarding saved directly to Firestore (Section 3)."""
    user_id = str(current_user.id)
    prefs_updates = {}
    if request.preferred_destinations is not None:
        prefs_updates["preferred_destinations"] = request.preferred_destinations
    if request.budget_range is not None:
        prefs_updates["budget_range"] = request.budget_range
    if request.travel_styles is not None:
        prefs_updates["travel_styles"] = request.travel_styles
    if request.interests is not None:
        prefs_updates["interests"] = request.interests
    if request.dietary_preferences is not None:
        prefs_updates["dietary_preferences"] = request.dietary_preferences

    user_repo.update_preferences(user_id, prefs_updates)

    profile_updates = {}
    if request.walking_tolerance:
        profile_updates["walking_tolerance"] = request.walking_tolerance
    if request.language:
        profile_updates["language"] = request.language
    if request.currency:
        profile_updates["currency"] = request.currency
    if profile_updates:
        user_repo.update_profile(user_id, profile_updates)

    return {
        "success": True,
        "message": "Travel preferences onboarded successfully. Recommendations are now personalized."
    }

@router.get("/emergency-contacts", response_model=List[EmergencyContactOut])
@router.get("/me/emergency-contacts", response_model=List[EmergencyContactOut])
def list_emergency_contacts(current_user: User = Depends(get_current_user)):
    """List Emergency Contacts from Firestore (Section 19)."""
    user = user_repo.get_by_id(str(current_user.id))
    contacts = user.emergency_contacts if user else []
    return [
        {
            "id": str(getattr(c, "id", "")),
            "contact_name": getattr(c, "contact_name", None) or (c.get("contact_name") if isinstance(c, dict) else ""),
            "relationship_type": getattr(c, "relationship_type", None) or (c.get("relationship_type") if isinstance(c, dict) else "Family"),
            "phone_number": getattr(c, "phone_number", None) or (c.get("phone_number") if isinstance(c, dict) else ""),
            "email": getattr(c, "email", None) or (c.get("email") if isinstance(c, dict) else None),
            "is_primary": getattr(c, "is_primary", False) or (c.get("is_primary") if isinstance(c, dict) else False)
        } for c in contacts
    ]

@router.post("/me/emergency-contacts", response_model=EmergencyContactOut)
@router.post("/emergency-contacts", response_model=EmergencyContactOut)
def add_emergency_contact(
    request: EmergencyContactCreate,
    current_user: User = Depends(get_current_user)
):
    """Add Trusted Emergency Contact to Firestore (Section 19)."""
    contact = user_repo.add_emergency_contact(str(current_user.id), request.model_dump())
    if not contact:
        raise HTTPException(status_code=400, detail="Failed to add emergency contact.")
    return {
        "id": str(contact.id),
        "contact_name": contact.contact_name,
        "relationship_type": contact.relationship_type,
        "phone_number": contact.phone_number,
        "email": contact.email,
        "is_primary": contact.is_primary
    }

@router.delete("/me/emergency-contacts/{contact_id}")
@router.delete("/emergency-contacts/{contact_id}")
def delete_emergency_contact(
    contact_id: str,
    current_user: User = Depends(get_current_user)
):
    """Delete Emergency Contact from Firestore."""
    success = user_repo.delete_emergency_contact(str(current_user.id), contact_id)
    if not success:
        raise HTTPException(status_code=404, detail="Contact not found.")
    return {"success": True, "message": "Emergency contact deleted."}

@router.get("/gdpr/export")
def gdpr_export(current_user: User = Depends(get_current_user)):
    """GDPR Data Portability / Export from Firestore (Section 44)."""
    user = user_repo.get_by_id(str(current_user.id)) or current_user
    return {
        "user_id": str(user.id),
        "email": user.email,
        "phone": user.phone,
        "full_name": user.full_name,
        "profile": user.profile.to_dict() if hasattr(user, "profile") and user.profile else {},
        "preferences": user.preferences.to_dict() if hasattr(user, "preferences") and user.preferences else {},
        "export_timestamp": datetime.now(timezone.utc).isoformat()
    }

@router.delete("/gdpr/delete")
def gdpr_delete_account(current_user: User = Depends(get_current_user)):
    """GDPR Right to Be Forgotten / Account Deletion from Firestore (Section 44)."""
    user_repo.delete(str(current_user.id))
    return {"success": True, "message": "Your tourist account and personal data have been permanently deleted."}
