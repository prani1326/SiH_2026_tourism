from fastapi import APIRouter, Query
from typing import Optional
from app.services.crowd_intelligence_service import crowd_intelligence_service

router = APIRouter()

@router.get("/monument")
async def get_monument_crowd(
    name: str = Query("Amber Fort", description="Monument or attraction name")
):
    return crowd_intelligence_service.get_crowd_intelligence_for_monument(monument_name=name)

@router.get("/destination/{destination_id}")
async def get_destination_crowd(destination_id: str):
    return crowd_intelligence_service.get_crowd_intelligence_for_monument(monument_name=destination_id)
