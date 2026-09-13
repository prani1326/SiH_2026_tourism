import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_root_health_and_security_headers():
    """Verify system root, health check, and enterprise security headers."""
    res_root = client.get("/")
    assert res_root.status_code == 200
    assert res_root.json()["status"] == "online"
    assert "bottom_navigation_spec" in res_root.json()

    # Verify enterprise security headers
    assert res_root.headers.get("X-Content-Type-Options") == "nosniff"
    assert res_root.headers.get("X-Frame-Options") == "DENY"
    assert res_root.headers.get("X-XSS-Protection") == "1; mode=block"
    assert "Strict-Transport-Security" in res_root.headers
    assert "X-Request-ID" in res_root.headers
    assert "X-Process-Time-Ms" in res_root.headers

    res_health = client.get("/health")
    assert res_health.status_code == 200
    assert res_health.json()["status"] == "healthy"

def test_splash_init():
    """Verify splash screen configuration, currencies, and feature flags."""
    res = client.get("/api/v1/auth/splash/init")
    assert res.status_code == 200
    data = res.json()
    assert "INR" in data["supported_currencies"]
    assert "en" in data["supported_languages"]
    assert data["emergency_baseline"]["National Emergency"] == "112"
    assert data["feature_flags"]["ai_planner_active"] is True

def test_auth_login_profile_and_logout_revocation():
    """Verify login, profile access, and token blacklisting on logout."""
    # 1. Login with demo traveler credentials
    login_res = client.post("/api/v1/auth/login", json={
        "email": "traveler@touristapp.com",
        "password": "Tourist@123"
    })
    assert login_res.status_code == 200
    login_data = login_res.json()
    token = login_data["access_token"]
    assert token is not None

    # 2. Access profile with active token
    headers = {"Authorization": f"Bearer {token}"}
    me_res = client.get("/api/v1/users/me", headers=headers)
    assert me_res.status_code == 200
    me_data = me_res.json()
    assert me_data["email"] == "traveler@touristapp.com"
    assert me_data["full_name"] == "Aarav Sharma"

    # 3. Logout - revokes the active token
    logout_res = client.post("/api/v1/auth/logout", headers=headers)
    assert logout_res.status_code == 200
    assert logout_res.json()["success"] is True

    # 4. Attempting to use the revoked token MUST fail with 401 Unauthorized
    revoked_res = client.get("/api/v1/users/me", headers=headers)
    assert revoked_res.status_code == 401
    assert "revoked" in revoked_res.json()["detail"].lower()

def test_phone_otp_flow():
    """Verify mobile number OTP dispatch and verification."""
    send_res = client.post("/api/v1/auth/otp/send", json={"phone": "+919988776655"})
    assert send_res.status_code == 200
    assert send_res.json()["success"] is True

    verify_res = client.post("/api/v1/auth/otp/verify", json={
        "phone": "+919988776655",
        "otp_code": "123456"
    })
    assert verify_res.status_code == 200
    assert "access_token" in verify_res.json()

def test_password_recovery_flow():
    """Verify forgot password and password reset confirmation."""
    req_res = client.post("/api/v1/auth/recovery/request", json={"email": "traveler@touristapp.com"})
    assert req_res.status_code == 200
    code = req_res.json()["recovery_code"]

    confirm_res = client.post("/api/v1/auth/recovery/confirm", json={
        "identifier": "traveler@touristapp.com",
        "recovery_code": code,
        "new_password": "NewTouristPass@123"
    })
    assert confirm_res.status_code == 200

    # Reset back to default password for other tests
    client.post("/api/v1/auth/recovery/confirm", json={
        "identifier": "traveler@touristapp.com",
        "recovery_code": code,
        "new_password": "Tourist@123"
    })

def test_google_firebase_login():
    """Verify Google / Firebase ID token exchange."""
    res = client.post("/api/v1/auth/google", json={"id_token": "mock-firebase-google-id-token"})
    assert res.status_code == 200
    data = res.json()
    assert "access_token" in data
    assert data["user"]["full_name"] == "Global Traveler"

