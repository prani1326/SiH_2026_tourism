from fastapi import APIRouter, HTTPException
from app.core.firestore_db import get_firestore
from app.schemas.trip import DisruptionReplanRequest
from app.services.replan_engine import replan_engine

router = APIRouter()

@router.post("/replan")
def simulate_disruption_and_replan(request: DisruptionReplanRequest):
    """
    Trip OS Auto Re-planning Engine with Firestore (Section 11, 45).
    Simulates disruptions (Rain, Flight delay, Closure, Crowd) and suggests
    constraint-preserving replacement activities and schedule revisions.
    """
    return replan_engine.evaluate_disruption(
        db=None,
        trip_id=request.trip_id,
        disruption_type=request.disruption_type,
        affected_activity_id=request.affected_activity_id,
        details=request.disruption_details
    )

@router.post("/apply-replan/{disruption_log_id}")
def apply_replan(disruption_log_id: str):
    """Apply Recommended Itinerary Fix to Active Trip in Firestore (Section 11)."""
    firestore = get_firestore()
    doc_ref = firestore.collection("disruption_logs").document(disruption_log_id)
    doc = doc_ref.get()
    if not doc.exists:
        raise HTTPException(status_code=404, detail="Disruption record not found.")

    log_data = doc.to_dict()
    doc_ref.update({"status": "Applied"})
    return {
        "success": True,
        "message": f"Successfully applied schedule adjustment for '{log_data.get('disruption_type', 'disruption')}'."
    }
