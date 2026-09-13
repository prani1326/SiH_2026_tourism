from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field

class ImageTranslateRequest(BaseModel):
    image: str = Field(..., description="Base64 encoded image string or data URI")
    targetLanguage: str = Field("en", description="Target language code (e.g. en, hi, es, fr, de)")
    sourceLanguage: Optional[str] = Field("auto", description="Source language code or 'auto'")

class ImageTranslateResponse(BaseModel):
    success: bool
    sourceLanguage: str
    targetLanguage: str
    originalText: str
    translatedText: str
    confidence: float = 0.95
    errorMessage: Optional[str] = None

class PlaceDetectRequest(BaseModel):
    image: Optional[str] = Field(None, description="Base64 encoded image string")
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    hint: Optional[str] = None

class PlaceDetectResponse(BaseModel):
    success: bool
    placeName: str
    city: str
    country: str
    latitude: float
    longitude: float
    confidence: float
    isConfident: bool = True
    candidates: List[str] = []
    builtDate: str = ""
    whyFamous: str = ""
    history: str = ""
    culturalImportance: str = ""
    interestingFacts: List[str] = []
    visitingTips: List[str] = []
    bestTimeToVisit: str = ""
    nearbyAttractions: List[str] = []
    imageUrl: Optional[str] = None
    errorMessage: Optional[str] = None

class VoiceTranslateRequest(BaseModel):
    audio: Optional[str] = Field(None, description="Base64 audio or data payload")
    text: Optional[str] = Field(None, description="Transcribed text if speech-to-text was run on device")
    sourceLanguage: str = "en"
    targetLanguage: str = "hi"

class VoiceTranslateResponse(BaseModel):
    success: bool
    detectedLanguage: str
    originalText: str
    translatedText: str
    confidence: float = 0.95
    errorMessage: Optional[str] = None

class PlaceInfoRequest(BaseModel):
    placeId: Optional[str] = None
    placeName: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    userLanguage: Optional[str] = "en"

class PlaceInfoResponse(BaseModel):
    success: bool
    id: str
    name: str
    stateCountry: str
    tagline: str
    overview: str
    history: str
    culture: str
    localTraditions: List[str] = []
    famousAttractions: List[str] = []
    famousFood: List[str] = []
    thingsToDo: List[str] = []
    safetyTips: List[str] = []
    transportationTips: str = ""
    bestVisitingTime: str = ""
    nearbyPlaces: List[str] = []
    usefulPhrases: List[Dict[str, str]] = []
    touristTips: List[str] = []
    errorMessage: Optional[str] = None

class TranslationSaveRequest(BaseModel):
    translationId: Optional[str] = None
    sourceLanguage: str
    targetLanguage: str
    originalText: str
    translatedText: str
    source: Optional[str] = "camera"

class SavedPlaceRequest(BaseModel):
    placeId: Optional[str] = None
    placeName: str
    city: Optional[str] = None
    country: Optional[str] = None
    location: Optional[str] = None
    description: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    confidence: Optional[float] = 0.95
    category: Optional[str] = "Landmark"
    rating: Optional[float] = 4.8

class ConversationSaveRequest(BaseModel):
    conversationId: Optional[str] = None
    speaker: str
    sourceLanguage: str
    targetLanguage: str
    originalText: str
    translatedText: str
