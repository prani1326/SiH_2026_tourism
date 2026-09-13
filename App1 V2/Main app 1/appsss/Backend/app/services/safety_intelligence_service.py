from typing import Dict, List, Any, Optional
import math
from datetime import datetime

class SafetyIntelligenceService:
    """
    Computes dynamic safety scores (0-100), risk factors, day/night variance,
    and analyzes safe routing options for tourists.
    """

    DESTINATION_RISK_BASELINES: Dict[str, Dict[str, Any]] = {
        "Jaipur": {"base_safety": 92, "night_safety": 84, "scam_risk": 25, "crowd_density": 65, "emergency_services_rating": 90},
        "Goa": {"base_safety": 94, "night_safety": 90, "scam_risk": 20, "crowd_density": 70, "emergency_services_rating": 88},
        "Agra": {"base_safety": 88, "night_safety": 78, "scam_risk": 40, "crowd_density": 80, "emergency_services_rating": 85},
        "Delhi": {"base_safety": 85, "night_safety": 72, "scam_risk": 35, "crowd_density": 85, "emergency_services_rating": 95},
        "Mumbai": {"base_safety": 95, "night_safety": 92, "scam_risk": 18, "crowd_density": 90, "emergency_services_rating": 96},
        "Udaipur": {"base_safety": 96, "night_safety": 90, "scam_risk": 15, "crowd_density": 50, "emergency_services_rating": 86},
        "Manali": {"base_safety": 93, "night_safety": 85, "scam_risk": 12, "crowd_density": 55, "emergency_services_rating": 80},
        "Rishikesh": {"base_safety": 95, "night_safety": 88, "scam_risk": 10, "crowd_density": 45, "emergency_services_rating": 82},
        "Varanasi": {"base_safety": 89, "night_safety": 76, "scam_risk": 38, "crowd_density": 88, "emergency_services_rating": 84},
        "Kochi": {"base_safety": 96, "night_safety": 91, "scam_risk": 12, "crowd_density": 40, "emergency_services_rating": 92}
    }

    def get_current_safety_intelligence(
        self,
        destination: str = "Jaipur",
        latitude: Optional[float] = None,
        longitude: Optional[float] = None,
        current_hour: Optional[int] = None
    ) -> Dict[str, Any]:
        hour = current_hour if current_hour is not None else datetime.now().hour
        is_night = hour < 6 or hour >= 20

        baseline = self.DESTINATION_RISK_BASELINES.get(
            destination,
            {"base_safety": 90, "night_safety": 82, "scam_risk": 20, "crowd_density": 60, "emergency_services_rating": 88}
        )

        day_score = baseline["base_safety"]
        night_score = baseline["night_safety"]
        current_score = night_score if is_night else day_score

        # Risk level determination
        if current_score >= 86:
            risk_label = "Very Safe"
            risk_color = "#10B981"
        elif current_score >= 71:
            risk_label = "Safe"
            risk_color = "#3B82F6"
        elif current_score >= 51:
            risk_label = "Moderate"
            risk_color = "#F59E0B"
        elif current_score >= 31:
            risk_label = "High Risk"
            risk_color = "#EF4444"
        else:
            risk_label = "Critical"
            risk_color = "#991B1B"

        explanations = []
        if is_night:
            explanations.append("Night hours detected: Main tourist avenues are well lit; recommend using verified cabs.")
        if baseline["crowd_density"] > 75:
            explanations.append(f"High tourist footfall in {destination}; keep valuables in secure compartments.")
        if baseline["scam_risk"] > 30:
            explanations.append("Occasional unauthorized guide solicitations reported near main monument gates.")
        if not explanations:
            explanations.append(f"Current conditions in {destination} are highly favorable with verified tourist police patrols.")

        return {
            "destination": destination,
            "safety_score": current_score,
            "risk_label": risk_label,
            "risk_color": risk_color,
            "is_night": is_night,
            "day_safety": day_score,
            "night_safety": night_score,
            "crowd_risk": "High" if baseline["crowd_density"] > 75 else ("Medium" if baseline["crowd_density"] > 50 else "Low"),
            "weather_risk": "Low",
            "scam_risk": "High" if baseline["scam_risk"] > 35 else ("Medium" if baseline["scam_risk"] > 20 else "Low"),
            "network_status": "Strong 5G / 4G",
            "nearby_police_stations": 3,
            "nearby_hospitals": 4,
            "explanation": " • ".join(explanations),
            "updated_at": datetime.utcnow().isoformat()
        }

    def evaluate_safe_routes(
        self,
        origin: str = "Hotel / Station",
        destination: str = "Hawa Mahal, Jaipur"
    ) -> List[Dict[str, Any]]:
        return [
            {
                "id": "route_safest",
                "name": "Route B (Main Boulevard)",
                "category": "Recommended",
                "is_recommended": True,
                "duration_min": 27,
                "distance_km": 6.8,
                "safety_score": 93,
                "lighting_quality": "Well-lit & CCTV Monitored",
                "police_patrols": True,
                "cost_inr": 180,
                "highlights": "Passes through tourist police kiosk and high-visibility main transit corridors.",
                "explanation": "Route B is 7 mins longer than fastest route but has significantly better lighting and verified police booths."
            },
            {
                "id": "route_fastest",
                "name": "Route A (Direct Alleyways)",
                "category": "Fastest",
                "is_recommended": False,
                "duration_min": 20,
                "distance_km": 5.2,
                "safety_score": 68,
                "lighting_quality": "Moderate / Narrow Alleys",
                "police_patrols": False,
                "cost_inr": 150,
                "highlights": "Quickest route through bazaar side-streets, moderate congestion.",
                "explanation": "Fastest travel time, but narrower lanes with limited nighttime illumination."
            },
            {
                "id": "route_cheapest",
                "name": "Route C (Metro + Walk)",
                "category": "Cheapest",
                "is_recommended": False,
                "duration_min": 32,
                "distance_km": 7.4,
                "safety_score": 88,
                "lighting_quality": "High (Metro Stations)",
                "police_patrols": True,
                "cost_inr": 40,
                "highlights": "Eco-friendly Jaipur Metro to Chandpole + 8 min walk on heritage sidewalk.",
                "explanation": "Lowest cost and zero traffic delays with active security inside metro stations."
            },
            {
                "id": "route_balanced",
                "name": "Route D (Ring Road Transit)",
                "category": "Balanced",
                "is_recommended": False,
                "duration_min": 24,
                "distance_km": 6.0,
                "safety_score": 84,
                "lighting_quality": "Good",
                "police_patrols": True,
                "cost_inr": 160,
                "highlights": "Smooth traffic flow around outer city wall.",
                "explanation": "Optimal trade-off between travel speed and safety score."
            }
        ]

safety_intelligence_service = SafetyIntelligenceService()
