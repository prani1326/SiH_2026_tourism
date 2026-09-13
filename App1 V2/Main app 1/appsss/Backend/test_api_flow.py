import requests
import json

def run_flow():
    token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjI0N2Y4MDYwMDM5YjVmNDBkOTQ5NjkzOGJiMTg5NzA2ZWY4ODkzM2QiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vdHJpcC1wbGFubmVyLXZlcnNpb24tMSIsImF1ZCI6InRyaXAtcGxhbm5lci12ZXJzaW9uLTEiLCJhdXRoX3RpbWUiOjE3ODg1NDkwOTYsInVzZXJfaWQiOiJwcUdPZFhPV21pVWw4N0hCZ3VnVVhaa251QmMyIiwic3ViIjoicHFHT2RYT1dtaVVsODdIQmd1Z1VYWmtudUJjMiIsImlhdCI6MTc4ODU1NDk5NCwiZXhwIjoxNzg4NTU4NTk0LCJlbWFpbCI6ImFiY0BhYmMuY29tIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJmaXJlYmFzZSI6eyJpZGVudGl0aWVzIjp7ImVtYWlsIjpbImFiY0BhYmMuY29tIl19LCJzaWduX2luX3Byb3ZpZGVyIjoicGFzc3dvcmQifX0.WPzWwVii5K6OzPsUq6G8HAs5YmhisqxjgmWkkcepyu_Bx-FKRQukiikyxfB4dGs_T7hyUGRcd9m_xEg9GNkJAs51tGVnCbUU92xTzavOAq7pZr8v9ATA2kT7AJMeEd6sMPZpjwnmsMyaNC03Ys10RN8aNQbXhgwd1OM5SXZ_wtYorcd1A54hKm7RwQg8wVuYliiJt8aOfxzz_7lShb1_6iD2MXO_ptKearmjTR-T39J2R6SFUqdoaJaQkuFAUB1pWIo_tF4J1X35JZzMtxi0QAAr1Y8npNQYqXwr0hAd0wg0KIMGCharRqN77uyNzU-LI7pIYvSppxgVKE4Guacp6w"

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    print("1. Testing GET /api/v1/auth/me ...")
    res = requests.get("http://localhost:8000/api/v1/auth/me", headers=headers)
    print("Auth Me status:", res.status_code)
    print("Auth Me response:", res.text)

    print("\n2. Testing POST /api/v1/payments/order ...")
    order_res = requests.post(
        "http://localhost:8000/api/v1/payments/order",
        headers=headers,
        json={"booking_id": "BKG-HOT-5F2E65"}
    )
    print("Order status:", order_res.status_code)
    print("Order response:", order_res.text)

    if order_res.status_code == 200:
        order_data = order_res.json()
        order_id = order_data.get("order_id")
        print(f"\n3. Testing POST /api/v1/payments/verify with order_id={order_id} ...")
        verify_res = requests.post(
            "http://localhost:8000/api/v1/payments/verify",
            headers=headers,
            json={
                "booking_id": "BKG-HOT-5F2E65",
                "razorpay_order_id": order_id,
                "razorpay_payment_id": f"pay_{order_id[-8:]}",
                "razorpay_signature": f"sig_{order_id[-8:]}"
            }
        )
        print("Verify status:", verify_res.status_code)
        print("Verify response:", verify_res.text)

if __name__ == "__main__":
    run_flow()
