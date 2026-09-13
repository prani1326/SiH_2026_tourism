from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field

class ReviewCreate(BaseModel):
    place_id: Optional[str] = None
    destination_id: Optional[str] = None
    target_id: Optional[str] = None
    target_type: Optional[str] = "destination"
    rating: float = Field(..., ge=1.0, le=5.0)
    title: Optional[str] = None
    comment: str
    cleanliness_rating: Optional[float] = 5.0
    punctuality_rating: Optional[float] = 5.0
    safety_rating: Optional[float] = 5.0

class ReviewOut(BaseModel):
    id: str
    user_id: str
    user_name: str
    user_avatar: str
    rating: float
    title: Optional[str] = None
    comment: str
    is_verified_booking: bool
    cleanliness_rating: float
    punctuality_rating: float
    safety_rating: float
    helpful_votes: int
    created_at: Any

class PlaceAttractionOut(BaseModel):
    id: str
    destination_id: str
    name: str
    category: str
    subcategory: Optional[str] = None
    cover_image: str
    description: str
    latitude: float
    longitude: float
    address: Optional[str] = None
    opening_hours: str
    visit_duration_minutes: int
    price: float
    rating: float
    reviews_count: int
    safety_score: int
    confidence_score: int
    confidence_breakdown: Dict[str, Any] = {}
    dietary_tags: List[str] = []
    diet_confidence: int = 95
    accessibility_friendly: bool
    family_friendly: bool
    solo_friendly: bool
    transport_options: List[Dict[str, Any]] = []

class DayPassBundleOut(BaseModel):
    id: str
    destination_id: str
    title: str
    description: str
    cover_image: str
    price: float
    original_price: float
    validity_days: int
    inclusions: List[str] = []

class DestinationOut(BaseModel):
    id: str
    name: str
    state: str
    country: str
    latitude: float
    longitude: float
    hero_image_url: str
    description: str
    known_for: Optional[str] = None
    rating: float
    best_time_to_visit: str
    ideal_stay: Optional[str] = None
    budget_per_day: Optional[str] = None
    estimated_budget_tier: str
    weather_temperature: str
    weather_condition: str
    safety_score: int
    tags: List[str] = []
    top_attractions: List[str] = []
    activities: List[str] = []
    famous_food: List[str] = []
    local_transport: List[str] = []
    nearby_places: List[str] = []
    is_featured: bool
    is_popular: bool

class DestinationDetailOut(DestinationOut):
    scam_alerts: List[Dict[str, Any]] = []
    local_etiquette: List[str] = []
    emergency_info: Dict[str, Any] = {}
    places: List[PlaceAttractionOut] = []
    day_passes: List[DayPassBundleOut] = []
    suggested_itineraries: List[Dict[str, Any]] = []

class GlobalSearchQuery(BaseModel):
    query: Optional[str] = None
    category: Optional[str] = None  # Attraction, Hotel, Restaurant, Activity, Event
    destination_id: Optional[str] = None
    near_lat: Optional[float] = None
    near_lon: Optional[float] = None
    max_distance_km: Optional[float] = 50.0
    min_price: Optional[float] = None
    max_price: Optional[float] = None
    min_rating: Optional[float] = None
    min_safety_score: Optional[int] = None
    min_confidence_score: Optional[int] = None
    dietary_filter: Optional[str] = None  # Vegetarian, Jain, Halal, Vegan
    accessibility_only: Optional[bool] = False
    family_friendly: Optional[bool] = False
    solo_friendly: Optional[bool] = False

class MenuScanRequest(BaseModel):
    image_base64: Optional[str] = None
    menu_text: Optional[str] = None
    user_diet: str = "Vegetarian"  # Vegetarian, Jain, Halal, Vegan, Nut-Allergy

class MenuScanResponse(BaseModel):
    overall_suitability: str  # Highly Suitable, Caution, Not Suitable
    diet_confidence: int
    detected_items: List[Dict[str, Any]]
    detected_risks: List[str]
    recommendations: List[str]
    total_items_analyzed: Optional[int] = 0
    suitable_items_count: Optional[int] = 0
    items: Optional[List[Dict[str, Any]]] = []
    warnings: Optional[List[str]] = []