def test_travel_preferences_onboarding():
    """Verify travel preference onboarding."""
    login_res = client.post("/api/v1/auth/login", json={
        "email": "traveler@touristapp.com",
        "password": "Tourist@123"
    })
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    pref_res = client.post("/api/v1/users/onboarding/preferences", headers=headers, json={
        "preferred_destinations": ["Jaipur", "Goa"],
        "budget_range": "₹40,000 - ₹80,000",
        "travel_styles": ["Couple", "Luxury"],
        "interests": ["Heritage", "Food", "Beaches"],
        "dietary_preferences": ["Vegetarian", "Jain"],
        "walking_tolerance": "Moderate",
        "language": "en",
        "currency": "INR"
    })
    assert pref_res.status_code == 200
    assert pref_res.json()["success"] is True

def test_destinations_and_search():
    """Verify destinations listing, detail, and global search."""
    list_res = client.get("/api/v1/destinations/")
    assert list_res.status_code == 200
    dests = list_res.json()
    assert len(dests) >= 5
    agra_dest = next((d for d in dests if d["name"] == "Agra"), None)
    assert agra_dest is not None

    # Get Agra detail
    detail_res = client.get(f"/api/v1/destinations/{agra_dest['id']}")
    assert detail_res.status_code == 200
    detail = detail_res.json()
    assert len(detail["places"]) >= 1
    assert len(detail["scam_alerts"]) >= 1

    # Destination travel guide
    guide_res = client.get(f"/api/v1/destinations/{agra_dest['id']}/guide")
    assert guide_res.status_code == 200
    assert guide_res.json()["destination"] == "Agra"

    # Global search for "Taj"
    search_res = client.get("/api/v1/search/global?q=Taj")
    assert search_res.status_code == 200
    search_data = search_res.json()
    assert search_data["total_results"] >= 1

def test_places_and_confidence_engine():
    """Verify place details, confidence breakdown, and verified reviews."""
    places_res = client.get("/api/v1/places/")
    assert places_res.status_code == 200
    places = places_res.json()
    assert len(places) >= 1
    place_id = places[0]["id"]

    # Detail
    p_detail = client.get(f"/api/v1/places/{place_id}")
    assert p_detail.status_code == 200

    # Algorithmic confidence score explanation
    conf_res = client.get(f"/api/v1/places/{place_id}/confidence")
    assert conf_res.status_code == 200
    conf_data = conf_res.json()
    assert "confidence_score" in conf_data
    assert len(conf_data["reasons"]) >= 1

def test_explore_categories_and_map():
    """Verify explore categories and List / Map view toggles."""
    cats_res = client.get("/api/v1/explore/categories")
    assert cats_res.status_code == 200
    assert len(cats_res.json()["categories"]) >= 5

    # List view
    list_view = client.get("/api/v1/explore/category/Heritage?view_mode=list")
    assert list_view.status_code == 200
    assert len(list_view.json()["items"]) >= 1

    # Map view
    map_view = client.get("/api/v1/explore/category/Heritage?view_mode=map")
    assert map_view.status_code == 200
    assert len(map_view.json()["map_markers"]) >= 1

def test_ai_trip_planner():
    """Verify AI Trip Planner generates day-wise structured itinerary."""
    plan_res = client.post("/api/v1/trips/ai-plan", json={
        "destination": "Goa",
        "start_date": "2026-11-10",
        "end_date": "2026-11-14",
        "traveler_count": 2,
        "budget": 45000.0,
        "travel_style": "Couple",
        "interests": ["Beaches", "Food"],
        "food_preference": "Vegetarian",
        "walking_tolerance": "Moderate",
        "pace": "Moderate"
    })
    assert plan_res.status_code == 200
    plan_data = plan_res.json()
    assert plan_data["duration_days"] == 5
    assert len(plan_data["days"]) == 5
    first_day = plan_data["days"][0]
    assert len(first_day["activities"]) == 4  # Morning, Afternoon, Evening, Night
    assert "alternative_activity" in first_day

