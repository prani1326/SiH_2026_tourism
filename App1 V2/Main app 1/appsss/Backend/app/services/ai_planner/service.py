import json
import time
import logging
import uuid
from datetime import datetime, timezone
from typing import Dict, Any, Optional
from app.core.firestore_db import get_firestore
from app.repositories.destination_repo import destination_repo
from app.schemas.trip import AITripPlanRequest
from app.services.ai_planner.guardrails import sanitize_and_guard_prompt
from app.services.ai_planner.llm_planner import gemini_planner
from app.services.ai_planner.fallback_planner import fallback_planner
from app.services.ai_planner.validation_pipeline import validation_pipeline
from app.services.ai_planner.schemas import GeneratedItineraryPlan

logger = logging.getLogger("tourist_app.ai.service")

class AIPlannerMasterService:
    """
    Enterprise AI Trip Planning Orchestrator.
    - Prompt Sanitization & Injection Defense
    - Gemini LLM generation with Structured Pydantic Output
    - Feasibility & Constraint Validation Pipeline
    - Resilient Deterministic Fallback using Firestore Repositories
    - Comprehensive Telemetry & Cost Logging directly to Firestore
    """

    def generate_itinerary(
        self,
        db: Optional[Any] = None,
        request: Optional[AITripPlanRequest] = None,
        user_id: Optional[str] = None
    ) -> Dict[str, Any]:
        start_time = time.time()
        
        # 1. Guardrails
        cleaned_dest, flagged = sanitize_and_guard_prompt(request.destination)
        request.destination = cleaned_dest

        # 2. Local context grounding via Firestore
        matches = destination_repo.search(query=request.destination.strip(), limit=1)
        dest = matches[0] if matches else None
        local_context = ""
        if dest:
            ctx_items = []
            if getattr(dest, "known_for", None):
                ctx_items.append(f"Destination Known For: {dest.known_for}")
            if getattr(dest, "top_attractions", None):
                ctx_items.append(f"Top Must-Visit Attractions: {', '.join(dest.top_attractions)}")
            if getattr(dest, "activities", None):
                ctx_items.append(f"Top Recommended Activities: {', '.join(dest.activities)}")
            if getattr(dest, "famous_food", None):
                ctx_items.append(f"Famous Local Dishes & Food: {', '.join(dest.famous_food)}")
            if getattr(dest, "local_transport", None):
                ctx_items.append(f"Available Local Transport: {', '.join(dest.local_transport)}")
            if getattr(dest, "best_time_to_visit", None):
                ctx_items.append(f"Best Time to Visit: {dest.best_time_to_visit}")
            if getattr(dest, "ideal_stay", None):
                ctx_items.append(f"Ideal Stay Duration: {dest.ideal_stay}")
            if getattr(dest, "budget_per_day", None):
                ctx_items.append(f"Budget per day: {dest.budget_per_day}")
            if getattr(dest, "nearby_places", None):
                ctx_items.append(f"Nearby Excursions: {', '.join(dest.nearby_places)}")
            if dest.places:
                places_txt = "\n".join([f"- {getattr(p, 'name', '')} ({getattr(p, 'category', '')}): {getattr(p, 'description', '')}" for p in dest.places[:10]])
                ctx_items.append(f"Verified Places:\n{places_txt}")
            local_context = "\n".join(ctx_items)

        plan: Optional[GeneratedItineraryPlan] = None
        model_used = "deterministic_rule_engine"
        is_fallback = True

        # 3. LLM Generation
        if gemini_planner.is_available:
            try:
                candidate_plan = gemini_planner.generate_plan(request, destination_context=local_context)
                if candidate_plan:
                    is_valid, validation_errors = validation_pipeline.validate_itinerary(
                        plan=candidate_plan,
                        budget_limit=request.budget,
                        dietary_preferences=[request.food_preference]
                    )
                    if is_valid:
                        plan = candidate_plan
                        model_used = gemini_planner.model_name
                        is_fallback = False
                    else:
                        logger.warning(f"LLM plan failed validation: {validation_errors}. Triggering fallback.")
            except Exception as e:
                logger.error(f"Error in LLM plan pipeline: {e}")

        # 4. Fallback execution
        if not plan:
            plan = fallback_planner.generate(db, request)
            model_used = "deterministic_rule_engine"
            is_fallback = True

        latency_ms = int((time.time() - start_time) * 1000)

        # 5. Persist generated trip to Firestore
        trip_id = str(uuid.uuid4())
        try:
            firestore = get_firestore()
            trip_data = {
                "id": trip_id,
                "user_id": user_id or "demo-traveler-touristapp",
                "destination_id": str(dest.id) if dest else request.destination.lower(),
                "destination_name": dest.name if dest else request.destination,
                "title": plan.title or f"{plan.duration_days}-Day Trip to {request.destination}",
                "start_date": request.start_date,
                "end_date": request.end_date,
                "traveler_count": getattr(request, "traveler_count", 1) or getattr(request, "num_travelers", 1) or 1,
                "travel_style": request.travel_style,
                "total_budget": float(request.budget),
                "total_estimated_cost": float(plan.total_estimated_cost),
                "status": "active",
                "itinerary_json": plan.model_dump_json(),
                "budget_breakdown": plan.budget_breakdown.model_dump() if hasattr(plan, "budget_breakdown") else {},
                "selected_preferences": {
                    "interests": request.interests,
                    "activities": request.activities,
                    "food_preference": request.food_preference,
                    "hotel_preference": request.hotel_preference,
                    "transport_preference": request.transport_preference,
                    "travel_pace": request.travel_pace
                },
                "days": [d.model_dump() for d in plan.days],
                "created_at": datetime.now(timezone.utc).isoformat(),
                "updated_at": datetime.now(timezone.utc).isoformat()
            }
            firestore.collection("trips").document(trip_id).set(trip_data)
        except Exception as e:
            logger.warning(f"Could not persist trip to Firestore: {e}")

        # 6. Log telemetry directly to Firestore
        try:
            firestore = get_firestore()
            log_id = str(uuid.uuid4())
            firestore.collection("ai_generation_logs").document(log_id).set({
                "id": log_id,
                "user_id": user_id,
                "prompt_type": "trip_planner",
                "model_name": model_used,
                "prompt_text": f"Dest: {request.destination}, Duration: {plan.duration_days}d, Budget: {request.budget}",
                "response_json": plan.model_dump_json(),
                "latency_ms": latency_ms,
                "status": "Success",
                "created_at": datetime.now(timezone.utc).isoformat()
            })
        except Exception as e:
            logger.warning(f"Could not persist AI generation telemetry to Firestore: {e}")

        # 7. Format response for API compatibility
        output = plan.model_dump()
        output["id"] = trip_id
        output["trip_id"] = trip_id
        output["is_fallback"] = is_fallback
        output["model_used"] = model_used
        output["latency_ms"] = latency_ms
        return output

ai_planner_service = AIPlannerMasterService()
