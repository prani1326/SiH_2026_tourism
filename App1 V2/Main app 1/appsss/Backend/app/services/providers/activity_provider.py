import secrets
import logging
from typing import Dict, Any, Optional
from app.services.providers.base import BookingProviderBase

logger = logging.getLogger("tourist_app.providers.activity")

class VerifiedActivityProviderAdapter(BookingProviderBase):
    """
    Adapter for Verified Experiences, Guided Heritage Walks, and Monument Day Passes.
    """
    @property
    def provider_name(self) -> str:
        return "VerifiedActivitiesIndiaAdapter"

    def check_availability(
        self,
        item_id: str,
        check_in_date: str,
        check_out_date: Optional[str] = None,
        guest_count: int = 1
    ) -> Dict[str, Any]:
        return {
            "available": True,
            "item_id": item_id,
            "slots_available": 15,
            "currency": "INR",
            "provider": self.provider_name
        }

    def create_reservation(
        self,
        item_id: str,
        booking_reference: str,
        guest_name: str,
        guest_contact: str,
        check_in_date: str,
        check_out_date: Optional[str] = None,
        guest_count: int = 1,
        total_amount: float = 0.0,
        special_requests: Optional[str] = None
    ) -> Dict[str, Any]:
        vendor_ref = f"ACT-PASS-{secrets.token_hex(4).upper()}"
        logger.info(f"[{self.provider_name}] Issued e-pass: {vendor_ref} for {guest_name}")
        return {
            "success": True,
            "vendor_booking_ref": vendor_ref,
            "provider_name": self.provider_name,
            "status": "ISSUED",
            "access_pass_code": secrets.token_hex(4).upper(),
            "entry_gate": "Gate 1 - FastTrack Tourist Concourse"
        }

    def cancel_reservation(
        self,
        vendor_booking_ref: str,
        cancellation_reason: str
    ) -> Dict[str, Any]:
        logger.info(f"[{self.provider_name}] Cancelled e-pass: {vendor_booking_ref}")
        return {
            "success": True,
            "vendor_booking_ref": vendor_booking_ref,
            "status": "CANCELLED",
            "refund_eligible": True
        }
