import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_safety_intelligence_endpoints():
    res = client.get("/api/v1/safety-intel/current?destination=Jaipur")
    assert res.status_code == 200
    data = res.json()
    assert "safety_score" in data
    assert "risk_label" in data
    assert data["destination"] == "Jaipur"

    route_res = client.post("/api/v1/safety-intel/routes", json={"origin": "Jaipur Junction", "destination": "Amber Fort"})
    assert route_res.status_code == 200
    routes = route_res.json()
    assert len(routes) >= 3
    assert any(r["id"] == "route_safest" for r in routes)

def test_crowd_intelligence_endpoints():
    res = client.get("/api/v1/crowd/monument?name=Amber Fort")
    assert res.status_code == 200
    data = res.json()
    assert "crowd_density_pct" in data
    assert "alternatives" in data
    assert len(data["alternatives"]) > 0

def test_guardian_endpoints():
    start_res = client.post("/api/v1/guardian/start", json={"trip_name": "Goa Coastal Explorer"})
    assert start_res.status_code == 200
    session = start_res.json()
    assert session["status"] == "ACTIVE_ON_SCHEDULE"
    assert "last_check_in" in session

    checkin_res = client.post("/api/v1/guardian/check-in", json={"battery_level": 82, "location_name": "Calangute Beach"})
    assert checkin_res.status_code == 200
    checkin_data = checkin_res.json()
    assert checkin_data["success"] is True

def test_offline_emergency_and_sync():
    bundle_res = client.get("/api/v1/emergency/bundle?destination=Jaipur")
    assert bundle_res.status_code == 200
    bundle = bundle_res.json()
    assert len(bundle["destination_helplines"]) >= 4

    sync_res = client.post("/api/v1/emergency/sync", json={
        "packet_id": "pkt_offline_001",
        "latitude": 26.9124,
        "longitude": 75.7873,
        "timestamp_offline": "2026-09-08T00:00:00Z",
        "emergency_type": "MEDICAL_ASSISTANCE"
    })
    assert sync_res.status_code == 200
    assert sync_res.json()["success"] is True

def test_heritage_lens_endpoints():
    res = client.post("/api/v1/heritage/analyze", json={"monument_hint": "Taj Mahal"})
    assert res.status_code == 200
    data = res.json()
    assert data["success"] is True
    assert "Taj Mahal" in data["monument"]["name"]
    assert "narration_script_hi" in data["monument"]

def test_scam_shield_endpoints():
    nearby_res = client.get("/api/v1/scams/nearby?destination=Jaipur")
    assert nearby_res.status_code == 200
    scams = nearby_res.json()
    assert len(scams) >= 2

    report_res = client.post("/api/v1/scams/report", json={
        "destination": "Jaipur",
        "scam_type": "Fake Meter",
        "location": "Airport Exit",
        "description": "Auto driver charged 3x normal rate.",
        "estimated_loss_inr": 300
    })
    assert report_res.status_code == 200
    assert report_res.json()["success"] is True

def test_sustainability_endpoints():
    res = client.get("/api/v1/sustainability/trip_123?transport=Metro&eco_stay=true")
    assert res.status_code == 200
    data = res.json()
    assert "overall_score" in data
    assert "breakdown" in data
    assert "carbon_offset_kg" in data

def test_local_experiences_endpoints():
    res = client.get("/api/v1/local-experiences?destination=Jaipur")
    assert res.status_code == 200
    exps = res.json()
    assert len(exps) >= 3
    assert any("Bagru" in e["title"] for e in exps)

def test_authority_intelligence_endpoints():
    crowd_res = client.get("/api/v1/intelligence/crowd")
    assert crowd_res.status_code == 200
    assert "total_active_tourists_estimate" in crowd_res.json()

    safety_res = client.get("/api/v1/intelligence/safety")
    assert safety_res.status_code == 200
    assert "state_safety_index" in safety_res.json()
