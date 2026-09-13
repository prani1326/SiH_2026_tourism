from typing import Dict, List, Any, Optional
import uuid
from datetime import datetime

class ScamIntelligenceService:
    """
    Manages area scam advisories, typical fair price benchmarks,
    and crowd-sourced verified incident reports for tourists.
    """

    DESTINATION_SCAMS: Dict[str, List[Dict[str, Any]]] = {
        "Jaipur": [
            {
                "id": "scam_jp_1",
                "title": "Unmetered / Fake Taxi & Auto Fare",
                "category": "Transport",
                "severity": "High",
                "description": "Drivers claiming meters are broken or quoting 3x fares outside Railway Station / Airport.",
                "verified_remedy": "Use prepaid taxi counters inside arrival lounge or app-based rides (Ola/Uber).",
                "fair_price_benchmark": "Station to City Center: ₹150 – ₹220",
                "reported_hotspot": "Jaipur Junction Exit 1 & Airport Gate 2",
                "reports_count": 28
            },
            {
                "id": "scam_jp_2",
                "title": "Gemstone / Jewelry Export Commission Scheme",
                "category": "Shopping",
                "severity": "Critical",
                "description": "Strangers befriending tourists asking them to carry duty-free gems abroad for profit.",
                "verified_remedy": "Decline any requests to transport merchandise. Purchase certified jewelry only from Govt-approved emporiums.",
                "fair_price_benchmark": "Govt Rajasthan Emporium (Rajasthali) certified rates",
                "reported_hotspot": "Johari Bazaar & MI Road side alleys",
                "reports_count": 19
            },
            {
                "id": "scam_jp_3",
                "title": "Unauthorized Freelance Tour Guides",
                "category": "Guides",
                "severity": "Medium",
                "description": "Touts offering unofficial guide services at monument parking lots without ASI / Ministry ID cards.",
                "verified_remedy": "Hire only certified guides wearing official Ministry of Tourism badges or use the app's AI Heritage Lens.",
                "fair_price_benchmark": "Official ASI Guide: ₹300 – ₹500 for 2 hours",
                "reported_hotspot": "Amer Fort parking area & Hawa Mahal front",
                "reports_count": 14
            }
        ],
        "Agra": [
            {
                "id": "scam_ag_1",
                "title": "Overpriced 'Skip-the-Line' VIP Entry Tickets",
                "category": "Ticketing",
                "severity": "High",
                "description": "Touts selling standard entry tickets at 2x price claiming they are VIP express passes.",
                "verified_remedy": "Book official tickets online via ASI portal or App True Trip Booking.",
                "fair_price_benchmark": "Official Indian Tourist: ₹50, Foreign: ₹1100",
                "reported_hotspot": "Taj Mahal West Gate Parking",
                "reports_count": 34
            },
            {
                "id": "scam_ag_2",
                "title": "Synthetic Marble Inlay Sold as Pure Alabaster",
                "category": "Souvenirs",
                "severity": "Medium",
                "description": "Selling resin/chalk souvenirs claimed as handcrafted marble with genuine precious stones.",
                "verified_remedy": "Scratch test or buy from UP Tourism handicraft centres.",
                "fair_price_benchmark": "Small Coaster Set: ₹250 – ₹450",
                "reported_hotspot": "Fatehabad Road souvenir strips",
                "reports_count": 22
            }
        ]
    }

    def __init__(self):
        self._user_reports: List[Dict[str, Any]] = []

    def get_nearby_scam_alerts(self, destination: str = "Jaipur") -> List[Dict[str, Any]]:
        return self.DESTINATION_SCAMS.get(destination, self.DESTINATION_SCAMS["Jaipur"])

    def report_scam_incident(
        self,
        user_id: str,
        destination: str,
        scam_type: str,
        location: str,
        description: str,
        estimated_loss_inr: Optional[float] = None
    ) -> Dict[str, Any]:
        report = {
            "report_id": f"scam_{uuid.uuid4().hex[:10]}",
            "user_id": user_id,
            "destination": destination,
            "scam_type": scam_type,
            "location": location,
            "description": description,
            "estimated_loss_inr": estimated_loss_inr or 0.0,
            "status": "SUBMITTED_FOR_VERIFICATION",
            "created_at": datetime.utcnow().isoformat()
        }
        self._user_reports.append(report)

        return {
            "success": True,
            "message": "Scam incident report submitted! Our Tourist Intelligence Team will verify and update the local scam shield.",
            "report": report
        }

scam_intelligence_service = ScamIntelligenceService()
