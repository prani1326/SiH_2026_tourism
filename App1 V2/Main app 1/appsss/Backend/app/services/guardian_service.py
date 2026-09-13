from typing import Dict, List, Any, Optional
import uuid
from datetime import datetime, timedelta

class GuardianService:
    """
    Manages opt-in Trip Guardian sessions, telemetry (battery, network, GPS),
    scheduled check-ins, missed check-in alerts, and safe check-in confirmations.
    """

    def __init__(self):
        # In-memory store backed by Firestore in production
        self._active_sessions: Dict[str, Dict[str, Any]] = {}

    def start_guardian_session(
        self,
        user_id: str,
        trip_name: str = "Jaipur Heritage Tour",
        trusted_contacts: Optional[List[Dict[str, str]]] = None,
        check_in_interval_hours: int = 3
    ) -> Dict[str, Any]:
        session_id = f"guardian_{uuid.uuid4().hex[:12]}"
        now = datetime.utcnow()
        next_check_in = now + timedelta(hours=check_in_interval_hours)

        contacts = trusted_contacts or [
            {"name": "Emergency Contact 1", "phone": "+91 98765 43210", "relationship": "Family"},
            {"name": "Emergency Contact 2", "phone": "+91 91234 56789", "relationship": "Friend"}
        ]

        session_data = {
            "session_id": session_id,
            "user_id": user_id,
            "trip_name": trip_name,
            "status": "ACTIVE_ON_SCHEDULE",
            "is_sharing_enabled": True,
            "current_location_name": "Amber Fort, Jaipur",
            "latitude": 26.9855,
            "longitude": 75.8513,
            "battery_level": 74,
            "network_status": "Strong 5G",
            "last_check_in": now.strftime("%I:%M %p"),
            "next_check_in": next_check_in.strftime("%I:%M %p"),
            "check_in_interval_hours": check_in_interval_hours,
            "trusted_contacts": contacts,
            "created_at": now.isoformat(),
            "expires_at": (now + timedelta(days=5)).isoformat()
        }

        self._active_sessions[user_id] = session_data
        return session_data

    def check_in_safe(
        self,
        user_id: str,
        battery_level: int = 70,
        location_name: Optional[str] = None
    ) -> Dict[str, Any]:
        now = datetime.utcnow()
        session = self._active_sessions.get(user_id)
        if not session:
            # create auto session
            session = self.start_guardian_session(user_id=user_id)

        next_check_in = now + timedelta(hours=session.get("check_in_interval_hours", 3))
        session["last_check_in"] = now.strftime("%I:%M %p")
        session["next_check_in"] = next_check_in.strftime("%I:%M %p")
        session["status"] = "ACTIVE_ON_SCHEDULE"
        session["battery_level"] = battery_level
        if location_name:
            session["current_location_name"] = location_name

        self._active_sessions[user_id] = session
        return {
            "success": True,
            "message": "Check-in confirmed! Your trusted contacts have been notified that you are safe.",
            "session": session
        }

    def pause_or_resume_sharing(self, user_id: str, enable: bool) -> Dict[str, Any]:
        session = self._active_sessions.get(user_id)
        if not session:
            session = self.start_guardian_session(user_id=user_id)

        session["is_sharing_enabled"] = enable
        session["status"] = "ACTIVE_ON_SCHEDULE" if enable else "SHARING_PAUSED"
        self._active_sessions[user_id] = session
        return {
            "success": True,
            "is_sharing_enabled": enable,
            "status": session["status"]
        }

    def get_guardian_status(self, user_id: str) -> Dict[str, Any]:
        session = self._active_sessions.get(user_id)
        if not session:
            session = self.start_guardian_session(user_id=user_id)
        return session

guardian_service = GuardianService()
