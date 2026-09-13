from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field

class BudgetBreakdownSchema(BaseModel):
    hotel: float = Field(0.0, ge=0, description="Estimated accommodation cost")
    food: float = Field(0.0, ge=0, description="Estimated meals and dining cost")
    transport: float = Field(0.0, ge=0, description="Estimated local transport and cabs cost")
    activities: float = Field(0.0, ge=0, description="Estimated entry fees and activity tickets cost")
    miscellaneous: float = Field(0.0, ge=0, description="Estimated shopping and buffer cost")

class GeneratedActivitySchema(BaseModel):
    time_slot: str = Field(..., description="Morning, Afternoon, Evening, or Night")
    title: str
    description: str
    place_name: str
    place_id: Optional[str] = None
    activity_type: str = Field("sightseeing", description="place, activity, restaurant, hotel, or transit")
    start_time: str
    end_time: str
    estimated_cost: float = Field(0.0, ge=0)
    travel_time_minutes: int = Field(15, ge=0)
    is_outdoor: bool = False
    transport_mode: str = "Cab"
    dietary_tags: List[str] = Field(default_factory=list)

class GeneratedDaySchema(BaseModel):
    day_number: int = Field(..., ge=1)
    date: str
    title: str = "Day Exploration"
    theme: str
    weather_summary: str = "Pleasant and clear"
    notes: Optional[str] = None
    activities: List[GeneratedActivitySchema] = Field(default_factory=list)
    estimated_day_cost: float = Field(0.0, ge=0)
    alternative_activity: Optional[Dict[str, Any]] = Field(default_factory=dict)

class GeneratedItineraryPlan(BaseModel):
    title: str
    trip_title: Optional[str] = None
    destination: str
    destination_id: Optional[str] = None
    destination_name: Optional[str] = None
    duration_days: int = Field(..., ge=1)
    travelers: int = Field(2, ge=1)
    total_estimated_cost: float = Field(..., ge=0)
    currency: str = "INR"
    days: List[GeneratedDaySchema]
    budget_breakdown: BudgetBreakdownSchema = Field(default_factory=BudgetBreakdownSchema)
    highlights: List[str] = Field(default_factory=list)
    safety_notes: List[str] = Field(default_factory=list)
