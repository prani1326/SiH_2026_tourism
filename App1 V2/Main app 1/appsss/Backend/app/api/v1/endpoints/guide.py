from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status, Request
from app.core.security import get_current_user
from app.models.firestore_models import User
from app.schemas.guide import (
    ImageTranslateRequest, ImageTranslateResponse,
    PlaceDetectRequest, PlaceDetectResponse,
    VoiceTranslateRequest, VoiceTranslateResponse,
    PlaceInfoRequest, PlaceInfoResponse,
    TranslationSaveRequest, SavedPlaceRequest,
    ConversationSaveRequest
)
from app.services.guide_service import guide_service

router = APIRouter()

# =============================================================================
# 1. Camera Translation
# =============================================================================
@router.post("/translate-image", response_model=ImageTranslateResponse)
async def translate_image_endpoint(
    request: ImageTranslateRequest,
    current_user: Optional[User] = Depends(get_current_user)
):
    """
    Process camera image, run OCR, detect source language, and translate into target language.
    """
    if not request.image:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Image payload cannot be empty."
        )

    result = await guide_service.translate_image(
        image_data=request.image,
        target_lang=request.targetLanguage,
        source_lang=request.sourceLanguage or "auto"
    )

    if not result.get("success"):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=result.get("errorMessage", "Failed to process image translation.")
        )

    return ImageTranslateResponse(**result)

# =============================================================================
# 2. AI Place Detection
# =============================================================================
@router.post("/detect-place", response_model=PlaceDetectResponse)
async def detect_place_endpoint(
    request: PlaceDetectRequest,
    current_user: Optional[User] = Depends(get_current_user)
):
    """
    Identify landmark or tourist site using visual recognition & GPS context.
    """
    result = await guide_service.detect_place(
        image_data=request.image,
        latitude=request.latitude,
        longitude=request.longitude,
        hint=request.hint
    )

    if not result.get("success"):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=result.get("errorMessage", "Could not identify landmark.")
        )

    return PlaceDetectResponse(**result)

# =============================================================================
# 3. Live Voice Translation
# =============================================================================
@router.post("/voice/translate", response_model=VoiceTranslateResponse)
async def translate_voice_endpoint(
    request: VoiceTranslateRequest,
    current_user: Optional[User] = Depends(get_current_user)
):
    """
    Live voice translation: Speech recognition -> language detection -> translation.
    """
    result = await guide_service.translate_voice(
        audio_data=request.audio,
        text_data=request.text,
        source_lang=request.sourceLanguage,
        target_lang=request.targetLanguage
    )

    if not result.get("success"):
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=result.get("errorMessage", "Voice translation failed.")
        )

    return VoiceTranslateResponse(**result)

# =============================================================================
# 4. AI Travel Guide / Destination Information
# =============================================================================
@router.post("/place-info", response_model=PlaceInfoResponse)
async def get_place_info_endpoint(
    request: PlaceInfoRequest,
    current_user: Optional[User] = Depends(get_current_user)
):
    """
    Retrieve grounded travel guide insights (history, culture, foods, attractions, tips).
    """
    if not request.placeName:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Place name is required."
        )

    result = await guide_service.get_place_info(
        place_name=request.placeName,
        place_id=request.placeId,
        user_lang=request.userLanguage or "en"
    )

    return PlaceInfoResponse(**result)

# =============================================================================
# 5. Saved Translations & History (Firestore Persistence)
# =============================================================================
@router.get("/translations")
async def get_user_translations_endpoint(
    limit: int = 50,
    current_user: User = Depends(get_current_user)
):
    user_id = str(current_user.id)
    return await guide_service.get_user_translations(user_id=user_id, limit=limit)

@router.post("/translations")
async def save_user_translation_endpoint(
    request: TranslationSaveRequest,
    current_user: User = Depends(get_current_user)
):
    user_id = str(current_user.id)
    return await guide_service.save_user_translation(user_id=user_id, record=request.dict())

@router.delete("/translations/{translation_id}")
async def delete_user_translation_endpoint(
    translation_id: str,
    current_user: User = Depends(get_current_user)
):
    user_id = str(current_user.id)
    success = await guide_service.delete_user_translation(user_id=user_id, translation_id=translation_id)
    return {"success": success, "translationId": translation_id}

# =============================================================================
# 6. Saved Places (Firestore Persistence)
# =============================================================================
@router.get("/saved-places")
async def get_user_saved_places_endpoint(
    limit: int = 50,
    current_user: User = Depends(get_current_user)
):
    user_id = str(current_user.id)
    return await guide_service.get_user_saved_places(user_id=user_id, limit=limit)

@router.post("/saved-places")
async def save_user_place_endpoint(
    request: SavedPlaceRequest,
    current_user: User = Depends(get_current_user)
):
    user_id = str(current_user.id)
    return await guide_service.save_user_place(user_id=user_id, place=request.dict())

@router.delete("/saved-places/{place_id}")
async def delete_user_saved_place_endpoint(
    place_id: str,
    current_user: User = Depends(get_current_user)
):
    user_id = str(current_user.id)
    success = await guide_service.delete_user_saved_place(user_id=user_id, place_id=place_id)
    return {"success": success, "placeId": place_id}
