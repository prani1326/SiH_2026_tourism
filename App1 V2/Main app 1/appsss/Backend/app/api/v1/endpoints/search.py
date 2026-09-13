import math
from typing import List, Optional, Dict, Any
from fastapi import APIRouter, Query
from app.repositories.destination_repo import destination_repo
from app.repositories.place_repo import place_repo

router = APIRouter()

def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Compute distance between two geographical points in kilometers."""
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat / 2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

@router.get("/")
@router.get("/global")
def global_search(
    q: Optional[str] = Query(None, description="Search query keyword"),
    category: Optional[str] = None,
    near_lat: Optional[float] = None,
    near_lon: Optional[float] = None,
    max_distance_km: float = 100.0,
    min_rating: Optional[float] = None,
    min_safety_score: Optional[int] = None,
    min_confidence_score: Optional[int] = None,
    dietary: Optional[str] = None,
    family_friendly: Optional[bool] = None,
    solo_friendly: Optional[bool] = None,
    accessibility_only: Optional[bool] = None
):
    """Global Search & Multi-Filter Engine backed by Firestore (Section 5)."""
    matched_destinations = []
    matched_places = []

    # 1. Search Destinations
    dest_results = destination_repo.search(query=q or "", limit=25)
    for d in dest_results:
        dist = None
        d_lat = getattr(d, "latitude", None)
        d_lon = getattr(d, "longitude", None)
        if near_lat is not None and near_lon is not None and d_lat is not None and d_lon is not None:
            dist = round(haversine_km(near_lat, near_lon, d_lat, d_lon), 1)
            if dist > max_distance_km:
                continue

        matched_destinations.append({
            "type": "destination",
            "id": str(d.id),
            "name": d.name,
            "state": d.state,
            "rating": getattr(d, "rating", 4.5),
            "hero_image_url": getattr(d, "hero_image_url", None),
            "safety_score": getattr(d, "safety_score", 90),
            "distance_km": dist
        })

    # 2. Search Places / Attractions / Hotels / Restaurants
    place_results = place_repo.search_places(
        destination_id=None,
        query=q,
        category=category,
        min_rating=min_rating,
        limit=50
    )

    for p in place_results:
        # Filters
        p_safety = getattr(p, "safety_score", 90)
        if min_safety_score and p_safety < min_safety_score:
            continue
        p_conf = getattr(p, "confidence_score", 90)
        if min_confidence_score and p_conf < min_confidence_score:
            continue
        if family_friendly is not None and getattr(p, "family_friendly", True) != family_friendly:
            continue
        if solo_friendly is not None and getattr(p, "solo_friendly", True) != solo_friendly:
            continue
        if accessibility_only and not getattr(p, "accessibility_friendly", False):
            continue

        p_lat = getattr(p, "latitude", 0.0)
        p_lon = getattr(p, "longitude", 0.0)
        dist = None
        if near_lat is not None and near_lon is not None and p_lat and p_lon:
            dist = round(haversine_km(near_lat, near_lon, p_lat, p_lon), 1)
            if dist > max_distance_km:
                continue

        dietary_tags = getattr(p, "dietary_tags", [])
        if dietary:
            diet_match = any(dietary.lower() in tag.lower() for tag in dietary_tags)
            if not diet_match:
                continue

        matched_places.append({
            "type": "place",
            "id": str(p.id),
            "destination_id": getattr(p, "destination_id", None),
            "name": p.name,
            "category": getattr(p, "category", "Attraction"),
            "subcategory": getattr(p, "subcategory", None),
            "cover_image": getattr(p, "cover_image", None),
            "price": getattr(p, "price", 0.0),
            "rating": getattr(p, "rating", 4.5),
            "safety_score": p_safety,
            "confidence_score": p_conf,
            "dietary_tags": dietary_tags,
            "distance_km": dist,
            "coordinates": {"lat": p_lat, "lon": p_lon}
        })

    return {
        "query": q,
        "total_results": len(matched_destinations) + len(matched_places),
        "destinations": matched_destinations,
        "places": matched_places,
        "search_suggestions": [
            "Taj Mahal sunrise ticket",
            "Pure vegetarian restaurants in Jaipur",
            "Goa watersports day pass",
            "Kerala backwaters houseboat"
        ]
    }
