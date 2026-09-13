from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field

class RequestToBookRequest(BaseModel):
    item_type: str = Field(..., examples=["Hotel"])  # Hotel, Flight, Activity, Experience, Restaurant, Transport, DayPass
    item_id: str
    item_title: str
    check_in_date: str
    check_out_date: Optional[str] = None
    guest_count: int = 1
    total_amount: float
    currency: str = "INR"
    trip_id: Optional[str] = None
    special_requests: Optional[str] = None

class BookingOut(BaseModel):
    id: str
    user_id: str
    trip_id: Optional[str] = None
    item_type: str
    item_id: str
    item_title: str
    booking_reference: str
    check_in_date: str
    check_out_date: Optional[str] = None
    guest_count: int
    total_amount: float
    currency: str
    status: str
    payment_status: Optional[str] = "pending"
    voucher_qr_data: Optional[str] = None
    cancellation_policy: str
    created_at: Any

class PaymentProcessRequest(BaseModel):
    booking_id: str
    payment_method: str = Field("UPI", examples=["UPI, Credit_Debit_Card, Wallet, NetBanking"])
    currency: str = "INR"
    upi_id: Optional[str] = None
    card_token: Optional[str] = None
    idempotency_key: Optional[str] = None

class PaymentReceiptOut(BaseModel):
    transaction_ref: str
    booking_id: str
    booking_reference: str
    item_title: str
    amount: float
    currency: str
    payment_method: str
    status: str
    invoice_number: str
    timestamp: Any
    receipt_url: str

class RazorpayOrderCreateRequest(BaseModel):
    booking_id: str
    idempotency_key: Optional[str] = None

class RazorpayOrderOut(BaseModel):
    order_id: str
    amount: float
    currency: str
    key_id: str
    transaction_ref: str
    status: str
    is_simulated: bool
    booking_id: str

class RazorpayVerifyRequest(BaseModel):
    booking_id: str
    razorpay_order_id: str
    razorpay_payment_id: str
    razorpay_signature: str

class CancellationRequest(BaseModel):
    booking_id: str
    cancellation_reason: str

class RefundStatusOut(BaseModel):
    refund_ref: str
    booking_id: str
    amount: float
    currency: str
    status: str  # Initiated -> Processing -> Completed
    milestones: List[Dict[str, Any]]