def test_itinerary_builder_activity_crud():
    """Verify itinerary builder: add activity, update, reorder, delete."""
    login_res = client.post("/api/v1/auth/login", json={
        "email": "traveler@touristapp.com",
        "password": "Tourist@123"
    })
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    trips_res = client.get("/api/v1/trips", headers=headers)
    assert trips_res.status_code == 200
    user_trips = trips_res.json()
    if not user_trips or not user_trips[0].get("days"):
        create_res = client.post("/api/v1/trips/custom", headers=headers, json={
            "destination_name": "Goa",
            "title": "Explore Goa",
            "start_date": "2026-11-10",
            "end_date": "2026-11-12",
            "total_budget": 15000.0,
            "days": [{"day_number": 1, "date": "2026-11-10", "activities": []}]
        })
        user_trips = [create_res.json()]
    assert len(user_trips) >= 1
    trip = user_trips[0]
    day_id = trip["days"][0]["id"]

    # 1. Add Activity
    add_res = client.post("/api/v1/trips/activities", json={
        "trip_day_id": day_id,
        "time_slot": "Evening",
        "title": "Chai & Street Samosa Tasting Walk",
        "estimated_cost": 250.0,
        "travel_time_minutes": 10
    })
    assert add_res.status_code == 200
    act_id = add_res.json()["activity_id"]

    # 2. Update Activity
    up_res = client.put(f"/api/v1/trips/activities/{act_id}", json={
        "title": "Updated Chai & Food Walk",
        "is_completed": True
    })
    assert up_res.status_code == 200

    # 3. Delete Activity
    del_res = client.delete(f"/api/v1/trips/activities/{act_id}")
    assert del_res.status_code == 200

def test_trip_share_and_duplicate():
    """Verify shareable link generation and duplicate trip cloning."""
    login_res = client.post("/api/v1/auth/login", json={
        "email": "traveler@touristapp.com",
        "password": "Tourist@123"
    })
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    trips_res = client.get("/api/v1/trips", headers=headers)
    trip_id = trips_res.json()[0]["id"]

    # Share
    share_res = client.post(f"/api/v1/trips/{trip_id}/share")
    assert share_res.status_code == 200
    assert "share_url" in share_res.json()

    # Duplicate
    dup_res = client.post(f"/api/v1/trips/{trip_id}/duplicate", headers=headers)
    assert dup_res.status_code == 200
    assert "new_trip_id" in dup_res.json()

def test_trip_os_auto_replanning():
    """Verify Trip OS dynamic replanning on disruption (e.g. Rain)."""
    trips_res = client.get("/api/v1/destinations/")
    agra_id = trips_res.json()[0]["id"]

    replan_res = client.post("/api/v1/trip-os/replan", json={
        "trip_id": agra_id,
        "disruption_type": "Rain",
        "disruption_details": "Heavy thunderstorm predicted at 3:00 PM."
    })
    assert replan_res.status_code == 200
    replan_data = replan_res.json()
    assert "recommended_action" in replan_data
    assert len(replan_data["suggested_replacements"]) >= 1

def test_true_trip_cost_and_drift():
    """Verify True Trip Cost calculation and drift detection."""
    cost_res = client.get("/api/v1/budget/dummy-trip-id/true-cost")
    assert cost_res.status_code == 200
    cost_data = cost_res.json()
    assert "breakdown" in cost_data
    assert "flights_intercity" in cost_data["breakdown"]
    assert "drift_percent" in cost_data
    assert len(cost_data["cost_saving_suggestions"]) >= 1

def test_request_to_book_and_payment():
    """Verify Request-to-Book lifecycle and simulated payment."""
    login_res = client.post("/api/v1/auth/login", json={
        "email": "traveler@touristapp.com",
        "password": "Tourist@123"
    })
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Request to Book
    bkg_res = client.post("/api/v1/bookings/request-to-book", headers=headers, json={
        "item_type": "Hotel",
        "item_id": "hotel-123",
        "item_title": "Boutique Heritage Haveli",
        "check_in_date": "2026-11-15",
        "check_out_date": "2026-11-18",
        "guest_count": 2,
        "total_amount": 14500.0,
        "currency": "INR"
    })
    assert bkg_res.status_code == 200
    bkg = bkg_res.json()
    assert bkg["status"] == "Requested"
    booking_id = bkg["id"]

    # 2. Process Payment
    pay_res = client.post("/api/v1/payments/process", headers=headers, json={
        "booking_id": booking_id,
        "payment_method": "UPI",
        "currency": "INR"
    })
    assert pay_res.status_code == 200
    pay_data = pay_res.json()
    assert pay_data["status"] == "Succeeded"
    assert "invoice_number" in pay_data

    # 3. Invoice
    inv_res = client.get(f"/api/v1/payments/{booking_id}/invoice")
    assert inv_res.status_code == 200
    assert inv_res.json()["status"] == "Paid & Verified"

