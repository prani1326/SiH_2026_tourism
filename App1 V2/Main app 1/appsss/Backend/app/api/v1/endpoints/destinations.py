from typing import List, Optional
from fastapi import APIRouter, HTTPException
from app.repositories.destination_repo import destination_repo
from app.schemas.destination import DestinationOut, DestinationDetailOut

router = APIRouter()

def _serialize_destination(d) -> dict:
    return {
        "id": str(d.id),
        "name": d.name,
        "state": d.state,
        "country": d.country,
        "latitude": d.latitude,
        "longitude": d.longitude,
        "hero_image_url": d.hero_image_url,
        "description": d.description,
        "known_for": getattr(d, "known_for", None) or d.description,
        "rating": d.rating,
        "best_time_to_visit": d.best_time_to_visit,
        "ideal_stay": getattr(d, "ideal_stay", None) or "2–3 days",
        "budget_per_day": getattr(d, "budget_per_day", None) or d.estimated_budget_tier,
        "estimated_budget_tier": d.estimated_budget_tier,
        "weather_temperature": d.weather_temperature,
        "weather_condition": d.weather_condition,
        "safety_score": int(d.safety_score) if isinstance(d.safety_score, (int, float)) else 90,
        "tags": d.tags or [],
        "top_attractions": getattr(d, "top_attractions", []) or [],
        "activities": getattr(d, "activities", []) or [],
        "famous_food": getattr(d, "famous_food", []) or [],
        "local_transport": getattr(d, "local_transport", []) or [],
        "nearby_places": getattr(d, "nearby_places", []) or [],
        "is_featured": bool(d.is_featured),
        "is_popular": bool(d.is_popular)
    }

@router.get("", response_model=List[DestinationOut], include_in_schema=False)
@router.get("/", response_model=List[DestinationOut])
def list_destinations(
    state: Optional[str] = None,
    tag: Optional[str] = None,
    query: Optional[str] = None,
    popular_only: bool = False
):
    """List Destinations with filters from Firestore (Section 4, 43)."""
    results = destination_repo.search(
        query=query,
        state=state,
        tag=tag,
        is_featured=True if popular_only else None,
        limit=100
    )

    return [_serialize_destination(d) for d in results]

@router.get("/popular", response_model=List[DestinationOut])
def get_popular_destinations(limit: int = 20):
    """List Popular Destinations from Firestore (Section 5)."""
    results = destination_repo.search(
        is_featured=True,
        limit=limit
    )
    if not results:
        results = destination_repo.list_all(limit=limit)
    return [_serialize_destination(d) for d in results]

