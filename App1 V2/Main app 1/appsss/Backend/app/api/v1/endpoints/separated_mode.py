import uuid
from datetime import datetime, timezone
from typing import List
from fastapi import APIRouter, Depends, HTTPException
from app.core.firestore_db import get_firestore
from app.core.security import get_current_user
from app.models.firestore_models import User
from app.schemas.safety import (
    SeparatedGroupCreate, GroupMemberCheckin, GroupStatusResponse
)

router = APIRouter()

@router.post("/groups", response_model=GroupStatusResponse)
def create_separated_group(
    request: SeparatedGroupCreate,
    current_user: User = Depends(get_current_user)
):
    """
    Setup Lost Person / Separated Group Mode with Firestore (Section 20).
    Configures rendezvous meeting points (e.g. 'Gate 2') and geofencing.
    """
    firestore = get_firestore()
    group_id = str(uuid.uuid4())
    member_id = str(uuid.uuid4())
    now_iso = datetime.now(timezone.utc).isoformat()

    admin_member = {
        "id": member_id,
        "user_id": str(current_user.id),
        "name": current_user.full_name or "Traveler",
        "phone": current_user.phone,
        "status": "Safe",
        "last_checkin": now_iso,
        "lat": request.geofence_lat,
        "lon": request.geofence_lon
    }

    group_data = {
        "id": group_id,
        "trip_id": request.trip_id,
        "group_name": request.group_name,
        "admin_user_id": str(current_user.id),
        "rendezvous_meeting_point": request.rendezvous_meeting_point,
        "geofence_lat": request.geofence_lat,
        "geofence_lon": request.geofence_lon,
        "geofence_radius_meters": request.geofence_radius_meters,
        "checkin_interval_minutes": request.checkin_interval_minutes,
        "is_active": True,
        "members": [admin_member],
        "created_at": now_iso,
        "updated_at": now_iso
    }

    firestore.collection("separated_groups").document(group_id).set(group_data)

    return {
        "group_id": group_id,
        "group_name": group_data["group_name"],
        "rendezvous_meeting_point": group_data["rendezvous_meeting_point"],
        "members": [{
            "id": admin_member["id"],
            "name": admin_member["name"],
            "status": admin_member["status"],
            "last_checkin": admin_member["last_checkin"]
        }],
        "geofence_active": True,
        "alerts": []
    }

@router.post("/checkin")
def member_checkin(request: GroupMemberCheckin):
    """Member 'I'm Safe' Check-in or Separated Alert in Firestore (Section 20)."""
    firestore = get_firestore()
    doc_ref = firestore.collection("separated_groups").document(request.group_id)
    doc = doc_ref.get()
    if not doc.exists:
        raise HTTPException(status_code=404, detail="Separated group not found.")

    group_data = doc.to_dict()
    members = group_data.get("members", [])
    now_iso = datetime.now(timezone.utc).isoformat()

    found = False
    for m in members:
        if m.get("name") == request.member_name:
            m["status"] = request.status
            m["lat"] = request.latitude
            m["lon"] = request.longitude
            m["last_checkin"] = now_iso
            found = True
            break

    if not found:
        members.append({
            "id": str(uuid.uuid4()),
            "user_id": f"guest_{abs(hash(request.member_name)) % 10000}",
            "name": request.member_name,
            "status": request.status,
            "lat": request.latitude,
            "lon": request.longitude,
            "last_checkin": now_iso
        })

    doc_ref.update({"members": members, "updated_at": now_iso})

    return {
        "success": True,
        "status": request.status,
        "rendezvous_meeting_point": group_data.get("rendezvous_meeting_point"),
        "message": f"Check-in received for {request.member_name}."
    }

@router.get("/groups/{group_id}/status", response_model=GroupStatusResponse)
def get_group_status(group_id: str):
    """Group Member Locations & Separated Alerts from Firestore (Section 20)."""
    firestore = get_firestore()
    doc = firestore.collection("separated_groups").document(group_id).get()
    if not doc.exists:
        raise HTTPException(status_code=404, detail="Group not found.")

    group = doc.to_dict()
    members = group.get("members", [])
    member_data = []
    alerts = []

    for m in members:
        member_data.append({
            "id": m.get("id"),
            "name": m.get("name"),
            "status": m.get("status", "Safe"),
            "last_checkin": m.get("last_checkin"),
            "lat": m.get("lat"),
            "lon": m.get("lon")
        })
        if m.get("status") != "Safe":
            alerts.append(f"ALERT: {m.get('name')} marked as {m.get('status')}! Please regroup at {group.get('rendezvous_meeting_point')}.")

    return {
        "group_id": group_id,
        "group_name": group.get("group_name", "Travel Group"),
        "rendezvous_meeting_point": group.get("rendezvous_meeting_point", "Meeting Point"),
        "members": member_data,
        "geofence_active": group.get("is_active", True),
        "alerts": alerts
    }
