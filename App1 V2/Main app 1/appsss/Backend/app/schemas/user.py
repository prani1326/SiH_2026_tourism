from typing import Optional, List, Dict, Any
from pydantic import BaseModel, EmailStr

class UserProfileUpdate(BaseModel):
    full_name: Optional[str] = None
    avatar_url: Optional[str] = None
    bio: Optional[str] = None
    nationality: Optional[str] = None
    language: Optional[str] = None
    currency: Optional[str] = None
    walking_tolerance: Optional[str] = None  # Low, Moderate, High
    pace: Optional[str] = None  # Relaxed, Moderate, Fast

class TravelPreferencesUpdate(BaseModel):
    preferred_destinations: Optional[List[str]] = None
    budget_range: Optional[str] = None
    travel_styles: Optional[List[str]] = None  # Solo, Family, Couple, Group, Adventure, Luxury, Budget
    interests: Optional[List[str]] = None  # Heritage, Nature, Beaches, Mountains, Food, Shopping, Adventure, Culture, Spiritual, Wildlife
    dietary_preferences: Optional[List[str]] = None  # Vegetarian, Jain, Halal, Other, Vegan
    walking_tolerance: Optional[str] = None
    language: Optional[str] = None
    currency: Optional[str] = None
    family_requirements: Optional[Dict[str, Any]] = None

class EmergencyContactCreate(BaseModel):
    contact_name: str
    relationship_type: str = "Family"
    phone_number: str
    email: Optional[EmailStr] = None
    is_primary: bool = False

class EmergencyContactOut(EmergencyContactCreate):
    id: str

class SavedItemCreate(BaseModel):
    item_type: str  # destination, place, hotel, restaurant, activity, event
    item_id: str
    item_name: str
    image_url: Optional[str] = None
    notes: Optional[str] = None

class SavedItemOut(SavedItemCreate):
    id: str

class SavedCollectionCreate(BaseModel):
    name: str
    description: Optional[str] = None

class SavedCollectionOut(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    is_shared: bool
    share_token: Optional[str] = None
    items: List[SavedItemOut] = []

class UserOut(BaseModel):
    id: str
    email: Optional[str] = None
    phone: Optional[str] = None
    full_name: str
    is_verified: bool
    role: str
    profile: Optional[Dict[str, Any]] = None
    preferences: Optional[Dict[str, Any]] = None
    emergency_contacts: List[EmergencyContactOut] = []
