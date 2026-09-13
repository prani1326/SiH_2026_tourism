import json
import os
import sys
import time
from typing import Dict, Any, List

# Ensure app root is in sys.path
root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, root_dir)

from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

class TestRunner:
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.results: List[Dict[str, Any]] = []
        self.auth_token = None
        self.firebase_token = None
        self.refresh_token = None
        self.user_id = None

    def record(self, name: str, method: str, path: str, status_code: int, expected: int or list, passed: bool, notes: str = ""):
        if passed:
            self.passed += 1
            status_str = "PASS"
        else:
            self.failed += 1
            status_str = "FAIL"
        
        res = {
            "name": name,
            "method": method,
            "path": path,
            "status_code": status_code,
            "expected": expected,
            "result": status_str,
            "notes": notes
        }
        self.results.append(res)
        print(f"[{status_str}] {method} {path} -> {status_code} (expected {expected}) {notes}")

    def run_all(self):
        print("================================================================================")
        print("Tourist App - Firebase Architecture End-to-End Test Suite")
        print("Target: FastAPI + Firebase Admin SDK + Firestore + Auth + Storage + FCM")
        print("================================================================================")

        # ---------------------------------------------------------
        # 1. System Health & Public Baseline
        # ---------------------------------------------------------
        print("\n--- [1/6] System Health & Public APIs ---")
        res = client.get("/")
        self.record("Root System Endpoint", "GET", "/", res.status_code, 200, res.status_code == 200)

        res = client.get("/health")
        self.record("Health Check", "GET", "/health", res.status_code, 200, res.status_code == 200)

        res = client.get("/api/v1/auth/splash/init")
        self.record("Mobile Splash Init", "GET", "/api/v1/auth/splash/init", res.status_code, 200, res.status_code == 200)

        # ---------------------------------------------------------
        # 2. Authentication: Password, OTP & Firebase ID Token
        # ---------------------------------------------------------
        print("\n--- [2/6] Authentication & Firebase Auth ---")
        
        # 2a. Demo traveler password login
        res_login = client.post("/api/v1/auth/login", json={
            "email": "traveler@touristapp.com",
            "password": "Tourist@123"
        })
        login_passed = res_login.status_code == 200 and "access_token" in res_login.json()
        self.record("Standard Password Login", "POST", "/api/v1/auth/login", res_login.status_code, 200, login_passed)
        if login_passed:
            login_data = res_login.json()
            self.auth_token = login_data["access_token"]
            self.refresh_token = login_data.get("refresh_token")

        # 2b. Firebase ID Token Login
        res_fb = client.post("/api/v1/auth/firebase-login", json={
            "id_token": "mock-firebase-google-id-token-traveler-prod-verified",
            "provider": "google.com"
        })
        fb_passed = res_fb.status_code in [200, 201] and "access_token" in res_fb.json()
        self.record("Firebase ID Token Login", "POST", "/api/v1/auth/firebase-login", res_fb.status_code, [200, 201], fb_passed)
        if fb_passed:
            self.firebase_token = res_fb.json()["access_token"]
            self.user_id = res_fb.json().get("user", {}).get("id")

        # 2c. Token Refresh Flow
        if self.refresh_token:
            res_ref = client.post("/api/v1/auth/refresh", json={
                "refresh_token": self.refresh_token
            })
            self.record("Token Refresh", "POST", "/api/v1/auth/refresh", res_ref.status_code, 200, res_ref.status_code == 200)

        # 2d. Phone OTP Dispatch & Verification
        res_otp_send = client.post("/api/v1/auth/phone/otp/send", json={"phone": "+919876543210"})
        self.record("Phone OTP Dispatch", "POST", "/api/v1/auth/phone/otp/send", res_otp_send.status_code, 200, res_otp_send.status_code == 200)

        res_otp_verify = client.post("/api/v1/auth/phone/otp/verify", json={"phone": "+919876543210", "otp_code": "123456"})
        self.record("Phone OTP Verification", "POST", "/api/v1/auth/phone/otp/verify", res_otp_verify.status_code, 200, res_otp_verify.status_code == 200)

        # ---------------------------------------------------------
        # 3. Protected APIs & Authorization Verification
        # ---------------------------------------------------------
        print("\n--- [3/6] Protected APIs & Authorization ---")
        headers = {"Authorization": f"Bearer {self.auth_token}"}
        fb_headers = {"Authorization": f"Bearer {self.firebase_token}"}

        # 3a. Current user profile via JWT
        res = client.get("/api/v1/users/me", headers=headers)
        self.record("Get Profile (Platform JWT)", "GET", "/api/v1/users/me", res.status_code, 200, res.status_code == 200)

        # 3b. Current user profile via Firebase Token
        res_fb_me = client.get("/api/v1/users/me", headers=fb_headers)
        self.record("Get Profile (Firebase Token)", "GET", "/api/v1/users/me", res_fb_me.status_code, 200, res_fb_me.status_code == 200)

        # 3c. Direct Firebase ID token on protected endpoint
        res_direct_fb = client.get("/api/v1/users/me", headers={"Authorization": "Bearer mock-firebase-google-id-token-traveler-prod-verified"})
        self.record("Direct Firebase ID Token on Protected Endpoint", "GET", "/api/v1/users/me", res_direct_fb.status_code, 200, res_direct_fb.status_code == 200)

        # 3d. Update user onboarding preferences
        res = client.post("/api/v1/users/onboarding/preferences", headers=headers, json={
            "preferred_destinations": ["Jaipur", "Kerala", "Goa"],
            "budget_range": "Luxury",
            "travel_styles": ["Cultural", "Adventure"],
            "interests": ["Heritage", "Food", "Photography"],
            "dietary_preferences": ["Vegetarian"]
        })
        self.record("Update Preferences in Firestore", "POST", "/api/v1/users/onboarding/preferences", res.status_code, 200, res.status_code == 200)

        # ---------------------------------------------------------
        # 4. Unauthorized & Invalid Request Validation
        # ---------------------------------------------------------
        print("\n--- [4/6] Unauthorized & Invalid Input Tests ---")
        
        # 4a. Access protected route without token (401 expected)
        res_no_auth = client.get("/api/v1/users/me")
        self.record("Protected Route Without Auth Header", "GET", "/api/v1/users/me", res_no_auth.status_code, 401, res_no_auth.status_code == 401)

        # 4b. Access protected route with invalid token (401 expected)
        res_bad_token = client.get("/api/v1/users/me", headers={"Authorization": "Bearer completely-invalid-expired-jwt-token"})
        self.record("Protected Route With Malformed Token", "GET", "/api/v1/users/me", res_bad_token.status_code, 401, res_bad_token.status_code == 401)

        # 4c. Invalid Schema Input (422 Unprocessable Entity expected)
        res_invalid_input = client.post("/api/v1/auth/login", json={"email": "not-an-email", "invalid_field": 123})
        self.record("Invalid Login Payload Schema", "POST", "/api/v1/auth/login", res_invalid_input.status_code, 422, res_invalid_input.status_code == 422)

        # ---------------------------------------------------------
        # 5. Core Tourist Features CRUD on Firestore
        # ---------------------------------------------------------
        print("\n--- [5/6] Tourist Features & Firestore CRUD ---")

        # 5a. Destinations Listing & Filtering
        res = client.get("/api/v1/destinations/")
        self.record("List Destinations from Firestore", "GET", "/api/v1/destinations/", res.status_code, 200, res.status_code == 200)
        dests = res.json()
        first_dest_id = dests[0]["id"] if dests else "dest-delhi"

        # 5b. Destination Detail
        res = client.get(f"/api/v1/destinations/{first_dest_id}")
        self.record(f"Get Destination Detail ({first_dest_id})", "GET", f"/api/v1/destinations/{first_dest_id}", res.status_code, 200, res.status_code == 200)

        # 5c. Global Search
        res = client.get("/api/v1/search/?q=monument")
        self.record("Global Search", "GET", "/api/v1/search/?q=monument", res.status_code, 200, res.status_code == 200)

        # 5d. Explore Categories
        res = client.get("/api/v1/explore/categories")
        self.record("Explore Categories", "GET", "/api/v1/explore/categories", res.status_code, 200, res.status_code == 200)

        # 5e. AI Trip Planning
        res_ai = client.post("/api/v1/trips/ai-plan", headers=headers, json={
            "destination": "Jaipur",
            "start_date": "2026-10-15",
            "end_date": "2026-10-18",
            "budget": 25000,
            "travelers_count": 2,
            "pace": "Moderate",
            "interests": ["Palaces", "Heritage", "Rajasthani Cuisine"],
            "food_preference": "Vegetarian"
        })
        ai_passed = res_ai.status_code == 200 and "itinerary" in res_ai.json()
        self.record("AI Trip Planner Engine", "POST", "/api/v1/trips/ai-plan", res_ai.status_code, 200, ai_passed)

        # 5f. Trips CRUD
        res_trip_create = client.post("/api/v1/trips/", headers=headers, json={
            "title": "Autumn in Rajasthan",
            "destination_id": first_dest_id,
            "start_date": "2026-10-15",
            "end_date": "2026-10-18",
            "traveler_count": 2,
            "travel_style": "Cultural",
            "total_budget": 35000.0
        })
        created_trip = res_trip_create.json() if res_trip_create.status_code in [200, 201] else {}
        trip_id = created_trip.get("id")
        self.record("Create Trip in Firestore", "POST", "/api/v1/trips/", res_trip_create.status_code, [200, 201], res_trip_create.status_code in [200, 201])

        if trip_id:
            res_trip_get = client.get(f"/api/v1/trips/{trip_id}", headers=headers)
            self.record(f"Get Trip Detail ({trip_id})", "GET", f"/api/v1/trips/{trip_id}", res_trip_get.status_code, 200, res_trip_get.status_code == 200)

            # Add Activity to Trip Day
            res_act = client.post(f"/api/v1/trips/{trip_id}/activities", headers=headers, json={
                "day_number": 1,
                "title": "Visit Amber Fort & Palace",
                "time_slot": "Morning",
                "start_time": "09:00 AM",
                "end_time": "12:00 PM",
                "estimated_cost": 500.0,
                "is_outdoor": True
            })
            self.record("Add Activity to Trip Day in Firestore", "POST", f"/api/v1/trips/{trip_id}/activities", res_act.status_code, [200, 201], res_act.status_code in [200, 201])

            # True Trip Cost
            res_cost = client.get(f"/api/v1/budget/{trip_id}/drift", headers=headers)
            self.record("Budget Drift Calculation", "GET", f"/api/v1/budget/{trip_id}/drift", res_cost.status_code, 200, res_cost.status_code == 200)

            # Trip OS Auto-replanning
            res_replan = client.post(f"/api/v1/trip-os/{trip_id}/replan", headers=headers, json={
                "disruption_type": "Heavy Rain",
                "affected_day": 1,
                "notes": "Outdoor palace visit obstructed by monsoon downpour."
            })
            self.record("Trip OS Auto-Replan", "POST", f"/api/v1/trip-os/{trip_id}/replan", res_replan.status_code, 200, res_replan.status_code == 200)

        # 5g. Request to Book
        res_book = client.post("/api/v1/bookings/request", headers=headers, json={
            "item_type": "Hotel",
            "item_id": "hotel-heritage-palace-1",
            "item_title": "Grand Heritage Palace Haveli",
            "check_in_date": "2026-10-15",
            "check_out_date": "2026-10-18",
            "guest_count": 2,
            "total_amount": 12500.0,
            "trip_id": trip_id
        })
        book_passed = res_book.status_code in [200, 201]
        booking_data = res_book.json() if book_passed else {}
        booking_id = booking_data.get("id")
        self.record("Request-to-Book in Firestore", "POST", "/api/v1/bookings/request", res_book.status_code, [200, 201], book_passed)

        # 5h. List Bookings
        res_bookings = client.get("/api/v1/bookings/", headers=headers)
        self.record("List User Bookings from Firestore", "GET", "/api/v1/bookings/", res_bookings.status_code, 200, res_bookings.status_code == 200)

        # 5i. Payment Initialization
        if booking_id:
            res_pay = client.post(f"/api/v1/payments/{booking_id}/checkout", headers=headers, json={
                "payment_method": "UPI",
                "idempotency_key": f"test_idemp_{int(time.time())}"
            })
            self.record("Payment Order Initialization", "POST", f"/api/v1/payments/{booking_id}/checkout", res_pay.status_code, 200, res_pay.status_code == 200)

        # ---------------------------------------------------------
        # 6. Safety, SOS, Support & FCM
        # ---------------------------------------------------------
        print("\n--- [6/6] Safety, SOS, Support & FCM ---")

        # 6a. Emergency Contacts CRUD
        res_add_contact = client.post("/api/v1/users/me/emergency-contacts", headers=headers, json={
            "contact_name": "Pooja Sharma",
            "relationship_type": "Spouse",
            "phone_number": "+919876543211",
            "email": "pooja@example.com",
            "is_primary": True
        })
        contact_id = res_add_contact.json().get("id") if res_add_contact.status_code in [200, 201] else None
        self.record("Add Emergency Contact in Firestore", "POST", "/api/v1/users/me/emergency-contacts", res_add_contact.status_code, [200, 201], res_add_contact.status_code in [200, 201])

        res_contacts = client.get("/api/v1/users/me/emergency-contacts", headers=headers)
        self.record("List Emergency Contacts from Firestore", "GET", "/api/v1/users/me/emergency-contacts", res_contacts.status_code, 200, res_contacts.status_code == 200)

        # 6b. Emergency SOS Alert Trigger (dispatches FCM and SMS)
        res_sos = client.post("/api/v1/safety/sos", headers=headers, json={
            "latitude": 26.9124,
            "longitude": 75.7873,
            "address": "Near Hawa Mahal, Pink City, Jaipur, Rajasthan",
            "trip_id": trip_id
        })
        sos_id = res_sos.json().get("alert_id") or res_sos.json().get("id")
        self.record("Trigger Emergency SOS in Firestore", "POST", "/api/v1/safety/sos", res_sos.status_code, [200, 201], res_sos.status_code in [200, 201])

        if sos_id:
            res_sos_resolve = client.post(f"/api/v1/safety/sos/{sos_id}/resolve", headers=headers, json={
                "resolution_notes": "Tourist escorted to safe location by local tourist police."
            })
            self.record(f"Resolve SOS Alert ({sos_id}) in Firestore", "POST", f"/api/v1/safety/sos/{sos_id}/resolve", res_sos_resolve.status_code, 200, res_sos_resolve.status_code == 200)

        # 6c. Notification Center
        res_notifs = client.get("/api/v1/notifications/", headers=headers)
        self.record("List Notifications from Firestore", "GET", "/api/v1/notifications/", res_notifs.status_code, 200, res_notifs.status_code == 200)

        # 6d. Support Concierge
        res_ticket = client.post("/api/v1/support/tickets", headers=headers, json={
            "subject": "Inquiry about luggage storage at New Delhi station",
            "category": "General",
            "priority": "Medium",
            "message": "Is there a 24-hour cloakroom available at the station?"
        })
        ticket_id = res_ticket.json().get("id") if res_ticket.status_code in [200, 201] else None
        self.record("Create Support Ticket in Firestore", "POST", "/api/v1/support/tickets", res_ticket.status_code, [200, 201], res_ticket.status_code in [200, 201])

        # 6e. Verified Review Submission
        res_rev = client.post("/api/v1/reviews/", headers=headers, json={
            "target_type": "destination",
            "target_id": first_dest_id,
            "rating": 5.0,
            "comment": "Incredible architecture, vibrant markets and delicious street food!"
        })
        self.record("Submit Verified Review in Firestore", "POST", "/api/v1/reviews/", res_rev.status_code, [200, 201], res_rev.status_code in [200, 201])

        # 6f. Clean-up: Delete test emergency contact
        if contact_id:
            res_del_contact = client.delete(f"/api/v1/users/me/emergency-contacts/{contact_id}", headers=headers)
            self.record("Delete Emergency Contact from Firestore", "DELETE", f"/api/v1/users/me/emergency-contacts/{contact_id}", res_del_contact.status_code, 200, res_del_contact.status_code == 200)

        # ---------------------------------------------------------
        # Summary
        # ---------------------------------------------------------
        print("\n================================================================================")
        print(f"Test Suite Summary: Total: {self.passed + self.failed} | Passed: {self.passed} | Failed: {self.failed}")
        print("================================================================================")
        
        # Save results to JSON file
        report_path = os.path.join(root_dir, "docs", "postman_test_report.json")
        with open(report_path, "w", encoding="utf-8") as f:
            json.dump({
                "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                "total_tests": self.passed + self.failed,
                "passed": self.passed,
                "failed": self.failed,
                "results": self.results
            }, f, indent=2)
        print(f"Full test report saved to: {report_path}")
        return self.failed == 0

if __name__ == "__main__":
    runner = TestRunner()
    success = runner.run_all()
    sys.exit(0 if success else 1)
