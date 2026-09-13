import uuid
import secrets
from datetime import datetime, timezone
from typing import List
from fastapi import APIRouter, Depends, HTTPException
from app.core.firestore_db import get_firestore
from app.core.security import get_current_user, get_current_user_optional
from app.models.firestore_models import User
from app.schemas.user import (
    SavedCollectionCreate, SavedCollectionOut, SavedItemCreate, SavedItemOut
)

router = APIRouter()

@router.get("/", response_model=List[SavedCollectionOut])
def get_user_collections(current_user: User = Depends(get_current_user)):
    """Saved Collections: Wishlist, Plan Later, Favorites in Firestore (Section 36)."""
    firestore = get_firestore()
    docs = firestore.collection("saved_collections").where("user_id", "==", str(current_user.id)).get()

    results = []
    if not docs:
        # Auto-initialize default collections
        defaults = [
            {"name": "Wishlist", "description": "Dream destinations to visit"},
            {"name": "Plan Later", "description": "Saved places for upcoming itineraries"},
            {"name": "Favorites", "description": "Loved hotels and restaurants"}
        ]
        now_iso = datetime.now(timezone.utc).isoformat()
        for d in defaults:
            cid = str(uuid.uuid4())
            data = {
                "id": cid,
                "user_id": str(current_user.id),
                "name": d["name"],
                "description": d["description"],
                "is_shared": False,
                "share_token": secrets.token_urlsafe(16),
                "items": [],
                "created_at": now_iso,
                "updated_at": now_iso
            }
            firestore.collection("saved_collections").document(cid).set(data)
            results.append({
                "id": cid,
                "name": data["name"],
                "description": data["description"],
                "is_shared": False,
                "share_token": data["share_token"],
                "items": []
            })
    else:
        for doc in docs:
            data = doc.to_dict()
            results.append({
                "id": doc.id,
                "name": data.get("name", "Collection"),
                "description": data.get("description", ""),
                "is_shared": data.get("is_shared", False),
                "share_token": data.get("share_token", ""),
                "items": data.get("items", [])
            })

    return results

@router.post("/", response_model=SavedCollectionOut)
def create_collection(
    request: SavedCollectionCreate,
    current_user: User = Depends(get_current_user)
):
    """Create Custom Saved Collection in Firestore (Section 36)."""
    firestore = get_firestore()
    cid = str(uuid.uuid4())
    now_iso = datetime.now(timezone.utc).isoformat()
    data = {
        "id": cid,
        "user_id": str(current_user.id),
        "name": request.name,
        "description": request.description,
        "is_shared": False,
        "share_token": secrets.token_urlsafe(16),
        "items": [],
        "created_at": now_iso,
        "updated_at": now_iso
    }
    firestore.collection("saved_collections").document(cid).set(data)
    return {
        "id": cid,
        "name": data["name"],
        "description": data["description"],
        "is_shared": data["is_shared"],
        "share_token": data["share_token"],
        "items": []
    }

@router.post("/{collection_id}/items", response_model=SavedItemOut)
def add_item_to_collection(
    collection_id: str,
    request: SavedItemCreate,
    current_user: User = Depends(get_current_user)
):
    """Save Destination, Hotel, or Place to Collection in Firestore (Section 36)."""
    firestore = get_firestore()
    doc_ref = firestore.collection("saved_collections").document(collection_id)
    doc = doc_ref.get()
    if not doc.exists or doc.to_dict().get("user_id") != str(current_user.id):
        raise HTTPException(status_code=404, detail="Collection not found.")

    coll_data = doc.to_dict()
    items = coll_data.get("items", [])
    item_id = str(uuid.uuid4())
    new_item = {
        "id": item_id,
        "item_type": request.item_type,
        "item_id": request.item_id,
        "item_name": request.item_name,
        "image_url": request.image_url,
        "notes": request.notes
    }
    items.append(new_item)
    doc_ref.update({"items": items, "updated_at": datetime.now(timezone.utc).isoformat()})

    return new_item

@router.delete("/items/{item_id}")
def remove_item(item_id: str, current_user: Optional[User] = Depends(get_current_user_optional)):
    """Remove item from collection in Firestore."""
    firestore = get_firestore()
    if current_user:
        docs = firestore.collection("saved_collections").where("user_id", "==", str(current_user.id)).get()
    else:
        docs = firestore.collection("saved_collections").get()

    found = False
    for doc in docs:
        coll_data = doc.to_dict()
        items = coll_data.get("items", [])
        original_len = len(items)
        items = [it for it in items if it.get("id") != item_id]
        if len(items) < original_len:
            firestore.collection("saved_collections").document(doc.id).update({
                "items": items,
                "updated_at": datetime.now(timezone.utc).isoformat()
            })
            found = True
            break

    if not found:
        raise HTTPException(status_code=404, detail="Saved item not found.")

    return {"success": True, "message": "Item removed from collection."}
