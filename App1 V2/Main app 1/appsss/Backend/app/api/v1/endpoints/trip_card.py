from fastapi import APIRouter, HTTPException
from app.schemas.trip import DigitalTripCardOut
from app.services.offline_service import offline_service

router = APIRouter()

@router.get("/{trip_id}/card", response_model=DigitalTripCardOut)
def get_digital_trip_card(trip_id: str):
    """
    Digital Trip Card with Firestore (Section 16).
    Produces consolidated tourist passport, verified vouchers, QR code,
    emergency contacts, embassy details, and insurance info.
    """
    try:
        return offline_service.generate_trip_card(None, trip_id)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))

@router.get("/{trip_id}/offline-package")
def download_offline_package(trip_id: str):
    """
    Offline Mode Data Bundler with Firestore (Section 17).
    Compiles complete offline JSON snapshot for client-side local caching,
    enabling full operation with poor or zero internet connectivity.
    """
    try:
        return offline_service.get_full_offline_package(None, trip_id)
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
