from fastapi import APIRouter, Query
from typing import Optional
from app.services.local_experience_service import local_experience_service

router = APIRouter()

@router.get("/")
@router.get("")
async def list_local_experiences(
    destination: Optional[str] = Query(None),
    category: Optional[str] = Query(None)
):
    return local_experience_service.get_experiences(destination=destination, category=category)

@router.get("/{experience_id}")
async def get_experience_detail(experience_id: str):
    return local_experience_service.get_experience_by_id(experience_id)
