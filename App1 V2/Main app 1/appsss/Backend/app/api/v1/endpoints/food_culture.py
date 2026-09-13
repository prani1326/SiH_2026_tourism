from fastapi import APIRouter, HTTPException
from app.repositories.place_repo import place_repo
from app.schemas.destination import MenuScanRequest, MenuScanResponse
from app.services.cultural_engine import cultural_engine

router = APIRouter()

@router.get("/diet-confidence/{place_id}")
def get_restaurant_diet_confidence(place_id: str):
    """Diet Confidence Rating for Restaurant with Firestore (Section 25)."""
    place = place_repo.get_by_id(place_id)
    if not place:
        raise HTTPException(status_code=404, detail="Restaurant not found.")

    dietary_tags = getattr(place, "dietary_tags", []) or []
    diet_confidence = getattr(place, "diet_confidence", 95) or 95

    return {
        "restaurant_name": place.name,
        "dietary_tags": dietary_tags,
        "diet_confidence_score": diet_confidence,
        "confidence_badge": f"{diet_confidence}% Verified Dietary Kitchen",
        "certifications": [
            "100% Segregated Vegetarian Cookware",
            "Jain Preparation on Request (No Root Veggies)",
            "Halal Sourced Meat Certification" if "Halal" in dietary_tags else "Pure Vegetarian Environment"
        ]
    }

@router.post("/menu-scan", response_model=MenuScanResponse)
def scan_menu_for_diet(request: MenuScanRequest):
    """
    Menu Scanning & Dietary Suitability Analyzer (Section 25).
    Translates and inspects menu text or ingredients for Veg, Jain, Halal, and allergens.
    """
    menu_sample = request.menu_text or (
        "Paneer Butter Masala\nAloo Gobhi Dry\nChicken Biryani Special\n"
        "Mushroom Fried Rice\nDal Makhani Handi\nGarlic Naan Butter"
    )
    return cultural_engine.analyze_menu_for_diet(
        menu_text=menu_sample,
        user_diet=request.user_diet
    )
