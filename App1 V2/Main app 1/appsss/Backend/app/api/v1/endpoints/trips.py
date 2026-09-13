import secrets
import uuid
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException
from app.core.security import get_current_user
from app.models.firestore_models import User, Trip, TripDay, TripActivity
from app.repositories.trip_repo import trip_repo
from app.repositories.destination_repo import destination_repo
from app.schemas.trip import (
    AITripPlanRequest, TripCreateRequest, TripActivityCreate, TripActivityUpdate,
    ReorderActivitiesRequest, TripOut
)
from app.services.ai_planner_service import ai_planner_service

from app.services.profile_kyc_service import profile_kyc_service
from app.core.security import get_current_user_optional

router = APIRouter()

@router.post("/ai-plan")
def generate_ai_itinerary(
    request: AITripPlanRequest,
    current_user: Optional[User] = Depends(get_current_user_optional)
):
    """
    AI-Powered Trip Planning Engine using Firestore repositories (Section 9, 45).
    Enforces Profile Completion + KYC Verification before generating/saving a Trip Plan.
    """
    if current_user:
        eligibility = profile_kyc_service.check_trip_eligibility(str(current_user.id))
        if not eligibility.get("eligible"):
            raise HTTPException(
                status_code=403,
                detail={
                    "success": False,
                    "errorCode": "PROFILE_KYC_REQUIRED",
                    "error_code": "PROFILE_KYC_REQUIRED",
                    "message": "Complete your profile and KYC before creating a trip.",
                    "missingRequirements": eligibility.get("missing_requirements", [])
                }
            )
    return ai_planner_service.generate_itinerary(str(current_user.id) if current_user else None, request)

@router.post("/", response_model=TripOut)
@router.post("/create", response_model=TripOut)
def create_trip(
    request: TripCreateRequest,
    current_user: User = Depends(get_current_user)
):
    """Create a new Trip in Firestore (Section 10). Enforces Profile Completion + KYC Verification."""
    eligibility = profile_kyc_service.check_trip_eligibility(str(current_user.id))
    if not eligibility.get("eligible"):
        raise HTTPException(
            status_code=403,
            detail={
                "success": False,
                "errorCode": "PROFILE_KYC_REQUIRED",
                "error_code": "PROFILE_KYC_REQUIRED",
                "message": "Complete your profile and KYC before creating a trip.",
                "missingRequirements": eligibility.get("missing_requirements", [])
            }
        )

    # Create initial day
    day1 = TripDay(
        id=str(uuid.uuid4()),
        day_number=1,
        date=request.start_date,
        weather_summary="Sunny & pleasant",
        notes="Arrival and orientation",
        activities=[]
    )
    trip = Trip(
        id=str(uuid.uuid4()),
        user_id=str(current_user.id),
        destination_id=request.destination_id,
        title=request.title,
        start_date=request.start_date,
        end_date=request.end_date,
        traveler_count=request.traveler_count,
        travel_style=request.travel_style,
        total_budget=request.total_budget,
        total_estimated_cost=request.total_budget * 0.85,
        status="planning",
        days=[day1]
    )
    trip_repo.create(trip)
    return get_trip_detail(str(trip.id))

