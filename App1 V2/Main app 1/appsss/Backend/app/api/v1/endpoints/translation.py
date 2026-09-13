from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional

router = APIRouter()

class TranslationRequest(BaseModel):
    text: str
    target_language: str
    source_language: Optional[str] = "en"

class TranslationResponse(BaseModel):
    original_text: str
    translated_text: str
    target_language: str

@router.post("/text", response_model=TranslationResponse)
async def translate_text(request: TranslationRequest):
    return TranslationResponse(
        original_text=request.text,
        translated_text=f"[Translated to {request.target_language}]: {request.text}",
        target_language=request.target_language
    )
