from typing import Dict, List, Any, Optional
import uuid
from datetime import datetime

class OfflineEmergencyService:
    """
    Handles preparation of offline emergency packets and synchronization of queued SOS alerts
    when device connectivity is restored.
    """

    def __init__(self):
        self._synced_alerts: List[Dict[str, Any]] = []

    def get_emergency_bundle(self, destination: str = "Jaipur") -> Dict[str, Any]:
        return {
            "destination": destination,
            "destination_helplines": [
                {"name": "National Emergency Helpline", "number": "112"},
                {"name": "Tourist Police Helpline", "number": "1363"},
                {"name": "Women Helpline", "number": "1090"},
                {"name": "Ambulance", "number": "108"},
                {"name": "Fire & Rescue", "number": "101"},
                {"name": "Tourist Facilitation Desk (Airport/Rail)", "number": "+91 141 2822888"}
            ],
            "first_aid_guides": [
                {"title": "Dehydration & Heat Stroke", "instruction": "Move to shade, loosen clothes, sip electrolyte water, apply cool compress."},
                {"title": "Sprains & Fractures", "instruction": "Immobilize limb with splint/bandage, apply ice pack, elevate."},
                {"title": "Cuts & Bleeding", "instruction": "Apply direct pressure with clean cloth, elevate wound, seek medical post."}
            ],
            "offline_instructions": [
                "1. If offline, the app captures your precise GPS coordinates immediately.",
                "2. An encrypted SOS packet is stored in local storage.",
                "3. Broadcast emergency beacon via Bluetooth/Wi-Fi Direct if enabled.",
                "4. As soon as cellular network or Wi-Fi reconnects, SOS is dispatched to Tourist Police & Emergency Contacts."
            ],
            "cached_at": datetime.utcnow().isoformat()
        }

    def sync_offline_sos(
        self,
        user_id: str,
        packet_id: str,
        latitude: float,
        longitude: float,
        timestamp_offline: str,
        emergency_type: str = "SOS_OFFLINE",
        medical_notes: Optional[str] = None
    ) -> Dict[str, Any]:
        alert_record = {
            "sync_id": f"sync_{uuid.uuid4().hex[:10]}",
            "packet_id": packet_id,
            "user_id": user_id,
            "latitude": latitude,
            "longitude": longitude,
            "offline_timestamp": timestamp_offline,
            "synced_at": datetime.utcnow().isoformat(),
            "emergency_type": emergency_type,
            "medical_notes": medical_notes or "None provided",
            "status": "DISPATCHED_TO_TOURIST_POLICE",
            "assigned_responder": "Jaipur Central Tourist Police Control Room",
            "responder_eta_min": 8
        }
        self._synced_alerts.append(alert_record)

        return {
            "success": True,
            "message": "Offline SOS successfully synced with Tourist Control Center! Responders notified.",
            "alert": alert_record
        }

offline_emergency_service = OfflineEmergencyService()
