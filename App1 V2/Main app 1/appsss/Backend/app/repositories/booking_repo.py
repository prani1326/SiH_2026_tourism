from typing import Any, Dict, List, Optional
from app.models.firestore_models import Booking, PaymentTransaction, Refund
from app.repositories.base_repo import BaseFirestoreRepository

class BookingRepository(BaseFirestoreRepository[Booking]):
    collection_name = "bookings"
    model_class = Booking

    def get_user_bookings(self, user_id: str, status: Optional[str] = None, limit: int = 100) -> List[Booking]:
        if not user_id:
            return []
        all_bookings = self.find_many("user_id", "==", str(user_id), limit=limit)
        if status:
            return [b for b in all_bookings if b.status.lower() == status.lower()]
        return all_bookings

    def get_by_reference(self, reference: str) -> Optional[Booking]:
        if not reference:
            return None
        return self.find_one("booking_reference", "==", reference.strip())

    def update_status(self, booking_id: str, status: str) -> Optional[Booking]:
        return self.update(booking_id, {"status": status})

    def add_payment(self, booking_id: str, payment_data: Dict[str, Any]) -> Optional[PaymentTransaction]:
        booking = self.get_by_id(booking_id)
        if not booking:
            return None
        payment = PaymentTransaction(booking_id=booking_id, **payment_data)
        payments = [p.to_dict() if isinstance(p, PaymentTransaction) else p for p in booking.payments]
        payments.append(payment.to_dict())
        self.update(booking_id, {"payments": payments})
        return payment

    def add_refund(self, booking_id: str, refund_data: Dict[str, Any]) -> Optional[Refund]:
        booking = self.get_by_id(booking_id)
        if not booking:
            return None
        refund = Refund(booking_id=booking_id, **refund_data)
        refunds = [r.to_dict() if isinstance(r, Refund) else r for r in booking.refunds]
        refunds.append(refund.to_dict())
        self.update(booking_id, {"refunds": refunds})
        return refund

booking_repo = BookingRepository()
