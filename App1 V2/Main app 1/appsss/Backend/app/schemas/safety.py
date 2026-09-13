from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field

class SOSAlertRequest(BaseModel):
    latitude: float = Field(..., examples=[27.1751])
    longitude: float = Field(..., examples=[78.0421])
    address: Optional[str] = "Near Taj Mahal East Gate, Agra"
    trip_id: Optional[str] = None
    emergency_type: str = Field("General", examples=["Medical, Crime, Harassment, Lost, Accident"])

class SOSAlertResponse(BaseModel):
    alert_id: str
    status: str
    latitude: float
    longitude: float
    emergency_services: Dict[str, str]
    nearest_police: Dict[str, Any]
    nearest_hospital: Dict[str, Any]
    contacts_notified: List[Dict[str, str]]
    live_tracking_url: str
    instructions: List[str]

class LostPhoneRecoverRequest(BaseModel):
    email: str
    emergency_recovery_pin: str

class LostPhoneRecoverResponse(BaseModel):
    recovery_token: str
    user_name: str
    email: str
    phone: Optional[str]
    is_frozen: bool
    active_trips: List[Dict[str, Any]]
    confirmed_bookings: List[Dict[str, Any]]
    hotel_contacts: List[Dict[str, Any]]
    emergency_contacts: List[Dict[str, Any]]
    embassy_info: Dict[str, str]
    action_options: List[str]

class SeparatedGroupCreate(BaseModel):
    trip_id: Optional[str] = None
    group_name: str = Field(..., examples=["Family Rajasthan Tour"])
    rendezvous_meeting_point: str = Field("Gate 2 Tourist Info Center", examples=["Gate 2 Tourist Info Center"])
    geofence_lat: Optional[float] = 26.9124
    geofence_lon: Optional[float] = 75.7873
    geofence_radius_meters: float = 500.0
    checkin_interval_minutes: int = 60

class GroupMemberCheckin(BaseModel):
    group_id: str
    member_name: str
    latitude: float
    longitude: float
    status: str = Field("Safe", examples=["Safe, Separated, Need_Assistance"])

class GroupStatusResponse(BaseModel):
    group_id: str
    group_name: str
    rendezvous_meeting_point: str
    members: List[Dict[str, Any]]
    geofence_active: bool
    alerts: List[str]

class DeviceLocationSchema(BaseModel):
    latitude: float = 0.0
    longitude: float = 0.0
    accuracy: float = 0.0
    address: Optional[str] = None
    timestamp: int = 0
    is_live: bool = True

class DeviceHeartbeatRequest(BaseModel):
    device_id: str
    battery_level: int = 100
    is_charging: bool = False
    network_status: str = "ONLINE"
    location: Optional[DeviceLocationSchema] = None
    app_activity: str = "FOREGROUND"
    timestamp: int = 0

class HeartbeatResponse(BaseModel):
    success: bool = True
    status: str = "ACTIVE"
    device_state: str = "ACTIVE"
    pending_escalations: int = 0
    server_timestamp: int
    message: str = "Heartbeat recorded"

class ImSafeRequest(BaseModel):
    alert_id: Optional[str] = None
    notes: Optional[str] = "Traveler confirmed safe"
    timestamp: Optional[int] = None

class NeedHelpRequest(BaseModel):
    trip_id: Optional[str] = None
    urgency: str = "HIGH"
    details: str = "Traveler requested immediate assistance"
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    timestamp: Optional[int] = None

class SafetyActionResponse(BaseModel):
    success: bool = True
    event_id: Optional[str] = None
    status: str = "SUCCESS"
    message: str = "Operation completed"
    server_timestamp: int