@router.get("/", response_model=List[TripOut])
def get_user_trips(
    current_user: User = Depends(get_current_user)
):
    """Get All User Trips from Firestore (Section 10)."""
    trips = trip_repo.get_user_trips(str(current_user.id))
    results = []
    for t in trips:
        dest_name = "Custom Destination"
        if t.destination_id:
            dest = destination_repo.get_by_id(t.destination_id)
            if dest:
                dest_name = dest.name

        days_out = []
        for d in t.days:
            d_dict = d.to_dict() if isinstance(d, TripDay) else d
            acts = []
            for a in d_dict.get("activities", []):
                a_dict = a.to_dict() if isinstance(a, TripActivity) else a
                acts.append({
                    "id": str(a_dict.get("id")),
                    "trip_day_id": str(a_dict.get("trip_day_id")),
                    "place_id": str(a_dict.get("place_id")) if a_dict.get("place_id") else None,
                    "time_slot": a_dict.get("time_slot", "Morning"),
                    "sequence_order": a_dict.get("sequence_order", 1),
                    "title": a_dict.get("title", ""),
                    "description": a_dict.get("description", ""),
                    "start_time": a_dict.get("start_time", "09:00 AM"),
                    "end_time": a_dict.get("end_time", "11:30 AM"),
                    "estimated_cost": float(a_dict.get("estimated_cost", 0.0)),
                    "travel_time_minutes": int(a_dict.get("travel_time_minutes", 15)),
                    "is_outdoor": bool(a_dict.get("is_outdoor", True)),
                    "is_completed": bool(a_dict.get("is_completed", False)),
                    "transport_mode": a_dict.get("transport_mode", "Cab / Auto")
                })

            days_out.append({
                "id": str(d_dict.get("id")),
                "day_number": int(d_dict.get("day_number", 1)),
                "date": d_dict.get("date", ""),
                "weather_summary": d_dict.get("weather_summary", "Sunny & clear"),
                "notes": d_dict.get("notes"),
                "activities": acts
            })

        results.append({
            "id": str(t.id),
            "title": t.title,
            "destination_id": str(t.destination_id),
            "destination_name": dest_name,
            "start_date": t.start_date,
            "end_date": t.end_date,
            "traveler_count": t.traveler_count,
            "travel_style": t.travel_style,
            "total_budget": t.total_budget,
            "total_estimated_cost": t.total_estimated_cost,
            "actual_spend": t.actual_spend,
            "budget_drift_percent": t.budget_drift_percent,
            "status": t.status,
            "share_token": t.share_token,
            "days": days_out
        })
    return results

@router.get("/{trip_id}", response_model=TripOut)
def get_trip_detail(trip_id: str):
    """Get Complete Trip & Itinerary Timeline from Firestore (Section 10)."""
    trip = trip_repo.get_by_id(trip_id)
    if not trip:
        raise HTTPException(status_code=404, detail="Trip not found.")

    dest_name = "Custom Destination"
    if trip.destination_id:
        dest = destination_repo.get_by_id(trip.destination_id)
        if dest:
            dest_name = dest.name

    days_out = []
    for d in trip.days:
        d_dict = d.to_dict() if isinstance(d, TripDay) else d
        acts = []
        for a in d_dict.get("activities", []):
            a_dict = a.to_dict() if isinstance(a, TripActivity) else a
            acts.append({
                "id": str(a_dict.get("id")),
                "trip_day_id": str(a_dict.get("trip_day_id")),
                "place_id": str(a_dict.get("place_id")) if a_dict.get("place_id") else None,
                "time_slot": a_dict.get("time_slot", "Morning"),
                "sequence_order": a_dict.get("sequence_order", 1),
                "title": a_dict.get("title", ""),
                "description": a_dict.get("description", ""),
                "start_time": a_dict.get("start_time", "09:00 AM"),
                "end_time": a_dict.get("end_time", "11:30 AM"),
                "estimated_cost": float(a_dict.get("estimated_cost", 0.0)),
                "travel_time_minutes": int(a_dict.get("travel_time_minutes", 15)),
                "is_outdoor": bool(a_dict.get("is_outdoor", True)),
                "is_completed": bool(a_dict.get("is_completed", False)),
                "transport_mode": a_dict.get("transport_mode", "Cab / Auto")
            })

        days_out.append({
            "id": str(d_dict.get("id")),
            "day_number": int(d_dict.get("day_number", 1)),
            "date": d_dict.get("date", ""),
            "weather_summary": d_dict.get("weather_summary", "Sunny & clear"),
            "notes": d_dict.get("notes"),
            "activities": acts
        })

    return {
        "id": str(trip.id),
        "title": trip.title,
        "destination_id": str(trip.destination_id),
        "destination_name": dest_name,
        "start_date": trip.start_date,
        "end_date": trip.end_date,
        "traveler_count": trip.traveler_count,
        "travel_style": trip.travel_style,
        "total_budget": trip.total_budget,
        "total_estimated_cost": trip.total_estimated_cost,
        "actual_spend": trip.actual_spend,
        "budget_drift_percent": trip.budget_drift_percent,
        "status": trip.status,
        "share_token": trip.share_token,
        "days": days_out
    }

