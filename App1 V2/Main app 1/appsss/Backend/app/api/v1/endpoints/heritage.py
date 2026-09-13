from fastapi import APIRouter, Query
from typing import Optional
from pydantic import BaseModel
from app.services.heritage_service import heritage_service

router = APIRouter()

class HeritageAnalyzeRequest(BaseModel):
    image_base64: Optional[str] = None
    monument_hint: Optional[str] = "Amber Fort"

@router.post("/analyze")
async def analyze_heritage(request: HeritageAnalyzeRequest):
    return heritage_service.analyze_monument_image(
        image_base64=request.image_base64,
        monument_hint=request.monument_hint
    )

@router.get("/{monument_id}")
async def get_monument_details(monument_id: str):
    return heritage_service.get_monument_details(monument_id)
