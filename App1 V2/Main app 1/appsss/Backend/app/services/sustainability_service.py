from typing import Dict, List, Any, Optional
from datetime import datetime

class SustainabilityService:
    """
    Computes itinerary sustainability scores (0-100), breakdown across 5 pillars,
    and actionable recommendations to lower carbon footprint and empower local economies.
    """

    def calculate_trip_sustainability(
        self,
        trip_id: str = "trip_demo",
        transport_mode: str = "Cab",
        has_metro_or_walking: bool = True,
        homestay_or_eco_stay: bool = True,
        local_dining_count: int = 4
    ) -> Dict[str, Any]:
        # 5 pillars (each out of 20)
        # 1. Transport Mode (Public transit/electric vs diesel cabs)
        transit_score = 14 if transport_mode.lower() in ["cab", "taxi"] else 19
        if has_metro_or_walking:
            transit_score = min(20, transit_score + 4)

        # 2. Local Economy (Dining at local artisan places vs MNC chains)
        local_econ_score = min(20, 10 + (local_dining_count * 2))

        # 3. Crowd Impact (Visiting during off-peak windows or alternative heritage sites)
        crowd_impact_score = 16

        # 4. Walking & Micro-mobility
        walking_score = 17 if has_metro_or_walking else 12

        # 5. Eco-friendly Stay (Certified green hotels or verified homestays)
        stay_score = 18 if homestay_or_eco_stay else 13

        total_score = transit_score + local_econ_score + crowd_impact_score + walking_score + stay_score

        recommendations = []
        if transit_score < 18:
            recommendations.append("Swap 1 private taxi journey with the Jaipur Heritage Metro or electric e-rickshaw (+6 pts).")
        if local_econ_score < 18:
            recommendations.append("Support local culinary artisans by choosing recommended street food & thali kitchens (+4 pts).")
        if stay_score < 18:
            recommendations.append("Opt for solar-powered boutique stays or certified green heritage homestays (+5 pts).")
        if not recommendations:
            recommendations.append("Your trip itinerary has achieved Gold-tier eco sustainability!")

        return {
            "trip_id": trip_id,
            "overall_score": total_score,
            "tier": "Eco-Champion (Gold)" if total_score >= 80 else ("Responsible Traveler (Silver)" if total_score >= 65 else "Standard Impact (Bronze)"),
            "breakdown": {
                "transport": {"score": transit_score, "max": 20, "label": "Low-Carbon Transport"},
                "local_economy": {"score": local_econ_score, "max": 20, "label": "Local Economy Support"},
                "crowd_impact": {"score": crowd_impact_score, "max": 20, "label": "Off-Peak Dispersion"},
                "walking": {"score": walking_score, "max": 20, "label": "Pedestrian & Micro-Mobility"},
                "stay": {"score": stay_score, "max": 20, "label": "Eco-Certified Stays"}
            },
            "carbon_offset_kg": round((total_score / 100.0) * 42.5, 1),
            "recommendations": recommendations,
            "calculated_at": datetime.utcnow().isoformat()
        }

sustainability_service = SustainabilityService()
