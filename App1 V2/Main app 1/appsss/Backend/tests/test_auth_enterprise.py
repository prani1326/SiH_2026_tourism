import pytest
from fastapi.testclient import TestClient
from main import app
from app.core.redis import redis_client

client = TestClient(app)

def test_refresh_token_rotation_and_revocation():
    # 1. Login
    login_res = client.post("/api/v1/auth/login", json={
        "email": "traveler@touristapp.com",
        "password": "Tourist@123"
    })
    assert login_res.status_code == 200
    data = login_res.json()
    access_token = data["access_token"]
    refresh_token = data["refresh_token"]

    # 2. Rotate refresh token
    rotate_res = client.post("/api/v1/auth/refresh", json={
        "refresh_token": refresh_token
    })
    assert rotate_res.status_code == 200
    rot_data = rotate_res.json()
    new_access_token = rot_data["access_token"]
    new_refresh_token = rot_data["refresh_token"]
    assert new_access_token != access_token
    assert new_refresh_token != refresh_token

    # 3. Old refresh token MUST be rejected now
    old_res = client.post("/api/v1/auth/refresh", json={
        "refresh_token": refresh_token
    })
    assert old_res.status_code == 401

    # 4. New refresh token can be used with authenticated routes
    headers = {"Authorization": f"Bearer {new_access_token}"}
    me_res = client.get("/api/v1/users/me", headers=headers)
    assert me_res.status_code == 200

def test_device_registration():
    login_res = client.post("/api/v1/auth/login", json={
        "email": "traveler@touristapp.com",
        "password": "Tourist@123"
    })
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    reg_res = client.post("/api/v1/auth/devices/register", headers=headers, json={
        "fcm_token": "fcm_test_device_token_android_pixel_8",
        "device_id": "pixel-8-imei-test",
        "device_name": "Pixel 8 Pro"
    })
    assert reg_res.status_code == 200
    assert reg_res.json()["success"] is True

def test_redis_logout_token_revocation():
    login_res = client.post("/api/v1/auth/login", json={
        "email": "traveler@touristapp.com",
        "password": "Tourist@123"
    })
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Verify access works before logout
    pre_res = client.get("/api/v1/users/me", headers=headers)
    assert pre_res.status_code == 200

    # Logout
    logout_res = client.post("/api/v1/auth/logout", headers=headers)
    assert logout_res.status_code == 200

    # Access token MUST be rejected immediately after logout
    post_res = client.get("/api/v1/users/me", headers=headers)
    assert post_res.status_code == 401
    assert "revoked" in post_res.json()["detail"].lower()