def test_cancellation_and_refund_milestones():
    """Verify booking cancellation and refund milestone progression."""
    login_res = client.post("/api/v1/auth/login", json={
        "email": "traveler@touristapp.com",
        "password": "Tourist@123"
    })
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Create new booking to cancel
    bkg_res = client.post("/api/v1/bookings/request-to-book", headers=headers, json={
        "item_type": "Experience",
        "item_id": "exp-boat-9",
        "item_title": "Sunset River Boat Cruise",
        "check_in_date": "2026-12-01",
        "total_amount": 1800.0
    })
    booking_id = bkg_res.json()["id"]

    # Cancel
    cancel_res = client.post("/api/v1/refunds/cancel", headers=headers, json={
        "booking_id": booking_id,
        "cancellation_reason": "Travel dates changed due to schedule conflict."
    })
    assert cancel_res.status_code == 200
    refund_data = cancel_res.json()
    assert refund_data["status"] == "Initiated"
    assert len(refund_data["milestones"]) >= 3

def test_digital_trip_card_and_offline_package():
    """Verify Digital Trip Card generation with QR and offline package bundler."""
    trips_res = client.get("/api/v1/trips", headers={"Authorization": "Bearer dummy"})
    # Query seeded trip from DB
    dest_res = client.get("/api/v1/destinations/")
    agra_id = dest_res.json()[0]["id"]

    login_res = client.post("/api/v1/auth/login", json={"email": "traveler@touristapp.com", "password": "Tourist@123"})
    token = login_res.json()["access_token"]
    user_trips = client.get("/api/v1/trips", headers={"Authorization": f"Bearer {token}"}).json()
    trip_id = user_trips[0]["id"]

    # 1. Digital Trip Card
    card_res = client.get(f"/api/v1/trip-card/{trip_id}/card")
    assert card_res.status_code == 200
    card = card_res.json()
    assert "data:image/png;base64" in card["qr_code_base64"]
    assert "insurance_policy" in card

    # 2. Offline Package
    off_res = client.get(f"/api/v1/trip-card/{trip_id}/offline-package")
    assert off_res.status_code == 200
    off_pkg = off_res.json()
    assert "offline_map_manifest" in off_pkg
    assert "emergency_pocket_guide" in off_pkg

def test_safety_sos_dispatch_and_resolution():
    """Verify One-Tap SOS emergency dispatcher and resolution."""
    sos_res = client.post("/api/v1/safety/sos", json={
        "latitude": 27.1751,
        "longitude": 78.0421,
        "address": "East Gate Taj Complex",
        "emergency_type": "Medical"
    })
    assert sos_res.status_code == 200
    sos_data = sos_res.json()
    assert sos_data["status"] == "SOS_DISPATCHED"
    assert "112" in sos_data["emergency_services"].values()
    assert "nearest_police" in sos_data
    alert_id = sos_data["alert_id"]

    # Resolve SOS
    res_res = client.post(f"/api/v1/safety/sos/{alert_id}/resolve")
    assert res_res.status_code == 200
    assert res_res.json()["success"] is True

def test_lost_phone_mode_and_freeze():
    """Verify Lost Phone Mode recovery with email and emergency PIN, and account freeze."""
    rec_res = client.post("/api/v1/lost-phone/recover", json={
        "email": "traveler@touristapp.com",
        "emergency_recovery_pin": "1234"
    })
    assert rec_res.status_code == 200
    rec_data = rec_res.json()
    assert rec_data["user_name"] == "Aarav Sharma"
    assert "recovery_token" in rec_data
    assert len(rec_data["emergency_contacts"]) >= 1

    # Freeze Account
    token = rec_data["recovery_token"]
    freeze_res = client.post(f"/api/v1/lost-phone/freeze/{token}")
    assert freeze_res.status_code == 200
    assert freeze_res.json()["success"] is True

