from fastapi import APIRouter, Query
from typing import Optional
from pydantic import BaseModel
from app.services.safety_intelligence_service import safety_intelligence_service

router = APIRouter()

class SafeRouteRequest(BaseModel):
    origin: str = "Hotel / Station"
    destination: str = "Hawa Mahal, Jaipur"

@router.get("/current")
async def get_current_safety(
    destination: str = Query("Jaipur", description="Current tourist destination"),
    lat: Optional[float] = Query(None),
    lon: Optional[float] = Query(None),
    hour: Optional[int] = Query(None)
):
    return safety_intelligence_service.get_current_safety_intelligence(
        destination=destination,
        latitude=lat,
        longitude=lon,
        current_hour=hour
    )

@router.post("/routes")
async def get_safe_routes(request: SafeRouteRequest):
    return safety_intelligence_service.evaluate_safe_routes(
        origin=request.origin,
        destination=request.destination
    )
