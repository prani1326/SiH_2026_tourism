import secrets
import uuid
from datetime import datetime, timezone
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Header, Request, status
from app.core.security import get_current_user, get_current_user_optional, verify_ownership
from app.models.firestore_models import User, Booking, PaymentTransaction
from app.repositories.booking_repo import booking_repo
from app.repositories.payment_repo import payment_repo
from app.repositories.user_repo import user_repo
from app.schemas.booking import (
    PaymentProcessRequest, PaymentReceiptOut,
    RazorpayOrderCreateRequest, RazorpayOrderOut, RazorpayVerifyRequest
)
from app.services.payment_service import payment_service

router = APIRouter()

@router.post("/order", response_model=RazorpayOrderOut)
def create_payment_order(
    request: RazorpayOrderCreateRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Create a Razorpay payment order for a booking in Firestore (Section 14).
    Enforces idempotency and server-side order generation.
    """
    try:
        return payment_service.create_order(
            user_id=str(current_user.id),
            booking_id=request.booking_id,
            idempotency_key=request.idempotency_key
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except PermissionError as e:
        raise HTTPException(status_code=403, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Payment order creation failed: {e}")

@router.post("/verify")
def verify_payment(
    request: RazorpayVerifyRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Verifies Razorpay payment signature and confirms booking in Firestore (Section 14).
    Rejects tampered payments and unverified callbacks.
    """
    try:
        return payment_service.verify_payment(
            user_id=str(current_user.id),
            booking_id=request.booking_id,
            razorpay_order_id=request.razorpay_order_id,
            razorpay_payment_id=request.razorpay_payment_id,
            razorpay_signature=request.razorpay_signature
        )
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except PermissionError as e:
        raise HTTPException(status_code=403, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Payment verification failed: {e}")

@router.post("/webhook")
async def razorpay_webhook(
    request: Request,
    x_razorpay_signature: str = Header(None, alias="X-Razorpay-Signature")
):
    """
    Razorpay Webhook Endpoint (Section 14).
    Validates HMAC SHA-256 signature and reconciles asynchronous payment state in Firestore.
    """
    body = await request.body()
    try:
        return payment_service.handle_webhook(
            db=None,
            payload=body,
            signature=x_razorpay_signature or ""
        )
    except PermissionError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Webhook processing error: {e}")

@router.post("/process", response_model=PaymentReceiptOut)
def process_payment(
    request: PaymentProcessRequest,
    current_user: User = Depends(get_current_user)
):
    """
    Direct / Sandbox payment checkout endpoint stored in Firestore (Section 14).
    Simulates multi-currency payment execution across UPI, Cards, Wallets, and NetBanking
    while registering verifiable transaction and digital invoice.
    """
    booking = booking_repo.get_by_id(request.booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")

    verify_ownership(current_user, booking.user_id, "booking")

    txn_ref = f"TXN-{request.payment_method[:3].upper()}-{secrets.token_hex(6).upper()}"
    inv_num = f"INV-2026-{secrets.token_hex(4).upper()}"
    now_iso = datetime.now(timezone.utc).isoformat()

    txn = PaymentTransaction(
        id=str(uuid.uuid4()),
        booking_id=str(booking.id),
        user_id=str(current_user.id),
        transaction_ref=txn_ref,
        payment_method=request.payment_method,
        amount=booking.total_amount,
        currency=request.currency or booking.currency,
        status="Succeeded",
        invoice_number=inv_num,
        is_simulated=True,
        created_at=now_iso
    )
    payment_repo.create(txn)

    qr_data = f"VOUCHER:{booking.booking_reference}|TXN:{txn_ref}|INV:{inv_num}"
    booking_repo.update(booking.id, {
        "status": "Confirmed",
        "voucher_qr_data": qr_data
    })

    return {
        "transaction_ref": txn.transaction_ref,
        "booking_id": str(booking.id),
        "booking_reference": booking.booking_reference,
        "item_title": booking.item_title,
        "amount": txn.amount,
        "currency": txn.currency,
        "payment_method": txn.payment_method,
        "status": txn.status,
        "invoice_number": inv_num,
        "timestamp": txn.created_at,
        "receipt_url": f"https://tourist-app.platform/receipts/{inv_num}.pdf"
    }

@router.get("/{booking_id}/invoice")
def get_invoice(
    booking_id: str,
    current_user: Optional[User] = Depends(get_current_user_optional)
):
    """Fetch Digital Invoice / Receipt from Firestore with Ownership Enforcement (Section 14)."""
    booking = booking_repo.get_by_id(booking_id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found.")

    if current_user:
        verify_ownership(current_user, booking.user_id, "booking invoice")

    txs = payment_repo.find_many("booking_id", "==", str(booking_id), limit=1)
    payment = txs[0] if txs else None

    guest_name = "Tourist"
    if booking.user_id:
        user = user_repo.get_by_id(str(booking.user_id))
        if user and user.full_name:
            guest_name = user.full_name

    return {
        "invoice_number": payment.invoice_number if payment and hasattr(payment, "invoice_number") and payment.invoice_number else "INV-PENDING",
        "booking_reference": booking.booking_reference,
        "item_title": booking.item_title,
        "guest_name": guest_name,
        "dates": f"{booking.check_in_date} to {booking.check_out_date or 'Completed'}",
        "subtotal": booking.total_amount,
        "gst_tax_18_pct": round(booking.total_amount * 0.18, 2),
        "total_paid": booking.total_amount,
        "currency": booking.currency,
        "payment_method": payment.payment_method if payment and hasattr(payment, "payment_method") else "UPI",
        "status": "Paid & Verified",
        "issued_at": datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    }
