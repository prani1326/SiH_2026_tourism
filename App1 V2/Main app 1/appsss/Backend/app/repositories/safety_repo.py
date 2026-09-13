from typing import Any, Dict, List, Optional
from app.models.firestore_models import SOSAlert, LostPhoneSession, SeparatedGroup, GroupMember
from app.repositories.base_repo import BaseFirestoreRepository

class SafetyRepository(BaseFirestoreRepository[SOSAlert]):
    collection_name = "sos_alerts"
    model_class = SOSAlert

    def get_user_active_sos(self, user_id: str) -> Optional[SOSAlert]:
        snaps = self.collection.where("user_id", "==", str(user_id)).where("status", "==", "Active").limit(1).get()
        if not snaps:
            return None
        return SOSAlert.from_dict(snaps[0].to_dict(), doc_id=snaps[0].id)

    def resolve_sos(self, alert_id: str, notes: str) -> Optional[SOSAlert]:
        return self.update(alert_id, {"status": "Resolved", "resolution_notes": notes})

    # Lost Phone
    def get_lost_phone_session(self, recovery_token: str) -> Optional[LostPhoneSession]:
        snaps = self.db.collection("lost_phone_sessions").where("recovery_token", "==", recovery_token).limit(1).get()
        if not snaps:
            return None
        return LostPhoneSession.from_dict(snaps[0].to_dict(), doc_id=snaps[0].id)

    def save_lost_phone_session(self, session: LostPhoneSession):
        self.db.collection("lost_phone_sessions").document(str(session.id)).set(session.to_dict())

    # Separated Groups
    def get_group(self, group_id: str) -> Optional[SeparatedGroup]:
        snap = self.db.collection("separated_groups").document(str(group_id)).get()
        if not snap.exists:
            return None
        return SeparatedGroup.from_dict(snap.to_dict(), doc_id=snap.id)

    def save_group(self, group: SeparatedGroup):
        self.db.collection("separated_groups").document(str(group.id)).set(group.to_dict())

    def update_member_checkin(
        self,
        group_id: str,
        user_id: str,
        lat: Optional[float] = None,
        lon: Optional[float] = None,
        status: str = "Safe"
    ) -> bool:
        group = self.get_group(group_id)
        if not group:
            return False
        from datetime import datetime, timezone
        now_str = datetime.now(timezone.utc).isoformat()
        members = []
        found = False
        for m in group.members:
            m_dict = m.to_dict() if isinstance(m, GroupMember) else m
            if str(m_dict.get("user_id")) == str(user_id):
                m_dict["status"] = status
                m_dict["last_checkin_at"] = now_str
                if lat is not None:
                    m_dict["last_latitude"] = lat
                if lon is not None:
                    m_dict["last_longitude"] = lon
                found = True
            members.append(m_dict)
        if found:
            self.db.collection("separated_groups").document(str(group_id)).update({"members": members})
            return True
        return False

safety_repo = SafetyRepository()
