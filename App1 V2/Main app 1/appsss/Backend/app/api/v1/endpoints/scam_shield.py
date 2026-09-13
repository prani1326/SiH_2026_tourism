from fastapi import APIRouter, Header, Query
from typing import Optional
from pydantic import BaseModel
from app.services.scam_intelligence_service import scam_intelligence_service

router = APIRouter()

class ScamReportRequest(BaseModel):
    destination: str = "Jaipur"
    scam_type: str
    location: str
    description: str
    estimated_loss_inr: Optional[float] = None

@router.get("/nearby")
async def get_nearby_scams(
    destination: str = Query("Jaipur", description="Current destination")
):
    return scam_intelligence_service.get_nearby_scam_alerts(destination=destination)

@router.post("/report")
async def report_scam(
    request: ScamReportRequest,
    authorization: Optional[str] = Header(None)
):
    uid = "demo_user"
    return scam_intelligence_service.report_scam_incident(
        user_id=uid,
        destination=request.destination,
        scam_type=request.scam_type,
        location=request.location,
        description=request.description,
        estimated_loss_inr=request.estimated_loss_inr
    )
