from fastapi import APIRouter, Header
from typing import Optional, List, Dict
from pydantic import BaseModel
from app.services.guardian_service import guardian_service

router = APIRouter()

class StartGuardianRequest(BaseModel):
    trip_name: str = "Jaipur Heritage Tour"
    trusted_contacts: Optional[List[Dict[str, str]]] = None
    check_in_interval_hours: int = 3

class CheckInRequest(BaseModel):
    battery_level: int = 74
    location_name: Optional[str] = "Amber Fort, Jaipur"

class PauseSharingRequest(BaseModel):
    enable: bool = True

@router.post("/start")
async def start_guardian(
    request: StartGuardianRequest,
    authorization: Optional[str] = Header(None)
):
    uid = "demo_user"
    return guardian_service.start_guardian_session(
        user_id=uid,
        trip_name=request.trip_name,
        trusted_contacts=request.trusted_contacts,
        check_in_interval_hours=request.check_in_interval_hours
    )

@router.post("/check-in")
async def check_in_safe(
    request: CheckInRequest,
    authorization: Optional[str] = Header(None)
):
    uid = "demo_user"
    return guardian_service.check_in_safe(
        user_id=uid,
        battery_level=request.battery_level,
        location_name=request.location_name
    )

@router.post("/sharing-toggle")
async def toggle_sharing(
    request: PauseSharingRequest,
    authorization: Optional[str] = Header(None)
):
    uid = "demo_user"
    return guardian_service.pause_or_resume_sharing(user_id=uid, enable=request.enable)

@router.get("/status")
async def get_guardian_status(
    authorization: Optional[str] = Header(None)
):
    uid = "demo_user"
    return guardian_service.get_guardian_status(user_id=uid)
