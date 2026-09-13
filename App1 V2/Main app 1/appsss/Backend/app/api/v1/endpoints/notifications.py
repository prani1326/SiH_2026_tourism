from typing import List
from fastapi import APIRouter, Depends, HTTPException
from app.core.firestore_db import get_firestore
from app.core.security import get_current_user
from app.models.firestore_models import User
from app.schemas.support import NotificationOut

router = APIRouter()

@router.get("/", response_model=List[NotificationOut])
def get_notifications(
    current_user: User = Depends(get_current_user)
):
    """Notification Center with Firestore (Section 41)."""
    firestore = get_firestore()
    docs = (
        firestore.collection("notifications")
        .where("user_id", "==", str(current_user.id))
        .get()
    )
    
    notifs = [d.to_dict() for d in docs]
    # Sort descending by created_at
    notifs.sort(key=lambda n: n.get("created_at", ""), reverse=True)

    return [
        {
            "id": n.get("id"),
            "title": n.get("title", ""),
            "message": n.get("message", ""),
            "category": n.get("category", "Booking"),
            "is_read": n.get("is_read", False),
            "deep_link": n.get("deep_link"),
            "created_at": n.get("created_at")
        } for n in notifs
    ]

@router.put("/{notification_id}/read")
def mark_notification_read(
    notification_id: str,
    current_user: User = Depends(get_current_user)
):
    """Mark Notification as Read in Firestore (Section 41)."""
    firestore = get_firestore()
    doc_ref = firestore.collection("notifications").document(notification_id)
    doc = doc_ref.get()
    if not doc.exists:
        raise HTTPException(status_code=404, detail="Notification not found.")

    doc_ref.update({"is_read": True})
    return {"success": True, "message": "Notification marked as read in Firestore."}
