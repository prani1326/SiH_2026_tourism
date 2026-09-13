import uuid
from datetime import datetime, timezone
from typing import List, Dict, Any
from fastapi import APIRouter, Depends
from app.core.firestore_db import get_firestore
from app.core.security import get_current_user
from app.models.firestore_models import User
from app.schemas.community import GroupVoteRequest, GroupExpenseSplitRequest

router = APIRouter()

@router.post("/vote")
def cast_group_vote(
    request: GroupVoteRequest,
    current_user: User = Depends(get_current_user)
):
    """Group Planning & Collaboration: Place/Hotel Voting in Firestore (Section 35)."""
    firestore = get_firestore()
    vote_doc_id = f"{request.trip_id}_{request.item_id}"
    doc_ref = firestore.collection("group_votes").document(vote_doc_id)
    doc = doc_ref.get()

    if doc.exists:
        data = doc.to_dict()
    else:
        data = {
            "trip_id": request.trip_id,
            "item_id": request.item_id,
            "yes": 0,
            "no": 0,
            "voters": []
        }

    voter_id = str(current_user.id)
    if voter_id not in data.get("voters", []):
        if request.vote.lower() == "yes":
            data["yes"] = data.get("yes", 0) + 1
        else:
            data["no"] = data.get("no", 0) + 1
        data["voters"].append(voter_id)
        data["updated_at"] = datetime.now(timezone.utc).isoformat()
        doc_ref.set(data)

    return {
        "success": True,
        "item_id": request.item_id,
        "current_tally": {
            "yes_votes": data.get("yes", 0),
            "no_votes": data.get("no", 0)
        },
        "message": "Vote recorded successfully in Firestore."
    }

@router.post("/split-expense")
def split_group_expense(request: GroupExpenseSplitRequest):
    """Shared Budget & Group Expense Splitting with Firestore (Section 35)."""
    firestore = get_firestore()
    num_members = max(1, len(request.split_members))
    share_per_person = round(request.total_amount / num_members, 2)

    breakdown = [
        {
            "member": member,
            "owes_to": request.paid_by_member if member != request.paid_by_member else "Paid in Full",
            "amount": 0.0 if member == request.paid_by_member else share_per_person
        }
        for member in request.split_members
    ]

    expense_id = str(uuid.uuid4())
    expense_data = {
        "id": expense_id,
        "expense_title": request.title,
        "total_amount": request.total_amount,
        "paid_by": request.paid_by_member,
        "per_person_share": share_per_person,
        "breakdown": breakdown,
        "created_at": datetime.now(timezone.utc).isoformat()
    }
    firestore.collection("group_expenses").document(expense_id).set(expense_data)

    return {
        "expense_id": expense_id,
        "expense_title": request.title,
        "total_amount": request.total_amount,
        "paid_by": request.paid_by_member,
        "per_person_share": share_per_person,
        "breakdown": breakdown
    }
