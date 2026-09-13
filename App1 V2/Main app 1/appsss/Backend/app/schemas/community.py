from typing import Optional, List, Dict, Any
from pydantic import BaseModel

class CommunityPostCreate(BaseModel):
    forum_id: str
    title: str
    content: str
    post_type: str = "Tip"  # Question, Tip, Verified Recommendation

class CommunityCommentCreate(BaseModel):
    post_id: str
    content: str

class CommunityPostOut(BaseModel):
    id: str
    forum_id: str
    author_name: str
    author_avatar: str
    title: str
    content: str
    post_type: str
    likes_count: int
    comments_count: int
    is_verified_traveler: bool
    created_at: Any

class CommunityForumOut(BaseModel):
    id: str
    title: str
    slug: str
    description: str
    cover_image: str
    category: str
    member_count: int

class CreatorItineraryOut(BaseModel):
    id: str
    creator_name: str
    creator_avatar: str
    title: str
    destination_name: str
    duration_days: int
    total_estimated_cost: float
    price: float
    is_paid: bool
    purchases_count: int
    copy_count: int
    rating: float
    itinerary_data: Dict[str, Any]

class GroupVoteRequest(BaseModel):
    trip_id: str
    item_type: str  # Hotel, Attraction, Restaurant
    item_id: str
    vote: str  # Yes, No

class GroupExpenseSplitRequest(BaseModel):
    trip_id: str
    title: str
    total_amount: float
    paid_by_member: str
    split_members: List[str]
