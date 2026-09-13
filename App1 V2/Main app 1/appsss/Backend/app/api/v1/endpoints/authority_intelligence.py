from fastapi import APIRouter
from app.services.tourism_authority_service import tourism_authority_service

router = APIRouter()

@router.get("/crowd")
async def get_authority_crowd_overview():
    return tourism_authority_service.get_crowd_intelligence_overview()

@router.get("/safety")
async def get_authority_safety_overview():
    return tourism_authority_service.get_safety_intelligence_overview()

@router.get("/trends")
async def get_authority_trends():
    return tourism_authority_service.get_tourism_trends_and_sustainability()
