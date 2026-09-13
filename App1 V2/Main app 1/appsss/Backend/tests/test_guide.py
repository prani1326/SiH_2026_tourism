import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.security import create_access_token
from app.models.firestore_models import User
from app.repositories.user_repo import user_repo

client = TestClient(app)

@pytest.fixture
def auth_headers():
    test_uid = "test_traveler_guide_123"
    test_user = user_repo.get_by_id(test_uid)
    if not test_user:
        test_user = User(
            id=test_uid,
            firebase_uid=test_uid,
            email="traveler.guide@example.com",
            full_name="Guide Explorer",
            role="traveler",
            is_verified=True,
            is_active=True
        )
        user_repo.create(test_user)

    token = create_access_token(data={"sub": test_uid, "email": "traveler.guide@example.com", "role": "traveler"})
    return {"Authorization": f"Bearer {token}"}

def test_translate_image(auth_headers):
    payload = {
        "image": "Bienvenue à Paris\nRestaurant & Bar",
        "targetLanguage": "en",
        "sourceLanguage": "auto"
    }
    response = client.post("/api/v1/guide/translate-image", json=payload, headers=auth_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["targetLanguage"] == "en"
    assert "Paris" in data["translatedText"] or "Welcome" in data["translatedText"]

def test_detect_place_with_hint(auth_headers):
    payload = {
        "hint": "Taj Mahal",
        "latitude": 27.1751,
        "longitude": 78.0421
    }
    response = client.post("/api/v1/guide/detect-place", json=payload, headers=auth_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert "Taj Mahal" in data["placeName"]
    assert data["city"] == "Agra"
    assert data["country"] == "India"
    assert data["confidence"] >= 0.65
    assert len(data["interestingFacts"]) > 0

def test_detect_place_low_confidence_fallback(auth_headers):
    payload = {
        "hint": "Some unknown mountain without tags",
        "latitude": 0.0,
        "longitude": 0.0
    }
    response = client.post("/api/v1/guide/detect-place", json=payload, headers=auth_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert len(data["candidates"]) > 0

def test_voice_translation(auth_headers):
    payload = {
        "text": "Where is the nearest hotel?",
        "sourceLanguage": "en",
        "targetLanguage": "hi"
    }
    response = client.post("/api/v1/guide/voice/translate", json=payload, headers=auth_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert "होटल" in data["translatedText"] or "अनुवाद" in data["translatedText"]

def test_place_info_guide(auth_headers):
    payload = {
        "placeName": "Agra",
        "userLanguage": "en"
    }
    response = client.post("/api/v1/guide/place-info", json=payload, headers=auth_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["success"] is True
    assert data["name"] == "Agra"
    assert "Taj Mahal" in str(data["famousAttractions"])
    assert len(data["famousFood"]) > 0
    assert len(data["usefulPhrases"]) > 0

def test_saved_translations_flow(auth_headers):
    save_payload = {
        "sourceLanguage": "fr",
        "targetLanguage": "en",
        "originalText": "Merci beaucoup",
        "translatedText": "Thank you very much"
    }
    post_res = client.post("/api/v1/guide/translations", json=save_payload, headers=auth_headers)
    assert post_res.status_code == 200
    saved = post_res.json()
    assert saved["originalText"] == "Merci beaucoup"
    trans_id = saved["translationId"]

    get_res = client.get("/api/v1/guide/translations", headers=auth_headers)
    assert get_res.status_code == 200
    items = get_res.json()
    assert any(i["translationId"] == trans_id for i in items)

    del_res = client.delete(f"/api/v1/guide/translations/{trans_id}", headers=auth_headers)
    assert del_res.status_code == 200

def test_saved_places_flow(auth_headers):
    place_payload = {
        "placeName": "Taj Mahal",
        "city": "Agra",
        "country": "India",
        "location": "Agra, Uttar Pradesh",
        "description": "Iconic marble monument.",
        "latitude": 27.1751,
        "longitude": 78.0421
    }
    post_res = client.post("/api/v1/guide/saved-places", json=place_payload, headers=auth_headers)
    assert post_res.status_code == 200
    saved = post_res.json()
    assert saved["placeName"] == "Taj Mahal"
    place_id = saved["placeId"]

    get_res = client.get("/api/v1/guide/saved-places", headers=auth_headers)
    assert get_res.status_code == 200
    places = get_res.json()
    assert any(p["placeId"] == place_id for p in places)

    del_res = client.delete(f"/api/v1/guide/saved-places/{place_id}", headers=auth_headers)
    assert del_res.status_code == 200
