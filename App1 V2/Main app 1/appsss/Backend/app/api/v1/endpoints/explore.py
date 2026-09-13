from typing import Optional, List
from fastapi import APIRouter, Query
from app.repositories.destination_repo import destination_repo
from app.repositories.place_repo import place_repo

router = APIRouter()

EXPLORE_CATEGORIES = [
    {"name": "Heritage", "icon": "fort", "cover": "https://images.unsplash.com/photo-1599661046827-dacff0c0f09a?auto=format&fit=crop&w=600&q=80"},
    {"name": "Beaches", "icon": "beach_access", "cover": "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=600&q=80"},
    {"name": "Mountains", "icon": "terrain", "cover": "https://images.unsplash.com/photo-1581793745862-99fde7fa73d2?auto=format&fit=crop&w=600&q=80"},
    {"name": "Spiritual", "icon": "self_improvement", "cover": "https://images.unsplash.com/photo-1561361513-2d000a50f0dc?auto=format&fit=crop&w=600&q=80"},
    {"name": "Adventure", "icon": "kayaking", "cover": "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=600&q=80"},
    {"name": "Wildlife", "icon": "pets", "cover": "https://images.unsplash.com/photo-1575550959106-5a7defe28b56?auto=format&fit=crop&w=600&q=80"},
    {"name": "Food & Nightlife", "icon": "restaurant", "cover": "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=600&q=80"},
    {"name": "Shopping & Bazaars", "icon": "storefront", "cover": "https://images.unsplash.com/photo-1477587458883-47145ed94245?auto=format&fit=crop&w=600&q=80"}
]

@router.get("/categories")
def get_categories():
    """Explore Categories list (Section 6)."""
    return {"categories": EXPLORE_CATEGORIES}

@router.get("/category/{category_name}")
def explore_by_category(
    category_name: str,
    view_mode: str = Query("list", description="View mode: 'list' or 'map'")
):
    """Browse Explore Items with List/Map View toggle backed by Firestore (Section 6)."""
    cat_lower = category_name.strip().lower()

    # Find matching destinations
    all_dests = destination_repo.get_all(limit=100)
    matched_destinations = [
        d for d in all_dests 
        if cat_lower in [t.lower() for t in getattr(d, "tags", [])]
        or cat_lower in (getattr(d, "known_for", "") or "").lower()
        or cat_lower in (getattr(d, "description", "") or "").lower()
        or (cat_lower == "heritage" and any(k in (getattr(d, "name", "") + " " + getattr(d, "description", "")).lower() for k in ["heritage", "fort", "palace", "agra", "delhi", "jaipur", "monument"]))
    ]

    # Find matching places
    all_places = place_repo.get_all(limit=100)
    matched_places = [
        p for p in all_places 
        if cat_lower in (getattr(p, "category", "") or "").lower() or 
           (getattr(p, "subcategory", None) and cat_lower in str(p.subcategory).lower()) or
           (cat_lower == "heritage" and (getattr(p, "category", "") or "").lower() in ["monument", "heritage", "historical site"])
    ]

    items = []
    map_markers = []

    for d in matched_destinations:
        d_lat = getattr(d, "latitude", 0.0)
        d_lon = getattr(d, "longitude", 0.0)
        entry = {
            "type": "destination",
            "id": str(d.id),
            "title": d.name,
            "subtitle": f"{d.state}, {getattr(d, 'country', 'India')}",
            "rating": getattr(d, "rating", 4.5),
            "cover_image": getattr(d, "cover_image", None),
            "safety_score": getattr(d, "safety_score", 90),
            "latitude": d_lat,
            "longitude": d_lon
        }
        items.append(entry)
        map_markers.append({
            "id": str(d.id),
            "type": "destination",
            "title": d.name,
            "coordinates": {"lat": d_lat, "lon": d_lon},
            "rating": getattr(d, "rating", 4.5)
        })

    for p in matched_places:
        p_lat = getattr(p, "latitude", 0.0)
        p_lon = getattr(p, "longitude", 0.0)
        entry = {
            "type": "place",
            "id": str(p.id),
            "title": p.name,
            "subtitle": getattr(p, "subcategory", None) or getattr(p, "category", "Attraction"),
            "rating": getattr(p, "rating", 4.5),
            "cover_image": getattr(p, "cover_image", None),
            "safety_score": getattr(p, "safety_score", 90),
            "price": getattr(p, "price", 0.0),
            "latitude": p_lat,
            "longitude": p_lon
        }
        items.append(entry)
        map_markers.append({
            "id": str(p.id),
            "type": "place",
            "title": p.name,
            "coordinates": {"lat": p_lat, "lon": p_lon},
            "rating": getattr(p, "rating", 4.5)
        })

    return {
        "category": category_name.title(),
        "view_mode": view_mode,
        "count": len(items),
        "items": items if view_mode == "list" else [],
        "map_markers": map_markers
    }
