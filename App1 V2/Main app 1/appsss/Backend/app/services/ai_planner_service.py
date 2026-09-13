from typing import Dict, Any, Optional
from app.schemas.trip import AITripPlanRequest
from app.services.ai_planner.service import ai_planner_service

class AIPlannerService:
    """
    Intelligent AI Trip Planner Engine.
    Delegates to the Enterprise AI Planning Engine with Gemini LLM + Validation Pipeline + Fallback.
    Uses Firestore as the underlying state store.
    """

    @staticmethod
    def generate_itinerary(db: Optional[Any] = None, request: AITripPlanRequest = None, user_id: str = None) -> Dict[str, Any]:
        return ai_planner_service.generate_itinerary(db, request, user_id=user_id)
