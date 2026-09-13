import json
import secrets
import pytest
from fastapi.testclient import TestClient
from main import app
from app.services.providers.factory import get_booking_provider

client = TestClient(app)

def test_razorpay_order_and_idempotency():
    # 1. Login
    login_res = client.post("/api/v1/auth/login", json={
        "email": "traveler@touristapp.com",
        "password": "Tourist@123"
    })
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 2. Create a test booking
    bkg_res = client.post("/api/v1/bookings/request-to-book", headers=headers, json={
        "item_type": "Hotel",
        "item_id": f"hotel-{secrets.token_hex(3)}",
        "item_title": "Grand Heritage Palace",
        "check_in_date": "2026-12-10",
        "check_out_date": "2026-12-12",
        "guest_count": 2,
        "total_amount": 18000.0,
        "currency": "INR"
    })
    assert bkg_res.status_code == 200
    booking_id = bkg_res.json()["id"]

    # 3. Create payment order with idempotency key
    idempotency_key = f"idemp-key-{secrets.token_hex(6)}"
    order_res1 = client.post("/api/v1/payments/order", headers=headers, json={
        "booking_id": booking_id,
        "idempotency_key": idempotency_key
    })
    assert order_res1.status_code == 200
    data1 = order_res1.json()
    assert "order_id" in data1
    assert data1["amount"] == 18000.0
    assert data1["currency"] == "INR"

    # 4. Repeat with same idempotency key - must return same order
    order_res2 = client.post("/api/v1/payments/order", headers=headers, json={
        "booking_id": booking_id,
        "idempotency_key": idempotency_key
    })
    assert order_res2.status_code == 200
    data2 = order_res2.json()
    assert data2["order_id"] == data1["order_id"]

    # 5. Verify payment
    verify_res = client.post("/api/v1/payments/verify", headers=headers, json={
        "booking_id": booking_id,
        "razorpay_order_id": data1["order_id"],
        "razorpay_payment_id": f"pay_{secrets.token_hex(6)}",
        "razorpay_signature": f"sig_{secrets.token_hex(8)}"
    })
    assert verify_res.status_code == 200
    assert verify_res.json()["status"] == "Confirmed"
    assert "invoice_number" in verify_res.json()

def test_booking_provider_adapters():
    hotel_p = get_booking_provider("Hotel")
    avail = hotel_p.check_availability("htl-1", "2026-11-01", "2026-11-03")
    assert avail["available"] is True
    res = hotel_p.create_reservation("htl-1", "BKG-REF-1", "Aarav", "+919876543210", "2026-11-01", "2026-11-03")
    assert res["success"] is True
    assert "vendor_booking_ref" in res

    act_p = get_booking_provider("Activity")
    res_act = act_p.create_reservation("act-1", "BKG-REF-2", "Aarav", "+919876543210", "2026-11-02")
    assert res_act["success"] is True
    assert "access_pass_code" in res_act

    trans_p = get_booking_provider("Transport")
    res_trans = trans_p.create_reservation("trans-1", "BKG-REF-3", "Aarav", "+919876543210", "2026-11-01")
    assert res_trans["success"] is True
    assert "driver_contact" in res_trans
