from typing import Optional, List, Dict, Any
from pydantic import BaseModel

class SupportTicketCreate(BaseModel):
    category: str = "General"  # Booking, Payment, Refund, Cancellation, Hotel, Transport, Safety, Lost Phone, Lost Person, General
    subject: str
    message: str
    priority: str = "Medium"  # Low, Medium, High, Emergency
    booking_id: Optional[str] = None

class TicketMessageCreate(BaseModel):
    ticket_id: str
    message: str

class TicketMessageOut(BaseModel):
    id: str
    sender_type: str  # user, agent, system
    sender_name: str
    message: str
    created_at: Any

class SupportTicketOut(BaseModel):
    id: str
    category: str
    subject: str
    status: str  # Open, In_Progress, Resolved, Closed
    priority: str
    booking_id: Optional[str] = None
    created_at: Any
    messages: List[TicketMessageOut] = []

class NotificationOut(BaseModel):
    id: str
    title: str
    message: str
    category: str
    is_read: bool
    deep_link: Optional[str] = None
    created_at: Any
