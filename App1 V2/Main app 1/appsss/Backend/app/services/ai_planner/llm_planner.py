import json
import logging
from typing import Optional
from app.core.config import settings
from app.schemas.trip import AITripPlanRequest
from app.services.ai_planner.schemas import GeneratedItineraryPlan

logger = logging.getLogger("tourist_app.ai.llm")

class GeminiLLMPlanner:
    """
    Direct integration with Google Gemini 2.5 Flash using official Google GenAI SDK.
    Generates structured Pydantic JSON itineraries with schema enforcement and database grounding.
    """

    def __init__(self):
        import os
        self.api_key = settings.GEMINI_API_KEY or os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY") or ""
        self.model_name = settings.GEMINI_MODEL or "gemini-2.5-flash"
        self._client = None
        self._init_client()

    def _init_client(self):
        if self.api_key:
            try:
                from google import genai
                self._client = genai.Client(api_key=self.api_key)
                logger.info(f"Google Gemini Client initialized successfully for model: {self.model_name}")
            except Exception as e:
                logger.error(f"Failed to initialize Google GenAI SDK client: {e}")
                self._client = None
        else:
            logger.info("No Gemini API key configured. Set GEMINI_API_KEY or GOOGLE_API_KEY to enable LLM planning.")
            self._client = None

    @property
    def is_available(self) -> bool:
        return self._client is not None

    def generate_plan(self, request: AITripPlanRequest, destination_context: str = "") -> Optional[GeneratedItineraryPlan]:
        if not self.is_available:
            logger.info("Gemini planner unavailable (missing/invalid key). Proceeding to deterministic fallback planner.")
            return None

        prompt = f"""
        You are an expert AI Travel Planner & Concierge for India and Global Tourism.
        Analyze the following user selections and verified database records to generate an intelligent, realistic, day-by-day itinerary.

        === USER SELECTIONS ===
        - Destination: {request.destination}
        - Duration: {request.duration_days or 3} Days ({request.start_date} to {request.end_date})
        - Travelers: {request.traveler_count} ({request.travel_style})
        - Total Budget: ₹{request.budget:.0f} {request.currency}
        - Selected Interests: {', '.join(request.interests or ['Sightseeing', 'Food', 'Culture'])}
        - Preferred Activities: {', '.join(request.activities or ['Monument Tours', 'Local Food Trails'])}
        - Food Preference: {request.food_preference}
        - Hotel Preference: {request.hotel_preference}
        - Transport Mode: {request.transport_preference}
        - Travel Pace: {request.travel_pace}
        - Walking Tolerance: {request.walking_tolerance}

        === VERIFIED LOCAL DATABASE FACTS ===
        {destination_context}

        === PLANNING & REASONING RULES ===
        1. Ground in Facts: Prioritize the supplied database attractions, places, activities, and food spots. Preserve original `place_id` where available.
        2. Daily Structure: Each day must contain 3-4 structured activities:
           - Morning (08:30/09:00 - 12:00): Heritage, nature, or primary iconic monuments.
           - Afternoon (12:30 - 15:30): Authentic dining following '{request.food_preference}', followed by indoor/shaded exploration.
           - Evening (16:30 - 19:30): Scenic viewpoints, sunset walks, beaches, or water sports.
           - Night (20:00 - 22:00): Cultural dinners, night bazaars, or relaxing spots.
        3. Transport & Pacing:
           - Respect travel pace '{request.travel_pace}' (Relaxed = more buffer time, Balanced = optimal, Fast = packed highlights).
           - Set realistic travel times (10-30 mins) using preferred transport '{request.transport_preference}'.
        4. Budget Optimization:
           - Distribute the total budget (₹{request.budget:.0f}) across `budget_breakdown` (hotel, food, transport, activities, miscellaneous) matching '{request.hotel_preference}' and activity fees.
           - Ensure total estimated cost stays within budget.
        5. Return strict JSON matching the GeneratedItineraryPlan schema.
        """

        try:
            from google.genai import types
            response = self._client.models.generate_content(
                model=self.model_name,
                contents=prompt,
                config=types.GenerateContentConfig(
                    response_mime_type="application/json",
                    response_schema=GeneratedItineraryPlan,
                    temperature=0.3
                )
            )

            if response and response.text:
                plan_dict = json.loads(response.text)
                return GeneratedItineraryPlan(**plan_dict)
        except Exception as exc:
            logger.error(f"Gemini LLM planning generation failed: {exc}")
            return None

gemini_planner = GeminiLLMPlanner()
