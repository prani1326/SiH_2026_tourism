from fastapi import APIRouter, Depends, HTTPException
from app.core.firestore_db import get_firestore
from app.core.security import revoke_all_user_tokens
from app.schemas.safety import LostPhoneRecoverRequest, LostPhoneRecoverResponse
from app.services.safety_service import safety_service

router = APIRouter()

@router.post("/recover", response_model=LostPhoneRecoverResponse)
def recover_lost_phone_mode(request: LostPhoneRecoverRequest):
    """
    Lost Phone Mode Portal with Firestore (Section 18).
    Dedicated recovery mechanism allowing stranded tourists to access itineraries,
    hotel vouchers, and emergency help from ANY browser with email + PIN.
    """
    try:
        return safety_service.recover_lost_phone(
            email=request.email,
            emergency_recovery_pin=request.emergency_recovery_pin
        )
    except ValueError as e:
        raise HTTPException(status_code=401, detail=str(e))

@router.post("/freeze/{recovery_token}")
def freeze_tourist_account(recovery_token: str):
    """Remotely Freeze Tourist Account & Invalidate All Active Sessions in Firestore (Section 18)."""
    firestore = get_firestore()
    snaps = firestore.collection("lost_phone_sessions").where("recovery_token", "==", recovery_token).limit(1).get()
    if not snaps:
        raise HTTPException(status_code=404, detail="Invalid recovery session.")

    session_doc = snaps[0]
    session_data = session_doc.to_dict()
    user_id = session_data.get("user_id")

    firestore.collection("lost_phone_sessions").document(session_doc.id).update({"is_card_frozen": True})
    if user_id:
        revoke_all_user_tokens(user_id)

    return {
        "success": True,
        "message": "Account, payment credentials, and app access successfully frozen. All active tokens revoked."
    }
