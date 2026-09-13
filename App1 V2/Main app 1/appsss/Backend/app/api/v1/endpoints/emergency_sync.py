from fastapi import APIRouter, Header, Query
from typing import Optional
from pydantic import BaseModel
from app.services.offline_emergency_service import offline_emergency_service

router = APIRouter()

class OfflineSosSyncRequest(BaseModel):
    packet_id: str
    latitude: float
    longitude: float
    timestamp_offline: str
    emergency_type: str = "SOS_OFFLINE"
    medical_notes: Optional[str] = None

@router.get("/bundle")
async def get_emergency_bundle(
    destination: str = Query("Jaipur", description="Current destination")
):
    return offline_emergency_service.get_emergency_bundle(destination=destination)

@router.post("/sync")
async def sync_offline_sos(
    request: OfflineSosSyncRequest,
    authorization: Optional[str] = Header(None)
):
    uid = "demo_user"
    return offline_emergency_service.sync_offline_sos(
        user_id=uid,
        packet_id=request.packet_id,
        latitude=request.latitude,
        longitude=request.longitude,
        timestamp_offline=request.timestamp_offline,
        emergency_type=request.emergency_type,
        medical_notes=request.medical_notes
    )