def test_separated_group_mode():
    """Verify Lost Person / Separated Group Mode setup and check-in."""
    login_res = client.post("/api/v1/auth/login", json={"email": "traveler@touristapp.com", "password": "Tourist@123"})
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Setup Group
    grp_res = client.post("/api/v1/separated-mode/groups", headers=headers, json={
        "group_name": "Sharma Family Tour",
        "rendezvous_meeting_point": "Gate 2 Tourist Reception Center",
        "geofence_lat": 26.9124,
        "geofence_lon": 75.7873,
        "geofence_radius_meters": 500.0
    })
    assert grp_res.status_code == 200
    grp = grp_res.json()
    group_id = grp["group_id"]
    assert grp["rendezvous_meeting_point"] == "Gate 2 Tourist Reception Center"

    # 2. Member Check-in
    chk_res = client.post("/api/v1/separated-mode/checkin", json={
        "group_id": group_id,
        "member_name": "Priya Sharma",
        "latitude": 26.9125,
        "longitude": 75.7874,
        "status": "Safe"
    })
    assert chk_res.status_code == 200
    assert chk_res.json()["status"] == "Safe"

    # 3. Group Status
    stat_res = client.get(f"/api/v1/separated-mode/groups/{group_id}/status")
    assert stat_res.status_code == 200
    assert len(stat_res.json()["members"]) >= 2

def test_transport_and_menu_scanner():
    """Verify Local Transport Brain and Menu Scanner OCR analysis."""
    trans_res = client.get("/api/v1/transport-brain/compare?city=Agra")
    assert trans_res.status_code == 200
    assert len(trans_res.json()["options"]) >= 3

    menu_res = client.post("/api/v1/food-culture/menu-scan", json={
        "menu_text": "Paneer Butter Masala\nAloo Gobhi Dry\nChicken Curry Special",
        "user_diet": "Vegetarian"
    })
    assert menu_res.status_code == 200
    menu_data = menu_res.json()
    assert "Caution" in menu_data["overall_suitability"]
    assert len(menu_data["detected_risks"]) >= 1

def test_destination_preparation_checklist():
    """Verify Preparation Engine packing list and cultural guidelines."""
    prep_res = client.get("/api/v1/preparation/Agra")
    assert prep_res.status_code == 200
    prep_data = prep_res.json()
    assert len(prep_data["weather_packing_list"]) >= 3
    assert len(prep_data["cultural_preparation"]) >= 2

def test_verified_review_submission():
    """Verify verified review submission with multi-factor ratings."""
    login_res = client.post("/api/v1/auth/login", json={"email": "traveler@touristapp.com", "password": "Tourist@123"})
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    places_res = client.get("/api/v1/places/")
    place_id = places_res.json()[0]["id"]

    rev_res = client.post("/api/v1/reviews/", headers=headers, json={
        "place_id": place_id,
        "rating": 5.0,
        "title": "Outstanding guide and impeccable timing!",
        "comment": "The electric golf cart transfer was punctual and our verified guide was exceptional.",
        "cleanliness_rating": 5.0,
        "punctuality_rating": 5.0,
        "safety_rating": 5.0
    })
    assert rev_res.status_code == 200
    assert rev_res.json()["is_verified_booking"] is True

def test_community_forums_and_creators():
    """Verify community forums, posts, and creator itineraries marketplace."""
    # Forums
    forums_res = client.get("/api/v1/community/forums")
    assert forums_res.status_code == 200
    forums = forums_res.json()
    assert len(forums) >= 1
    forum_id = forums[0]["id"]

    # Posts
    posts_res = client.get(f"/api/v1/community/forums/{forum_id}/posts")
    assert posts_res.status_code == 200

    # Creator Itineraries
    creators_res = client.get("/api/v1/community/creators/itineraries")
    assert creators_res.status_code == 200
    creators = creators_res.json()
    assert len(creators) >= 1

