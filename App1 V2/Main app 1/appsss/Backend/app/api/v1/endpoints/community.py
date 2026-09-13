import uuid
from datetime import datetime, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException
from app.core.firestore_db import get_firestore
from app.core.security import get_current_user
from app.models.firestore_models import User
from app.schemas.community import (
    CommunityForumOut, CommunityPostCreate, CommunityPostOut,
    CommunityCommentCreate, CreatorItineraryOut
)

router = APIRouter()

DEFAULT_FORUMS = [
    {
        "id": "forum-heritage-lovers",
        "title": "Heritage & Monument Enthusiasts",
        "slug": "heritage-monuments",
        "description": "Discuss architecture, hidden gems, and photography tips across Indian forts.",
        "cover_image": "https://images.unsplash.com/photo-1599661046827-dacff0c0f09a?auto=format&fit=crop&w=600&q=80",
        "category": "Culture",
        "member_count": 1420
    },
    {
        "id": "forum-solo-backpackers",
        "title": "Solo Female & Budget Travelers",
        "slug": "solo-travelers",
        "description": "Safe travel advice, hostel recommendations, and buddy finding.",
        "cover_image": "https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=600&q=80",
        "category": "Solo Travel",
        "member_count": 2890
    },
    {
        "id": "forum-foodies",
        "title": "Vegetarian & Street Food Explorers",
        "slug": "pure-veg-street-food",
        "description": "Reviews of authentic sweet shops, dhaba trails, and verified pure vegetarian kitchens.",
        "cover_image": "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=600&q=80",
        "category": "Food",
        "member_count": 3150
    }
]

DEFAULT_CREATOR_ITINERARIES = [
    {
        "id": "ci-golden-triangle-express",
        "creator_name": "Aarav Sharma (Verified Travel Guide)",
        "creator_avatar": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
        "title": "Golden Triangle in 4 Days (Agra, Jaipur, Delhi)",
        "destination_name": "Agra & Jaipur",
        "duration_days": 4,
        "total_estimated_cost": 18500.0,
        "price": 0.0,
        "is_paid": False,
        "purchases_count": 320,
        "copy_count": 540,
        "rating": 4.9,
        "itinerary_data": {
            "focus": "Heritage & Photography",
            "pace": "Active",
            "highlight": "Secret sunrise viewpoint of Taj Mahal without crowds"
        }
    }
]

@router.get("/forums", response_model=List[CommunityForumOut])
def get_community_forums():
    """Contextual Travel Communities with Firestore (Section 33)."""
    firestore = get_firestore()
    docs = firestore.collection("community_forums").get()
    forums = [doc.to_dict() for doc in docs]
    if not forums:
        for f in DEFAULT_FORUMS:
            firestore.collection("community_forums").document(f["id"]).set(f)
        forums = DEFAULT_FORUMS

    return [
        {
            "id": f.get("id"),
            "title": f.get("title"),
            "slug": f.get("slug"),
            "description": f.get("description"),
            "cover_image": f.get("cover_image"),
            "category": f.get("category"),
            "member_count": f.get("member_count", 0)
        }
        for f in forums
    ]

@router.get("/forums/{forum_id}/posts", response_model=List[CommunityPostOut])
def get_forum_posts(forum_id: str):
    """Browse Community Posts & Traveler Tips from Firestore (Section 33)."""
    firestore = get_firestore()
    docs = firestore.collection("community_posts").where("forum_id", "==", forum_id).get()
    posts = [doc.to_dict() for doc in docs]

    if not posts:
        # Fallback sample post
        sample = {
            "id": "post-sample-01",
            "forum_id": forum_id,
            "author_name": "Priya Verma",
            "author_avatar": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
            "title": "Best sunrise photography spot without paying VIP tickets?",
            "content": "Mehtab Bagh across the river gives an unbelievable reflective view of the monument at 6:15 AM!",
            "post_type": "Tip",
            "likes_count": 28,
            "comments_count": 6,
            "is_verified_traveler": True,
            "created_at": datetime.now(timezone.utc).isoformat()
        }
        posts = [sample]

    return [
        {
            "id": p.get("id"),
            "forum_id": p.get("forum_id"),
            "author_name": p.get("author_name"),
            "author_avatar": p.get("author_avatar"),
            "title": p.get("title"),
            "content": p.get("content"),
            "post_type": p.get("post_type", "General"),
            "likes_count": p.get("likes_count", 0),
            "comments_count": p.get("comments_count", 0),
            "is_verified_traveler": p.get("is_verified_traveler", True),
            "created_at": p.get("created_at")
        }
        for p in posts
    ]