@router.get("/{destination_id}", response_model=DestinationDetailOut)
def get_destination_detail(destination_id: str):
    """Comprehensive Destination Details from Firestore (Section 7)."""
    dest = destination_repo.get_by_id(destination_id)
    if not dest:
        # Also allow finding by name for user convenience (e.g., 'Agra')
        matches = destination_repo.search(query=destination_id, limit=1)
        if matches:
            dest = matches[0]

    if not dest:
        raise HTTPException(status_code=404, detail="Destination not found.")

    places = dest.places or []
    if not places:
        from app.repositories.place_repo import place_repo
        places = place_repo.search_places(destination_id=str(dest.id))
        if not places:
            # Try searching by destination name
            places = place_repo.search_places(query=dest.name)

    scam_alerts = dest.scam_alerts or [
        {
            "id": f"scam-{dest.id}-01",
            "title": f"Unofficial Guides at {dest.name} monuments",
            "severity": "Medium",
            "description": "Always hire government-approved guides with official RFID badges.",
            "prevention_tip": "Book via official tourism counters or state portals."
        }
    ]

    day_passes = dest.day_passes or []

    suggested_itineraries = [
        {
            "title": f"Iconic 3-Day {dest.name} Tour",
            "duration": "3 Days / 2 Nights",
            "highlights": [getattr(p, "name", "") for p in places[:3]],
            "estimated_cost": "₹15,000 - ₹25,000 per person"
        },
        {
            "title": f"Slow Cultural & Culinary {dest.name}",
            "duration": "5 Days / 4 Nights",
            "highlights": ["Heritage walks", "Local bazaars", "Cooking masterclass"],
            "estimated_cost": "₹28,000 - ₹40,000 per person"
        }
    ]

    detail = _serialize_destination(dest)
    detail.update({
        "scam_alerts": scam_alerts,
        "local_etiquette": dest.local_etiquette or [],
        "emergency_info": dest.emergency_info or {},
        "places": [
            {
                "id": str(p.id if hasattr(p, "id") else p.get("id")),
                "destination_id": str(dest.id),
                "name": getattr(p, "name", None) or (p.get("name") if isinstance(p, dict) else ""),
                "category": getattr(p, "category", None) or (p.get("category") if isinstance(p, dict) else "Monument"),
                "subcategory": getattr(p, "subcategory", None) or (p.get("subcategory") if isinstance(p, dict) else None),
                "cover_image": getattr(p, "image_url", None) or getattr(p, "cover_image", None) or (p.get("image_url") or p.get("cover_image") if isinstance(p, dict) else ""),
                "description": getattr(p, "description", None) or (p.get("description") if isinstance(p, dict) else ""),
                "latitude": float(getattr(p, "latitude", 0.0) or (p.get("latitude", 0.0) if isinstance(p, dict) else 0.0)),
                "longitude": float(getattr(p, "longitude", 0.0) or (p.get("longitude", 0.0) if isinstance(p, dict) else 0.0)),
                "address": getattr(p, "address", None) or (p.get("address") if isinstance(p, dict) else None),
                "opening_hours": getattr(p, "opening_hours", None) or (p.get("opening_hours") if isinstance(p, dict) else "09:00 AM - 06:00 PM"),
                "visit_duration_minutes": int(getattr(p, "estimated_duration_hours", 2.0) * 60 if hasattr(p, "estimated_duration_hours") else (p.get("visit_duration_minutes", 120) if isinstance(p, dict) else 120)),
                "price": float(getattr(p, "entry_fee", 0.0) if hasattr(p, "entry_fee") else (p.get("price", 0.0) if isinstance(p, dict) else 0.0)),
                "rating": float(getattr(p, "rating", 4.5) or (p.get("rating", 4.5) if isinstance(p, dict) else 4.5)),
                "reviews_count": int(getattr(p, "reviews_count", 250) if hasattr(p, "reviews_count") else (p.get("reviews_count", 250) if isinstance(p, dict) else 250)),
                "safety_score": int(float(getattr(p, "safety_score", 95) if hasattr(p, "safety_score") else (p.get("safety_score", 95) if isinstance(p, dict) else 95))),
                "confidence_score": int(float(getattr(p, "confidence_score", 95) if hasattr(p, "confidence_score") else (p.get("confidence_score", 95) if isinstance(p, dict) else 95))),
                "confidence_breakdown": getattr(p, "confidence_breakdown", None) or (p.get("confidence_breakdown") if isinstance(p, dict) else {}),
                "dietary_tags": getattr(p, "dietary_tags", []) if hasattr(p, "dietary_tags") else (p.get("dietary_tags", []) if isinstance(p, dict) else []),
                "diet_confidence": int(getattr(p, "diet_confidence", 95) if hasattr(p, "diet_confidence") and getattr(p, "diet_confidence") is not None else (p.get("diet_confidence", 95) if isinstance(p, dict) and p.get("diet_confidence") is not None else 95)),
                "accessibility_friendly": bool(getattr(p, "is_wheelchair_accessible", True) if hasattr(p, "is_wheelchair_accessible") else (p.get("accessibility_friendly", True) if isinstance(p, dict) else True)),
                "family_friendly": True,
                "solo_friendly": True,
                "transport_options": [
                    {"mode": "Auto", "price": 50.0},
                    {"mode": "Taxi", "price": 200.0},
                    {"mode": "Metro", "price": 30.0}
                ]
            } for p in places
        ],
        "day_passes": [
            {
                "id": str(dp.id if hasattr(dp, "id") else dp.get("id")),
                "destination_id": str(dest.id),
                "title": getattr(dp, "title", None) or (dp.get("title") if isinstance(dp, dict) else ""),
                "description": getattr(dp, "description", None) or (dp.get("description") if isinstance(dp, dict) else ""),
                "cover_image": getattr(dp, "cover_image", None) or (dp.get("cover_image") if isinstance(dp, dict) else None),
                "price": float(getattr(dp, "price", 999.0) if hasattr(dp, "price") else (dp.get("price", 999.0) if isinstance(dp, dict) else 999.0)),
                "original_price": float(getattr(dp, "original_price", 1499.0) if hasattr(dp, "original_price") else (dp.get("original_price", 1499.0) if isinstance(dp, dict) else 1499.0)),
                "validity_days": int(getattr(dp, "validity_days", 1) if hasattr(dp, "validity_days") else (dp.get("validity_days", 1) if isinstance(dp, dict) else 1)),
                "inclusions": getattr(dp, "included_attractions", []) if hasattr(dp, "included_attractions") else (dp.get("inclusions", []) if isinstance(dp, dict) else [])
            } for dp in day_passes
        ],
        "suggested_itineraries": suggested_itineraries
    })
    return detail

@router.get("/{destination_id}/guide")
def get_destination_guide(destination_id: str):
    """Curated Destination Guide from Firestore (Section 39)."""
    dest = destination_repo.get_by_id(destination_id)
    if not dest:
        matches = destination_repo.search(query=destination_id, limit=1)
        if matches:
            dest = matches[0]

    if not dest:
        raise HTTPException(status_code=404, detail="Destination not found.")

    return {
        "destination": dest.name,
        "state": dest.state,
        "quick_summary": dest.description,
        "best_season": dest.best_time_to_visit,
        "typical_budget_tier": dest.estimated_budget_tier,
        "safety_index": f"{dest.safety_score}/100",
        "top_scams_to_avoid": dest.scam_alerts,
        "essential_cultural_etiquette": dest.local_etiquette,
        "emergency_contacts": dest.emergency_info
    }
