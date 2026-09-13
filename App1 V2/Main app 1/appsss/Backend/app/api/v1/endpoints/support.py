import uuid
from datetime import datetime, timezone
from typing import List
from fastapi import APIRouter, Depends, HTTPException
from app.core.firestore_db import get_firestore
from app.core.security import get_current_user
from app.models.firestore_models import User
from app.schemas.support import (
    SupportTicketCreate, SupportTicketOut, TicketMessageCreate, TicketMessageOut
)

router = APIRouter()

@router.get("/tickets", response_model=List[SupportTicketOut])
def get_user_tickets(
    current_user: User = Depends(get_current_user)
):
    """24/7 Support Tickets from Firestore (Section 40)."""
    firestore = get_firestore()
    docs = firestore.collection("support_tickets").where("user_id", "==", str(current_user.id)).get()

    tickets = [d.to_dict() for d in docs]
    tickets.sort(key=lambda t: t.get("created_at", ""), reverse=True)

    results = []
    for t in tickets:
        msgs = [
            {
                "id": m.get("id"),
                "sender_type": m.get("sender_type", "user"),
                "sender_name": m.get("sender_name", "Traveler"),
                "message": m.get("message", ""),
                "created_at": m.get("created_at")
            } for m in t.get("messages", [])
        ]
        results.append({
            "id": t.get("id"),
            "category": t.get("category", "General"),
            "subject": t.get("subject", ""),
            "status": t.get("status", "Open"),
            "priority": t.get("priority", "Medium"),
            "booking_id": t.get("booking_id"),
            "created_at": t.get("created_at"),
            "messages": msgs
        })
    return results

@router.post("/tickets", response_model=SupportTicketOut)
def create_support_ticket(
    request: SupportTicketCreate,
    current_user: User = Depends(get_current_user)
):
    """Open Support Ticket / Dispute in Firestore (Section 40)."""
    ticket_id = str(uuid.uuid4())
    now_iso = datetime.now(timezone.utc).isoformat()

    msg_id = str(uuid.uuid4())
    msg = {
        "id": msg_id,
        "ticket_id": ticket_id,
        "sender_type": "user",
        "sender_name": current_user.full_name or "Traveler",
        "message": request.message,
        "created_at": now_iso
    }

    auto_reply_id = str(uuid.uuid4())
    auto_reply = {
        "id": auto_reply_id,
        "ticket_id": ticket_id,
        "sender_type": "agent",
        "sender_name": "24/7 Travel Concierge AI",
        "message": "Thank you for contacting Support. An official travel operations agent has been assigned to your ticket.",
        "created_at": now_iso
    }

    ticket_data = {
        "id": ticket_id,
        "user_id": str(current_user.id),
        "category": request.category,
        "subject": request.subject,
        "priority": request.priority,
        "booking_id": request.booking_id,
        "status": "Open",
        "messages": [msg, auto_reply],
        "created_at": now_iso,
        "updated_at": now_iso
    }

    firestore = get_firestore()
    firestore.collection("support_tickets").document(ticket_id).set(ticket_data)

    return {
        "id": ticket_id,
        "category": ticket_data["category"],
        "subject": ticket_data["subject"],
        "status": ticket_data["status"],
        "priority": ticket_data["priority"],
        "booking_id": ticket_data["booking_id"],
        "created_at": ticket_data["created_at"],
        "messages": ticket_data["messages"]
    }

@router.post("/tickets/{ticket_id}/messages", response_model=TicketMessageOut)
def reply_to_ticket(
    ticket_id: str,
    request: TicketMessageCreate,
    current_user: User = Depends(get_current_user)
):
    """Live Chat Message Stream in Firestore (Section 40)."""
    firestore = get_firestore()
    doc_ref = firestore.collection("support_tickets").document(ticket_id)
    doc = doc_ref.get()
    if not doc.exists:
        raise HTTPException(status_code=404, detail="Support ticket not found.")

    ticket_data = doc.to_dict()
    now_iso = datetime.now(timezone.utc).isoformat()
    msg_id = str(uuid.uuid4())

    new_msg = {
        "id": msg_id,
        "ticket_id": ticket_id,
        "sender_type": "user",
        "sender_name": current_user.full_name or "Traveler",
        "message": request.message,
        "created_at": now_iso
    }

    messages = ticket_data.get("messages", [])
    messages.append(new_msg)
    doc_ref.update({"messages": messages, "updated_at": now_iso})

    return {
        "id": msg_id,
        "sender_type": new_msg["sender_type"],
        "sender_name": new_msg["sender_name"],
        "message": new_msg["message"],
        "created_at": new_msg["created_at"]
    }
