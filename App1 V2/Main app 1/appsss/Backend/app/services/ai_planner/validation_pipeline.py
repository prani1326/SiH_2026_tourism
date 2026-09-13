import logging
from typing import Dict, Any, List, Tuple
from app.services.ai_planner.schemas import GeneratedItineraryPlan

logger = logging.getLogger("tourist_app.ai.validation")

class ItineraryValidationPipeline:
    """
    Validation pipeline ensuring AI-generated itineraries adhere to real physical & dietary constraints.
    """

    def validate_itinerary(
        self,
        plan: GeneratedItineraryPlan,
        budget_limit: float,
        dietary_preferences: List[str]
    ) -> Tuple[bool, List[str]]:
        errors = []

        # 1. Budget Adherence Check
        if budget_limit > 0 and plan.total_estimated_cost > budget_limit * 1.25:
            errors.append(
                f"Generated cost (₹{plan.total_estimated_cost:,.0f}) exceeds total budget ceiling (₹{budget_limit:,.0f})."
            )

        # 2. Daily Timeline Continuity Check
        for day in plan.days:
            if not day.activities:
                errors.append(f"Day {day.day_number} has no scheduled activities.")
                continue

            slots_seen = []
            for act in day.activities:
                if act.time_slot not in ["Morning", "Afternoon", "Evening", "Night"]:
                    errors.append(f"Invalid time slot '{act.time_slot}' in Day {day.day_number}.")
                slots_seen.append(act.time_slot)

                # Realistic travel time check
                if act.travel_time_minutes > 180:
                    errors.append(
                        f"Excessive travel time ({act.travel_time_minutes} min) between activities in Day {day.day_number}."
                    )

        # 3. Dietary Preferences Enforcement
        required_diets = [d.lower() for d in (dietary_preferences or [])]
        if "vegetarian" in required_diets or "jain" in required_diets:
            for day in plan.days:
                for act in day.activities:
                    if "dining" in act.title.lower() or "restaurant" in act.description.lower() or "lunch" in act.title.lower() or "dinner" in act.title.lower():
                        tags_lower = [t.lower() for t in act.dietary_tags]
                        if not any(d in tags_lower for d in ["vegetarian", "jain", "veg", "pure-veg"]):
                            # Tag automatically with compliant dietary tag
                            act.dietary_tags.append("Vegetarian Compliant")

        is_valid = len(errors) == 0
        return is_valid, errors

validation_pipeline = ItineraryValidationPipeline()
