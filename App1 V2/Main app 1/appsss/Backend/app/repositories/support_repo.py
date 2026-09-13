from typing import Any, Dict, List, Optional
from app.models.firestore_models import SupportTicket, TicketMessage, Notification, Review
from app.repositories.base_repo import BaseFirestoreRepository

class SupportRepository(BaseFirestoreRepository[SupportTicket]):
    collection_name = "support_tickets"
    model_class = SupportTicket

    def get_user_tickets(self, user_id: str) -> List[SupportTicket]:
        return self.find_many("user_id", "==", str(user_id))

    def add_message(self, ticket_id: str, message_data: Dict[str, Any]) -> Optional[TicketMessage]:
        ticket = self.get_by_id(ticket_id)
        if not ticket:
            return None
        msg = TicketMessage(ticket_id=ticket_id, **message_data)
        msgs = [m.to_dict() if isinstance(m, TicketMessage) else m for m in ticket.messages]
        msgs.append(msg.to_dict())
        self.update(ticket_id, {"messages": msgs})
        return msg

    # Notifications
    def get_user_notifications(self, user_id: str, unread_only: bool = False, limit: int = 50) -> List[Notification]:
        query = self.db.collection("notifications").where("user_id", "==", str(user_id))
        if unread_only:
            query = query.where("is_read", "==", False)
        snaps = query.limit(limit).get()
        return [Notification.from_dict(s.to_dict(), doc_id=s.id) for s in snaps]

    def add_notification(self, notif: Notification) -> Notification:
        self.db.collection("notifications").document(str(notif.id)).set(notif.to_dict())
        return notif

    def mark_notification_read(self, notif_id: str) -> bool:
        snap = self.db.collection("notifications").document(str(notif_id)).get()
        if not snap.exists:
            return False
        self.db.collection("notifications").document(str(notif_id)).update({"is_read": True})
        return True

    # Reviews
    def list_reviews(self, target_type: str, target_id: str, limit: int = 50) -> List[Review]:
        snaps = self.db.collection("reviews")\
            .where("target_type", "==", target_type)\
            .where("target_id", "==", str(target_id))\
            .limit(limit)\
            .get()
        return [Review.from_dict(s.to_dict(), doc_id=s.id) for s in snaps]

    def add_review(self, review: Review) -> Review:
        self.db.collection("reviews").document(str(review.id)).set(review.to_dict())
        return review

support_repo = SupportRepository()