def test_group_voting_and_expense_splitting():
    """Verify collaborative group voting and expense splitting."""
    login_res = client.post("/api/v1/auth/login", json={"email": "traveler@touristapp.com", "password": "Tourist@123"})
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Vote
    vote_res = client.post("/api/v1/groups/vote", headers=headers, json={
        "trip_id": "trip-rajasthan-01",
        "item_type": "Hotel",
        "item_id": "hotel-heritage-haveli",
        "vote": "yes"
    })
    assert vote_res.status_code == 200
    assert vote_res.json()["current_tally"]["yes_votes"] >= 1

    # Split expense
    split_res = client.post("/api/v1/groups/split-expense", json={
        "trip_id": "trip-rajasthan-01",
        "title": "Private Tempo Traveler (Delhi to Agra)",
        "total_amount": 9000.0,
        "paid_by_member": "Aarav",
        "split_members": ["Aarav", "Rohan", "Priya"]
    })
    assert split_res.status_code == 200
    split_data = split_res.json()
    assert split_data["per_person_share"] == 3000.0

def test_saved_collections():
    """Verify user saved collections (Wishlist, Plan Later, Favorites) and item removal."""
    login_res = client.post("/api/v1/auth/login", json={"email": "traveler@touristapp.com", "password": "Tourist@123"})
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    colls_res = client.get("/api/v1/collections/", headers=headers)
    assert colls_res.status_code == 200
    colls = colls_res.json()
    assert len(colls) >= 1
    coll_id = colls[0]["id"]

    # Add item
    item_res = client.post(f"/api/v1/collections/{coll_id}/items", headers=headers, json={
        "item_type": "destination",
        "item_id": "dest-agra-01",
        "item_name": "Agra Fort Heritage",
        "notes": "Plan for early morning tour"
    })
    assert item_res.status_code == 200
    item_id = item_res.json()["id"]

    # Delete item
    del_res = client.delete(f"/api/v1/collections/items/{item_id}")
    assert del_res.status_code == 200

def test_bundles_and_events():
    """Verify Day Pass Bundles and local events calendar."""
    dest_res = client.get("/api/v1/destinations/")
    dest_id = dest_res.json()[0]["id"]

    passes_res = client.get(f"/api/v1/bundles-events/day-passes/{dest_id}")
    assert passes_res.status_code == 200

    events_res = client.get(f"/api/v1/bundles-events/events/{dest_id}")
    assert events_res.status_code == 200
    assert len(events_res.json()["events"]) >= 1

def test_support_tickets_and_messaging():
    """Verify 24/7 support ticket opening and concierge chat messaging."""
    login_res = client.post("/api/v1/auth/login", json={"email": "traveler@touristapp.com", "password": "Tourist@123"})
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Open Ticket
    tick_res = client.post("/api/v1/support/tickets", headers=headers, json={
        "category": "Booking",
        "subject": "Need late check-out at Agra resort",
        "message": "Can I extend checkout till 02:00 PM due to our afternoon train?"
    })
    assert tick_res.status_code == 200
    ticket = tick_res.json()
    assert len(ticket["messages"]) >= 2  # User message + automated AI concierge response
    ticket_id = ticket["id"]

    # Reply
    reply_res = client.post(f"/api/v1/support/tickets/{ticket_id}/messages", headers=headers, json={
        "ticket_id": ticket_id,
        "message": "Thank you for following up so quickly!"
    })
    assert reply_res.status_code == 200

def test_notifications_center():
    """Verify notification center listing and marking as read."""
    login_res = client.post("/api/v1/auth/login", json={"email": "traveler@touristapp.com", "password": "Tourist@123"})
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    notifs_res = client.get("/api/v1/notifications/", headers=headers)
    assert notifs_res.status_code == 200
    notifs = notifs_res.json()
    if notifs:
        notif_id = notifs[0]["id"]
        read_res = client.put(f"/api/v1/notifications/{notif_id}/read")
        assert read_res.status_code == 200

def test_gdpr_data_export():
    """Verify GDPR data portability export."""
    login_res = client.post("/api/v1/auth/login", json={"email": "traveler@touristapp.com", "password": "Tourist@123"})
    token = login_res.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    gdpr_res = client.get("/api/v1/users/gdpr/export", headers=headers)
    assert gdpr_res.status_code == 200
    data = gdpr_res.json()
    assert "user_id" in data
    assert "export_timestamp" in data