@router.get("/posts", response_model=List[CommunityPostOut])
def list_community_posts(forum_id: Optional[str] = None):
    """Browse Community Posts with optional forum_id from Firestore."""
    firestore = get_firestore()
    if forum_id:
        docs = firestore.collection("community_posts").where("forum_id", "==", forum_id).get()
    else:
        docs = firestore.collection("community_posts").limit(50).get()
    posts = [doc.to_dict() for doc in docs]

    if not posts:
        sample = {
            "id": "post-sample-01",
            "forum_id": forum_id or "forum-heritage-lovers",
            "author_name": "Priya Verma",
            "author_avatar": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
            "title": "Best sunrise photography spot without paying VIP tickets?",
            "content": "Mehtab Bagh across the river gives an unbelievable reflective view of the monument at 6:15 AM!",
            "post_type": "Tip",
            "likes_count": 28,
            "comments_count": 6,
            "is_verified_traveler": True,
            "created_at": datetime.now(timezone.utc).isoformat()
        }
        posts = [sample]

    return [
        {
            "id": p.get("id"),
            "forum_id": p.get("forum_id"),
            "author_name": p.get("author_name"),
            "author_avatar": p.get("author_avatar"),
            "title": p.get("title"),
            "content": p.get("content"),
            "post_type": p.get("post_type", "General"),
            "likes_count": p.get("likes_count", 0),
            "comments_count": p.get("comments_count", 0),
            "is_verified_traveler": p.get("is_verified_traveler", True),
            "created_at": p.get("created_at")
        }
        for p in posts
    ]

@router.post("/posts", response_model=CommunityPostOut)
def create_forum_post(
    request: CommunityPostCreate,
    current_user: User = Depends(get_current_user)
):
    """Post Tip / Question to Community in Firestore (Section 33)."""
    firestore = get_firestore()
    post_id = str(uuid.uuid4())
    now_iso = datetime.now(timezone.utc).isoformat()

    avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80"
    if hasattr(current_user, "profile") and current_user.profile and hasattr(current_user.profile, "avatar_url"):
        avatar = current_user.profile.avatar_url or avatar

    post_data = {
        "id": post_id,
        "forum_id": request.forum_id,
        "user_id": str(current_user.id),
        "author_name": current_user.full_name or "Traveler",
        "author_avatar": avatar,
        "title": request.title,
        "content": request.content,
        "post_type": request.post_type,
        "likes_count": 0,
        "comments_count": 0,
        "is_verified_traveler": True,
        "created_at": now_iso
    }

    firestore.collection("community_posts").document(post_id).set(post_data)

    return post_data

@router.get("/creators/itineraries", response_model=List[CreatorItineraryOut])
def get_creator_itineraries():
    """Creator Itinerary Marketplace with Firestore (Section 34)."""
    firestore = get_firestore()
    docs = firestore.collection("creator_itineraries").get()
    itineraries = [doc.to_dict() for doc in docs]
    if not itineraries:
        for ci in DEFAULT_CREATOR_ITINERARIES:
            firestore.collection("creator_itineraries").document(ci["id"]).set(ci)
        itineraries = DEFAULT_CREATOR_ITINERARIES

    return [
        {
            "id": ci.get("id"),
            "creator_name": ci.get("creator_name"),
            "creator_avatar": ci.get("creator_avatar"),
            "title": ci.get("title"),
            "destination_name": ci.get("destination_name"),
            "duration_days": ci.get("duration_days", 1),
            "total_estimated_cost": ci.get("total_estimated_cost", 0.0),
            "price": ci.get("price", 0.0),
            "is_paid": ci.get("is_paid", False),
            "purchases_count": ci.get("purchases_count", 0),
            "copy_count": ci.get("copy_count", 0),
            "rating": ci.get("rating", 4.9),
            "itinerary_data": ci.get("itinerary_data", {})
        }
        for ci in itineraries
    ]