@router.post("/{trip_id}/activities")
def add_activity_by_trip(trip_id: str, request: dict):
    trip = trip_repo.get_by_id(trip_id)
    if not trip or not trip.days:
        raise HTTPException(status_code=404, detail="Trip or trip days not found.")
    target_day = trip.days[0]
    day_id = str(getattr(target_day, "id", "") if hasattr(target_day, "id") else target_day.get("id"))
    req_dict = dict(request)
    req_dict["trip_day_id"] = req_dict.get("trip_day_id") or day_id
    act = trip_repo.add_activity(req_dict["trip_day_id"], req_dict)
    if not act:
        raise HTTPException(status_code=400, detail="Failed to add activity.")
    return {"success": True, "activity_id": str(act.id), "message": "Activity added to itinerary."}

@router.post("/activities")
def add_activity(request: TripActivityCreate):
    """Add Activity to Itinerary in Firestore (Section 10)."""
    act = trip_repo.add_activity(request.trip_day_id, request.model_dump())
    if not act:
        raise HTTPException(status_code=404, detail="Trip day not found.")
    return {"success": True, "activity_id": str(act.id), "message": "Activity added to itinerary."}

@router.put("/activities/{activity_id}")
def update_activity(
    activity_id: str,
    request: TripActivityUpdate
):
    """Edit Activity Details / Mark Completed in Firestore (Section 10)."""
    updates = {k: v for k, v in request.model_dump().items() if v is not None}
    success = trip_repo.update_activity(activity_id, updates)
    if not success:
        raise HTTPException(status_code=404, detail="Activity not found.")
    return {"success": True, "message": "Activity updated successfully."}

@router.delete("/activities/{activity_id}")
def delete_activity(activity_id: str):
    """Remove Activity from Itinerary in Firestore (Section 10)."""
    success = trip_repo.delete_activity(activity_id)
    if not success:
        raise HTTPException(status_code=404, detail="Activity not found.")
    return {"success": True, "message": "Activity removed from itinerary."}

@router.post("/days/{day_id}/reorder")
def reorder_activities(
    day_id: str,
    request: ReorderActivitiesRequest
):
    """Drag-and-Drop Reorder Day Activities in Firestore (Section 10)."""
    success = trip_repo.reorder_activities(day_id, request.activity_ids_in_order)
    if not success:
        raise HTTPException(status_code=404, detail="Trip day not found.")
    return {"success": True, "message": "Activities successfully reordered."}

@router.post("/{trip_id}/share")
def share_trip(trip_id: str):
    """Generate Shareable Link for Itinerary in Firestore (Section 10)."""
    trip = trip_repo.get_by_id(trip_id)
    if not trip:
        raise HTTPException(status_code=404, detail="Trip not found.")

    share_token = trip.share_token
    if not share_token:
        share_token = f"share-{secrets.token_urlsafe(12)}"
        trip_repo.update(trip_id, {"share_token": share_token})

    return {
        "share_token": share_token,
        "share_url": f"https://tourist-app.platform/trips/shared/{share_token}"
    }

@router.post("/{trip_id}/duplicate")
def duplicate_trip(
    trip_id: str,
    current_user: User = Depends(get_current_user)
):
    """Duplicate / Clone Itinerary in Firestore (Section 10)."""
    source_trip = trip_repo.get_by_id(trip_id)
    if not source_trip:
        raise HTTPException(status_code=404, detail="Trip not found.")

    new_days = []
    for d in source_trip.days:
        d_dict = d.to_dict() if isinstance(d, TripDay) else d
        new_day_id = str(uuid.uuid4())
        new_acts = []
        for a in d_dict.get("activities", []):
            a_dict = a.to_dict() if isinstance(a, TripActivity) else a
            a_copy = dict(a_dict)
            a_copy["id"] = str(uuid.uuid4())
            a_copy["trip_day_id"] = new_day_id
            new_acts.append(a_copy)
        d_copy = dict(d_dict)
        d_copy["id"] = new_day_id
        d_copy["activities"] = new_acts
        new_days.append(d_copy)

    new_trip = Trip(
        id=str(uuid.uuid4()),
        user_id=str(current_user.id),
        destination_id=source_trip.destination_id,
        title=f"Copy of {source_trip.title}",
        start_date=source_trip.start_date,
        end_date=source_trip.end_date,
        traveler_count=source_trip.traveler_count,
        travel_style=source_trip.travel_style,
        total_budget=source_trip.total_budget,
        total_estimated_cost=source_trip.total_estimated_cost,
        status="planning",
        days=new_days
    )
    trip_repo.create(new_trip)

    return {"success": True, "new_trip_id": str(new_trip.id), "message": "Itinerary duplicated successfully in Firestore."}
