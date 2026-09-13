from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field

class AITripPlanRequest(BaseModel):
    destination: str = Field(..., examples=["Goa"])
    destination_id: Optional[str] = None
    duration_days: Optional[int] = Field(3, ge=1)
    start_date: str = Field(..., examples=["2026-10-10"])
    end_date: str = Field(..., examples=["2026-10-15"])
    traveler_count: int = Field(2, ge=1)
    budget: float = Field(25000.0, description="Total budget in INR or preferred currency")
    currency: str = "INR"
    travel_style: str = Field("Couple", examples=["Solo, Couple, Family, Group, Adventure, Luxury, Budget"])
    interests: List[str] = Field(default_factory=lambda: ["Beaches", "Food", "Heritage"])
    activities: List[str] = Field(default_factory=lambda: ["Water Sports", "Sightseeing"])
    must_visit_places: Optional[List[str]] = None
    food_preference: str = Field("Local Food", examples=["Local Food, Vegetarian, Non-Vegetarian, Jain, Street Food"])
    hotel_preference: str = Field("3 Star", examples=["Budget, 3 Star, 4 Star, 5 Star, Heritage"])
    transport_preference: str = Field("Cab", examples=["Cab, Self-Drive, Metro, Walking"])
    travel_pace: str = Field("Balanced", examples=["Relaxed, Balanced, Fast-Paced"])
    walking_tolerance: str = Field("Moderate", examples=["Low, Moderate, High"])
    pace: str = Field("Balanced", examples=["Relaxed, Balanced, Fast-Paced"])
    pacing: Optional[str] = "Balanced"
    group_type: Optional[str] = "Couple"
    family_requirements: Optional[Dict[str, Any]] = None

class TripCreateRequest(BaseModel):
    title: str = "My Trip"
    destination_id: Optional[str] = None
    start_date: str = "2026-10-15"
    end_date: str = "2026-10-18"
    traveler_count: int = 1
    travel_style: str = "Solo"
    total_budget: float = 30000.0

class TripActivityCreate(BaseModel):
    trip_day_id: str
    place_id: Optional[str] = None
    time_slot: str = "Morning"
    title: str
    description: Optional[str] = None
    start_time: str = "09:00 AM"
    end_time: str = "11:30 AM"
    estimated_cost: float = 0.0
    travel_time_minutes: int = 20
    is_outdoor: bool = True
    transport_mode: str = "Taxi"

class TripActivityUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    estimated_cost: Optional[float] = None
    transport_mode: Optional[str] = None
    time_slot: Optional[str] = None
    is_completed: Optional[bool] = None

class ReorderActivitiesRequest(BaseModel):
    trip_day_id: str
    activity_ids: List[str]

class DisruptionReplanRequest(BaseModel):
    trip_id: str
    disruption_type: str = "Rain"
    disruption_details: Optional[str] = None
    affected_activity_id: Optional[str] = None
    current_day: Optional[int] = 1

class TrueTripCostResponse(BaseModel):
    trip_id: str
    currency: str = "INR"
    planned_budget: float
    total_estimated_cost: float
    actual_spend: float
    drift_amount: float
    drift_percent: float
    drift_status: str
    budget_drift_alert: str
    breakdown: Dict[str, float]
    cost_saving_suggestions: List[str]

class TripActivityOut(BaseModel):
    id: str
    trip_day_id: str
    place_id: Optional[str] = None
    time_slot: str
    sequence_order: int = 1
    title: str
    description: Optional[str] = None
    start_time: str
    end_time: str
    estimated_cost: float = 0.0
    travel_time_minutes: int = 15
    is_outdoor: bool = False
    is_completed: bool = False
    transport_mode: str = "Walking"

class TripDayOut(BaseModel):
    id: str
    day_number: int
    date: str
    weather_summary: Optional[str] = None
    notes: Optional[str] = None
    activities: List[TripActivityOut] = Field(default_factory=list)

class TripOut(BaseModel):
    id: str
    title: str
    destination_id: Optional[str] = None
    destination_name: Optional[str] = None
    start_date: str
    end_date: str
    traveler_count: int = 1
    travel_style: str = "Couple"
    total_budget: float = 0.0
    total_estimated_cost: float = 0.0
    actual_spend: float = 0.0
    budget_drift_percent: float = 0.0
    status: str = "planning"
    share_token: Optional[str] = None
    days: List[TripDayOut] = Field(default_factory=list)

class DigitalTripCardOut(BaseModel):
    card_id: str
    trip_id: str
    tourist_name: str
    destination_name: str
    start_date: str
    end_date: str
    hotel_info: Dict[str, Any]
    active_booking_refs: List[str]
    emergency_contacts: List[Dict[str, str]]
    embassy_info: Dict[str, str]
    insurance_policy: Dict[str, str]
    qr_code_base64: str
    offline_sync_token: str
    support_helpline: str

class TripShareRequest(BaseModel):
    shared_with_email: Optional[str] = None
    shared_with_phone: Optional[str] = None
    role: str = "viewer"

class ExpenseSplitCreate(BaseModel):
    description: str
    amount: float
    paid_by_user_id: str
    split_type: str = "EQUAL"
    split_details: Dict[str, float] = Field(default_factory=dict)
