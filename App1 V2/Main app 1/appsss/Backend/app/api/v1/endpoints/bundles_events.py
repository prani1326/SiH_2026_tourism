import json
from typing import List
from fastapi import APIRouter, HTTPException
from app.core.firestore_db import get_firestore
from app.repositories.destination_repo import destination_repo
from app.schemas.destination import DayPassBundleOut

router = APIRouter()

@router.get("/day-passes/{destination_id}", response_model=List[DayPassBundleOut])
def get_day_passes(destination_id: str):
    """Day Pass Bundles with Firestore (Section 37)."""
    dest = destination_repo.get_by_id(destination_id)
    if not dest:
        results = destination_repo.search(query=destination_id, limit=1)
        dest = results[0] if results else None

    if not dest:
        raise HTTPException(status_code=404, detail="Destination not found.")

    firestore = get_firestore()
    docs = firestore.collection("day_pass_bundles").where("destination_id", "==", str(dest.id)).get()

    passes = []
    for doc in docs:
        d = doc.to_dict()
        inclusions = d.get("inclusions", [])
        if isinstance(inclusions, str):
            try:
                inclusions = json.loads(inclusions)
            except Exception:
                inclusions = [inclusions]

        passes.append({
            "id": doc.id,
            "destination_id": d.get("destination_id", str(dest.id)),
            "title": d.get("title", "City Explorer Day Pass"),
            "description": d.get("description", "All-inclusive monument entry & fast-track access"),
            "cover_image": d.get("cover_image", "https://images.unsplash.com/photo-1599661046827-dacff0c0f09a?auto=format&fit=crop&w=600&q=80"),
            "price": float(d.get("price", 999.0)),
            "original_price": float(d.get("original_price", 1499.0)),
            "validity_days": int(d.get("validity_days", 1)),
            "inclusions": inclusions
        })

    if not passes:
        # Provide curated fallback passes for destination
        passes = [
            {
                "id": f"pass-{str(dest.id)[:6]}-01",
                "destination_id": str(dest.id),
                "title": f"Ultimate {dest.name} Heritage Day Pass",
                "description": "Fast-track entry to top 4 monuments, audio guide voucher, and AC tourist coach transfer.",
                "cover_image": getattr(dest, "cover_image", "https://images.unsplash.com/photo-1599661046827-dacff0c0f09a?auto=format&fit=crop&w=600&q=80"),
                "price": 1199.0,
                "original_price": 1750.0,
                "validity_days": 1,
                "inclusions": ["Monument Fast-Track Ticket", "Digital Audio Guide", "Tourist Electric Shuttle"]
            }
        ]

    return passes

@router.get("/events/{destination_id}")
def get_local_events(destination_id: str):
    """Local Events & Cultural Festivals Calendar with Firestore (Section 38)."""
    dest = destination_repo.get_by_id(destination_id)
    if not dest:
        results = destination_repo.search(query=destination_id, limit=1)
        dest = results[0] if results else None

    dest_name = dest.name if dest else destination_id.title()

    firestore = get_firestore()
    docs = firestore.collection("events").where("destination_id", "==", str(dest.id) if dest else destination_id).get()
    events = [doc.to_dict() for doc in docs]

    if not events:
        events = [
            {
                "id": "evt-01",
                "title": f"Grand {dest_name} Heritage & Light Festival",
                "category": "Cultural Event",
                "date": "2026-10-18 to 2026-10-20",
                "venue": "Historic Fort Grounds",
                "price": 450.0,
                "cover_image": "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80",
                "description": "Spectacular sound and light illumination accompanied by traditional Rajasthani/Mughal instrumentalists."
            },
            {
                "id": "evt-02",
                "title": f"International Culinary Walk & Artisan Exhibition",
                "category": "Food & Craft",
                "date": "2026-11-05",
                "venue": "Craft Bazaar & Town Hall",
                "price": 0.0,
                "cover_image": "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=600&q=80",
                "description": "Live artisan stalls exhibiting miniature marble work, blue pottery, and pure vegetarian culinary delights."
            }
        ]

    return {
        "destination": dest_name,
        "events": events
    }
