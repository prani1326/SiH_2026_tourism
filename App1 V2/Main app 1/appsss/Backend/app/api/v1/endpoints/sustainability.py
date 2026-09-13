from fastapi import APIRouter, Query
from typing import Optional
from pydantic import BaseModel
from app.services.sustainability_service import sustainability_service

router = APIRouter()

class SustainabilityRecalculateRequest(BaseModel):
    trip_id: str = "trip_demo"
    transport_mode: str = "Metro"
    has_metro_or_walking: bool = True
    homestay_or_eco_stay: bool = True
    local_dining_count: int = 5

@router.get("/{trip_id}")
async def get_trip_sustainability(
    trip_id: str,
    transport: str = Query("Cab"),
    eco_stay: bool = Query(True)
):
    return sustainability_service.calculate_trip_sustainability(
        trip_id=trip_id,
        transport_mode=transport,
        homestay_or_eco_stay=eco_stay
    )

@router.post("/recalculate")
async def recalculate_sustainability(request: SustainabilityRecalculateRequest):
    return sustainability_service.calculate_trip_sustainability(
        trip_id=request.trip_id,
        transport_mode=request.transport_mode,
        has_metro_or_walking=request.has_metro_or_walking,
        homestay_or_eco_stay=request.homestay_or_eco_stay,
        local_dining_count=request.local_dining_count
    )
