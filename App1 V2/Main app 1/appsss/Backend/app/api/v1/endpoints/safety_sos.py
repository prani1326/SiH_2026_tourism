from fastapi import APIRouter, Depends, HTTPException
from app.core.firestore_db import get_firestore
from app.core.security import get_current_user_optional
from app.repositories.destination_repo import destination_repo
from app.schemas.safety import (
    SOSAlertRequest,
    SOSAlertResponse,
    DeviceHeartbeatRequest,
    HeartbeatResponse,
    ImSafeRequest,
    NeedHelpRequest,
    SafetyActionResponse
)
from app.services.safety_service import safety_service

router = APIRouter()

@router.post("/sos", response_model=SOSAlertResponse)
def trigger_sos_alert(
    request: SOSAlertRequest,
    current_user = Depends(get_current_user_optional)
):
    """
    One-Tap SOS & Emergency Dispatcher in Firestore.
    Dispatches GPS coordinates to local emergency services and notifies
    trusted emergency contacts via priority channels.
    """
    user_id = str(current_user.id) if current_user else "anonymous-tourist"
    return safety_service.trigger_sos(
        user_id=user_id,
        latitude=request.latitude,
        longitude=request.longitude,
        address=request.address,
        trip_id=request.trip_id,
        emergency_type=request.emergency_type
    )

@router.post("/sos/{alert_id}/resolve")
def resolve_sos_alert(alert_id: str):
    """Resolve or Cancel SOS Alert in Firestore."""
    firestore = get_firestore()
    doc_ref = firestore.collection("sos_alerts").document(alert_id)
    doc = doc_ref.get()
    if not doc.exists:
        raise HTTPException(status_code=404, detail="SOS alert not found.")

    doc_ref.update({
        "status": "RESOLVED",
        "liveLocationActive": False
    })
    return {"success": True, "message": "Emergency alert marked as resolved in Firestore."}

@router.post("/heartbeat", response_model=HeartbeatResponse)
def receive_device_heartbeat(
    request: DeviceHeartbeatRequest,
    current_user = Depends(get_current_user_optional)
):
    """
    Receives device telemetry (battery, charging, network, location, app state).
    Updates device status and evaluates safety states.
    """
    user_id = str(current_user.id) if current_user else "anonymous-tourist"
    return safety_service.process_heartbeat(user_id=user_id, request=request)

@router.post("/im-safe", response_model=SafetyActionResponse)
def report_user_safe(
    request: ImSafeRequest,
    current_user = Depends(get_current_user_optional)
):
    """
    Traveler confirms 'I Am Safe', canceling pending non-SOS escalations.
    """
    user_id = str(current_user.id) if current_user else "anonymous-tourist"
    return safety_service.report_im_safe(user_id=user_id, request=request)

@router.post("/need-help", response_model=SafetyActionResponse)
def report_need_help(
    request: NeedHelpRequest,
    current_user = Depends(get_current_user_optional)
):
    """
    Traveler requests immediate safety check and triggers escalation workflow.
    """
    user_id = str(current_user.id) if current_user else "anonymous-tourist"
    return safety_service.report_need_help(user_id=user_id, request=request)

@router.post("/check-escalations")
def check_device_escalations():
    """
    Scheduled job endpoint to check unreachable devices and advance grace period escalations.
    """
    return safety_service.run_escalation_engine()

@router.get("/intelligence/{destination_id}")
def get_safety_intelligence(destination_id: str):
    """Safety Intelligence & Risk-Aware Route Guidance from Firestore."""
    dest = destination_repo.get_by_id(destination_id)
    if not dest:
        results = destination_repo.search(query=destination_id, limit=1)
        dest = results[0] if results else None

    if not dest:
        raise HTTPException(status_code=404, detail="Destination not found.")

    return {
        "destination": dest.name,
        "overall_safety_score": dest.safety_score,
        "daytime_safety_index": "98/100 (Extremely Safe)",
        "nighttime_safety_index": "90/100 (Well-lit tourist corridors recommended)",
        "solo_female_safety_rating": "4.8/5.0",
        "verified_help_points": [
            {"name": "Tourist Police Central Kiosk", "location": "Monument Gateway", "phone": "1363"},
            {"name": "Metro Help Desk & First Aid", "location": "Central Metro Station", "phone": "112"}
        ],
        "scam_intelligence": dest.scam_alerts,
        "risk_aware_route_advice": [
            "Use illuminated arterial roads (e.g. VIP Heritage Marg) after 10:00 PM.",
            "Authorized prepaid taxi counters located inside arrival concourses."
        ]
    }