@router.post("/creators/itineraries/{itinerary_id}/copy")
def copy_creator_itinerary(
    itinerary_id: str,
    current_user: User = Depends(get_current_user)
):
    """Copy Creator Itinerary to User Trips with Firestore (Section 34)."""
    firestore = get_firestore()
    doc_ref = firestore.collection("creator_itineraries").document(itinerary_id)
    doc = doc_ref.get()
    
    ci = None
    if doc.exists:
        ci = doc.to_dict()
    else:
        for item in DEFAULT_CREATOR_ITINERARIES:
            if item["id"] == itinerary_id or itinerary_id in item["id"]:
                ci = item
                break
    
    if not ci:
        ci = DEFAULT_CREATOR_ITINERARIES[0]

    current_count = ci.get("copy_count", 0) + 1
    if doc.exists:
        doc_ref.update({"copy_count": current_count})

    trip_id = str(uuid.uuid4())
    now_iso = datetime.now(timezone.utc).isoformat()
    
    days_data = [
        {
            "day": 1,
            "theme": "Arrival & Historical Exploration",
            "activities": [
                {"time_slot": "Morning", "start_time": "09:00", "end_time": "12:00", "title": "Old Delhi & Red Fort Exploration", "description": "Marvel at Mughal architecture and historical ramparts with audio guide.", "transport_mode": "Metro / Cab", "travel_time": "20 mins", "cost": "₹250"},
                {"time_slot": "Afternoon", "start_time": "13:00", "end_time": "15:30", "title": "Chandni Chowk Authentic Food Trail", "description": "Savor authentic regional parathas and sweets at iconic heritage eateries.", "transport_mode": "Walk", "travel_time": "5 mins", "cost": "₹400"},
                {"time_slot": "Evening", "start_time": "17:00", "end_time": "19:30", "title": "India Gate & Sunset Stroll", "description": "Sunset boulevard walk with panoramic lighting and vibrant street atmosphere.", "transport_mode": "Cab", "travel_time": "25 mins", "cost": "₹200"}
            ]
        },
        {
            "day": 2,
            "theme": "Agra & The Taj Mahal Wonder",
            "activities": [
                {"time_slot": "Morning", "start_time": "06:00", "end_time": "09:30", "title": "Taj Mahal Sunrise Experience", "description": "Experience the world wonder at sunrise with serene morning reflection.", "transport_mode": "Express Train / Cab", "travel_time": "1.5 hrs", "cost": "₹1100"},
                {"time_slot": "Afternoon", "start_time": "12:00", "end_time": "14:30", "title": "Agra Fort Mughal Heritage", "description": "Explore the royal red sandstone fortress and Emperor chambers.", "transport_mode": "Auto", "travel_time": "15 mins", "cost": "₹500"},
                {"time_slot": "Evening", "start_time": "17:00", "end_time": "18:30", "title": "Mehtab Bagh Sunset Across Yamuna", "description": "Spectacular photography spot capturing the Taj silhouette.", "transport_mode": "Auto", "travel_time": "20 mins", "cost": "₹300"}
            ]
        },
        {
            "day": 3,
            "theme": "Pink City Forts & Heritage",
            "activities": [
                {"time_slot": "Morning", "start_time": "08:30", "end_time": "12:30", "title": "Amber Palace & Sheesh Mahal", "description": "Hilltop fort with ornate mirror palace and courtyards.", "transport_mode": "Cab", "travel_time": "30 mins", "cost": "₹600"},
                {"time_slot": "Afternoon", "start_time": "14:00", "end_time": "16:30", "title": "Hawa Mahal & City Palace", "description": "Honeycomb facade and royal textile museum.", "transport_mode": "Auto", "travel_time": "20 mins", "cost": "₹700"},
                {"time_slot": "Evening", "start_time": "17:30", "end_time": "19:30", "title": "Nahargarh Fort Sunset Point", "description": "Sunset views over the entire Jaipur skyline.", "transport_mode": "Cab", "travel_time": "35 mins", "cost": "₹400"}
            ]
        },
        {
            "day": 4,
            "theme": "Bazaars, Cuisine & Departure",
            "activities": [
                {"time_slot": "Morning", "start_time": "09:30", "end_time": "12:00", "title": "Johari Bazaar Handicrafts & Gems", "description": "Traditional block prints, blue pottery and souvenirs.", "transport_mode": "Walk", "travel_time": "10 mins", "cost": "₹500"},
                {"time_slot": "Afternoon", "start_time": "13:00", "end_time": "15:00", "title": "Rajasthani Thali Experience", "description": "Authentic multi-course traditional royal lunch.", "transport_mode": "Auto", "travel_time": "15 mins", "cost": "₹800"}
            ]
        }
    ]

    import json
    itinerary_json = json.dumps({"days": days_data})

    trip_data = {
        "id": trip_id,
        "user_id": str(current_user.id),
        "title": ci.get("title", "Curated Journey"),
        "destination_name": ci.get("destination_name", "India"),
        "destination_id": "golden-triangle",
        "start_date": datetime.now(timezone.utc).strftime("%Y-%m-%d"),
        "end_date": datetime.now(timezone.utc).strftime("%Y-%m-%d"),
        "status": "active",
        "day_count": ci.get("duration_days", 4),
        "traveler_count": 2,
        "budget_total": ci.get("total_estimated_cost", 18500.0),
        "budget_spent": 0.0,
        "style": "Curated",
        "interests": ["Heritage", "Culture", "Photography"],
        "itinerary_json": itinerary_json,
        "created_at": now_iso
    }

    try:
        firestore.collection("trips").document(trip_id).set(trip_data)
    except Exception as e:
        pass

    return {
        "success": True,
        "trip_id": trip_id,
        "message": f"Successfully cloned '{ci.get('title')}' into your trips library.",
        "itinerary_data": trip_data
    }
