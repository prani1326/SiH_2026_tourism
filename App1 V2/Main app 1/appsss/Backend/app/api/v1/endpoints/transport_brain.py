from fastapi import APIRouter, Query
from app.services.cultural_engine import cultural_engine

router = APIRouter()

@router.get("/compare")
def compare_local_transport(city: str = Query("Agra", description="City name (e.g. Agra, Jaipur, Goa, Delhi)")):
    """
    Local Transport Brain (Section 23).
    Compares Airport-to-Hotel options: Cheapest, Fastest, Safest, Metro, Taxi, and City Passes.
    """
    return cultural_engine.compare_airport_to_hotel(city)
