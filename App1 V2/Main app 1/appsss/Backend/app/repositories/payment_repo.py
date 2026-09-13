from typing import Any, Dict, List, Optional
from datetime import datetime, timezone
from app.models.firestore_models import PaymentTransaction, Refund, IdempotencyRecord, AuditLog
from app.repositories.base_repo import BaseFirestoreRepository

class PaymentRepository(BaseFirestoreRepository[PaymentTransaction]):
    collection_name = "payments"
    model_class = PaymentTransaction

    def get_by_ref(self, ref: str) -> Optional[PaymentTransaction]:
        return self.find_one("transaction_ref", "==", ref)

    def get_by_razorpay_order_id(self, order_id: str) -> Optional[PaymentTransaction]:
        return self.find_one("razorpay_order_id", "==", order_id)

    # Idempotency
    def get_idempotency(self, key: str) -> Optional[IdempotencyRecord]:
        snap = self.db.collection("idempotency_records").document(str(key)).get()
        if not snap.exists:
            return None
        return IdempotencyRecord.from_dict(snap.to_dict(), doc_id=snap.id)

    def save_idempotency(self, record: IdempotencyRecord):
        self.db.collection("idempotency_records").document(str(record.key)).set(record.to_dict())

    # Audit log
    def log_audit(self, audit: AuditLog):
        self.db.collection("audit_logs").document(str(audit.id)).set(audit.to_dict())

    # Refund
    def get_refund_by_ref(self, refund_ref: str) -> Optional[Refund]:
        snaps = self.db.collection("refunds").where("refund_ref", "==", refund_ref).limit(1).get()
        if not snaps:
            return None
        s = snaps[0]
        return Refund.from_dict(s.to_dict(), doc_id=s.id)

    def save_refund(self, refund: Refund):
        self.db.collection("refunds").document(str(refund.id)).set(refund.to_dict())

payment_repo = PaymentRepository()
