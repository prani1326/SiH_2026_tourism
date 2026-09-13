import json
from datetime import datetime, timezone
from fastapi import APIRouter, Depends, HTTPException
from app.core.security import get_current_user, verify_ownership
from app.models.firestore_models import User, Booking, Refund
from app.repositories.booking_repo import booking_repo
from app.repositories.payment_repo import payment_repo
from app.schemas.booking import CancellationRequest, RefundStatusOut
from app.services.payment_service import payment_service

router = APIRouter()

@router.post("/cancel", response_model=RefundStatusOut)
def cancel_booking_and_initiate_refund(
    request: CancellationRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Cancellation & Refund Flow with Firestore (Section 15).
    Milestone Progression: Cancelled -> Refund initiated -> Processing -> Completed
    """
    booking = booking_repo.get_by_id(request.booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")

    verify_ownership(current_user, booking.user_id, "booking")

    if booking.status in ["Cancelled", "Refund pending", "Refund initiated", "Refund completed"]:
        raise HTTPException(status_code=400, detail="Booking is already cancelled or refund is in progress.")

    # Status will be set to "Refund pending" by process_refund below

    refund = payment_service.process_refund(
        db=None,
        booking=booking,
        amount=booking.total_amount,
        reason=request.cancellation_reason
    )

    return {
        "refund_ref": refund.refund_ref,
        "booking_id": str(booking.id),
        "amount": refund.amount,
        "currency": refund.currency,
        "status": refund.status,
        "milestones": refund.milestones
    }

@router.get("/{booking_id}/status", response_model=RefundStatusOut)
def get_refund_status(
    booking_id: str,
    current_user: User = Depends(get_current_user)
):
    """Fetch Real-Time Refund Milestones with Ownership Enforcement from Firestore (Section 15)."""
    booking = booking_repo.get_by_id(booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")

    verify_ownership(current_user, booking.user_id, "booking refund")

    firestore = payment_repo.db
    snaps = firestore.collection("refunds").where("booking_id", "==", str(booking_id)).limit(1).get()
    if not snaps:
        raise HTTPException(status_code=404, detail="No refund records found for this booking.")

    data = snaps[0].to_dict()
    milestones = data.get("milestones", [])
    if isinstance(milestones, str):
        try:
            milestones = json.loads(milestones)
        except Exception:
            milestones = []

    return {
        "refund_ref": data.get("refund_ref", ""),
        "booking_id": str(data.get("booking_id", booking_id)),
        "amount": float(data.get("amount", 0.0)),
        "currency": data.get("currency", "INR"),
        "status": data.get("status", "Initiated"),
        "milestones": milestones
    }
