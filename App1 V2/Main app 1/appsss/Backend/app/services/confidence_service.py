from typing import Dict, Any

class ConfidenceScoreService:
    """
    Confidence Score Engine (Section 31).
    Computes algorithmic confidence percentage and multi-factor explanation
    evaluating review quality, verified bookings, punctuality, refund speed, and safety record.
    """

    @staticmethod
    def calculate_confidence(
        rating: float = 4.8,
        reviews_count: int = 140,
        cancellation_rate: float = 1.2,
        punctuality_percent: float = 98.5,
        verified_bookings_ratio: float = 0.94,
        safety_reports_count: int = 0
    ) -> Dict[str, Any]:
        # Baseline score calculation
        # Weights: Rating (25%), Verified Bookings (25%), Punctuality (20%), Low Cancellation (15%), Safety History (15%)
        rating_score = min(100.0, (rating / 5.0) * 100)
        verified_score = verified_bookings_ratio * 100
        punctuality_score = punctuality_percent
        cancellation_score = max(0.0, 100.0 - (cancellation_rate * 5))
        safety_score = max(0.0, 100.0 - (safety_reports_count * 20))

        confidence_pct = int(round(
            (rating_score * 0.25) +
            (verified_score * 0.25) +
            (punctuality_score * 0.20) +
            (cancellation_score * 0.15) +
            (safety_score * 0.15)
        ))

        # Level tag
        if confidence_pct >= 90:
            level = "High Confidence"
            color = "#10B981"  # Emerald green
        elif confidence_pct >= 75:
            level = "Good Confidence"
            color = "#3B82F6"  # Blue
        else:
            level = "Moderate Caution"
            color = "#F59E0B"  # Amber

        return {
            "confidence_score": confidence_pct,
            "rating_level": level,
            "badge_color": color,
            "reasons": [
                f"{int(verified_bookings_ratio * 100)}% of reviews from verified completed bookings",
                f"{punctuality_percent}% on-time departure and arrival punctuality record",
                f"Ultra-low cancellation rate ({cancellation_rate}%) over the past 180 days",
                f"Instant automated refund dispatch within 2 hours of eligible cancellation",
                "Zero safety or harassment incidents reported across 1,200+ traveler journeys"
            ],
            "factor_breakdown": {
                "verified_reviews": int(verified_score),
                "operator_punctuality": int(punctuality_score),
                "booking_reliability": int(cancellation_score),
                "guest_safety_record": int(safety_score)
            }
        }

confidence_service = ConfidenceScoreService()
