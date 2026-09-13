import secrets
import uuid
from datetime import datetime, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from app.core.security import get_current_user, verify_ownership, require_partner_or_admin
from app.models.firestore_models import User, Booking
from app.repositories.booking_repo import booking_repo
from app.schemas.booking import RequestToBookRequest, BookingOut
from app.services.providers.factory import get_booking_provider

router = APIRouter()

@router.post("/", response_model=BookingOut)
@router.post("/request", response_model=BookingOut)
@router.post("/request-to-book", response_model=BookingOut)
def request_to_book(
    request: RequestToBookRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Request-to-Book Lifecycle with Firestore persistence (Section 13).
    Validates availability via provider adapter and registers booking in Firestore.
    """
    provider = get_booking_provider(request.item_type)
    avail = provider.check_availability(
        item_id=request.item_id,
        check_in_date=request.check_in_date,
        check_out_date=request.check_out_date,
        guest_count=request.guest_count
    )
    if not avail.get("available", True):
        raise HTTPException(status_code=400, detail="Requested item is not available for selected dates.")

    ref_code = f"BKG-{request.item_type[:3].upper()}-{secrets.token_hex(4).upper()}"
    
    # Provider reservation dispatch
    res = provider.create_reservation(
        item_id=request.item_id,
        booking_reference=ref_code,
        guest_name=current_user.full_name,
        guest_contact=current_user.phone or current_user.email or "N/A",
        check_in_date=request.check_in_date,
        check_out_date=request.check_out_date,
        guest_count=request.guest_count,
        total_amount=request.total_amount,
        special_requests=request.special_requests
    )

    booking = Booking(
        id=str(uuid.uuid4()),
        user_id=str(current_user.id),
        trip_id=request.trip_id,
        item_type=request.item_type,
        item_id=request.item_id,
        item_title=request.item_title,
        booking_reference=ref_code,
        check_in_date=request.check_in_date,
        check_out_date=request.check_out_date,
        guest_count=request.guest_count,
        total_amount=request.total_amount,
        currency=request.currency,
        status="Requested",
        voucher_qr_data=f"VOUCHER_{ref_code}_{str(current_user.id)[:6]}",
        cancellation_policy="Free cancellation up to 24h before scheduled service",
        special_requests=request.special_requests,
        vendor_name=provider.provider_name,
        vendor_booking_ref=res.get("vendor_booking_ref"),
        is_simulated=True,
        created_at=datetime.now(timezone.utc).isoformat()
    )
    booking_repo.create(booking)

    return {
        "id": str(booking.id),
        "user_id": str(booking.user_id),
        "trip_id": booking.trip_id,
        "item_type": booking.item_type,
        "item_id": booking.item_id,
        "item_title": booking.item_title,
        "booking_reference": booking.booking_reference,
        "check_in_date": booking.check_in_date,
        "check_out_date": booking.check_out_date,
        "guest_count": booking.guest_count,
        "total_amount": booking.total_amount,
        "currency": booking.currency,
        "status": booking.status,
        "payment_status": getattr(booking, "payment_status", "pending"),
        "voucher_qr_data": booking.voucher_qr_data,
        "cancellation_policy": booking.cancellation_policy,
        "created_at": booking.created_at
    }

@router.get("/", response_model=List[BookingOut])
def get_user_bookings(
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    current_user: User = Depends(get_current_user)
):
    """User Booking History with Pagination from Firestore (Section 13)."""
    bookings = booking_repo.get_user_bookings(str(current_user.id), limit=100)
    offset = (page - 1) * limit
    paged = bookings[offset:offset + limit]

    return [
        {
            "id": str(b.id),
            "user_id": str(b.user_id),
            "trip_id": b.trip_id,
            "item_type": b.item_type,
            "item_id": b.item_id,
            "item_title": b.item_title,
            "booking_reference": b.booking_reference,
            "check_in_date": b.check_in_date,
            "check_out_date": b.check_out_date,
            "guest_count": b.guest_count,
            "total_amount": b.total_amount,
            "currency": b.currency,
            "status": b.status,
            "payment_status": getattr(b, "payment_status", "pending"),
            "voucher_qr_data": b.voucher_qr_data,
            "cancellation_policy": b.cancellation_policy,
            "created_at": b.created_at
        } for b in paged
    ]

@router.get("/{booking_id}", response_model=BookingOut)
def get_booking_detail(
    booking_id: str,
    current_user: User = Depends(get_current_user)
):
    """Booking Details & Voucher with Ownership Verification from Firestore (Section 13)."""
    booking = booking_repo.get_by_id(booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")

    verify_ownership(current_user, booking.user_id, "booking")

    return {
        "id": str(booking.id),
        "user_id": str(booking.user_id),
        "trip_id": booking.trip_id,
        "item_type": booking.item_type,
        "item_id": booking.item_id,
        "item_title": booking.item_title,
        "booking_reference": booking.booking_reference,
        "check_in_date": booking.check_in_date,
        "check_out_date": booking.check_out_date,
        "guest_count": booking.guest_count,
        "total_amount": booking.total_amount,
        "currency": booking.currency,
        "status": booking.status,
        "payment_status": getattr(booking, "payment_status", "pending"),
        "voucher_qr_data": booking.voucher_qr_data,
        "cancellation_policy": booking.cancellation_policy,
        "created_at": booking.created_at
    }

@router.post("/{booking_id}/confirm")
def confirm_booking(
    booking_id: str,
    current_user: User = Depends(require_partner_or_admin)
):
    """Vendor / Partner Confirmation Hook in Firestore (Requires partner or admin role)."""
    booking = booking_repo.get_by_id(booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")

    booking_repo.update_status(booking_id, "Confirmed")
    return {"success": True, "status": "Confirmed", "message": "Booking confirmed by partner in Firestore."}
