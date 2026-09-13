import json
import uuid
import datetime
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.firestore_db import get_firestore
from app.repositories.destination_repo import destination_repo
from app.repositories.trip_repo import trip_repo
from app.repositories.user_repo import user_repo
from app.models.firestore_models import Destination, Trip, User, UserProfile, PlaceAttraction, EmergencyContact
from scripts.seed_destinations import DESTINATIONS_JSON

def seed_firestore():
    """
    Seeds baseline demo destinations, places, trips, emergency contacts, and demo users directly into Firestore.
    Safe and idempotent.
    """
    firestore = get_firestore()
    is_mock = hasattr(firestore, "auto_persist")
    if is_mock:
        firestore.auto_persist = False

    try:
        # Fast path: check if baseline data already exists to avoid redundant slow network roundtrips on startup
        if not is_mock and destination_repo.get_all(limit=1):
            print("[Firestore Seed] Baseline destinations already seeded. Skipping startup seed.")
            return

        print("Checking & seeding 20 canonical Indian destinations into Firestore...")
        from scripts.seed_destinations import DESTINATIONS_DATA

        for data in DESTINATIONS_DATA:
            dest_id = data["id"]
            existing_dest = destination_repo.get_by_id(dest_id)
            
            dest_kwargs = {
                "id": dest_id,
                "name": data["name"],
                "state": data["state"],
                "country": data["country"],
                "latitude": data["latitude"],
                "longitude": data["longitude"],
                "hero_image_url": data["hero_image_url"],
                "description": data["description"],
                "known_for": data["known_for"],
                "top_attractions": data["top_attractions"],
                "activities": data["activities"],
                "best_time_to_visit": data["best_time_to_visit"],
                "ideal_stay": data["ideal_stay"],
                "budget_per_day": data["budget_per_day"],
                "estimated_budget_tier": data["estimated_budget_tier"],
                "weather_temperature": data["weather_temperature"],
                "weather_condition": data["weather_condition"],
                "famous_food": data["famous_food"],
                "local_transport": data["local_transport"],
                "nearby_places": data["nearby_places"],
                "safety_score": data["safety_score"],
                "rating": data["rating"],
                "tags": data["tags"],
                "is_popular": data["is_popular"],
                "is_featured": data["is_featured"]
            }

            dest = Destination(**dest_kwargs)
            if existing_dest:
                print(f"Updating destination: {dest_id}")
                destination_repo.update(dest_id, dest_kwargs)
            else:
                print(f"Creating canonical destination: {dest_id}")
                destination_repo.create(dest)

            # Ensure demo trips exist for demo-traveler-touristapp
            trip_id = f"trip-{data['name'].lower().replace(' ', '-')}"
            if not trip_repo.get_by_id(trip_id):
                today = datetime.date.today()
                top_attractions = data.get("top_attractions", [])
                famous_food = data.get("famous_food", [])

                activities = []
                if len(top_attractions) > 0:
                    activities.append({"id": str(uuid.uuid4()), "title": f"Visit {top_attractions[0]}", "time_slot": "Morning", "estimated_cost": 250.0})
                if len(top_attractions) > 1:
                    activities.append({"id": str(uuid.uuid4()), "title": f"Explore {top_attractions[1]}", "time_slot": "Afternoon", "estimated_cost": 300.0})
                if len(famous_food) > 0:
                    activities.append({"id": str(uuid.uuid4()), "title": f"Dinner with {famous_food[0]}", "time_slot": "Evening", "estimated_cost": 450.0})

                trip = Trip(
                    id=trip_id,
                    user_id="demo-traveler-touristapp",
                    destination_id=dest_id,
                    destination_name=dest.name,
                    title=f"Explore {dest.name}",
                    start_date=str(today),
                    end_date=str(today + datetime.timedelta(days=2)),
                    status="planning",
                    total_budget=15000.0,
                    actual_spend=0.0,
                    days=[
                        {
                            "id": str(uuid.uuid4()),
                            "day_number": 1,
                            "date": str(today),
                            "notes": "Arrival & Highlights",
                            "activities": activities
                        }
                    ]
                )
                trip_repo.create(trip)

        # Seed places / attractions if missing
        places_docs = firestore.collection("places").get()
        if len(places_docs) < 5:
            print("Seeding demo places and attractions...")
            all_destinations = destination_repo.get_all(limit=10)
            for d in all_destinations:
                attractions = getattr(d, "top_attractions", []) or ["City Center", "Historic Monument", "Local Market"]
                for idx, attr_name in enumerate(attractions[:5], start=1):
                    pid = f"place-{d.id}-{idx}"
                    place = PlaceAttraction(
                        id=pid,
                        destination_id=d.id,
                        name=attr_name,
                        category="Monument" if idx % 2 != 0 else "Experience",
                        subcategory="Historical Site" if idx % 2 != 0 else "Cultural Tour",
                        cover_image="https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=800&q=80",
                        description=f"Iconic attraction in {d.name} known for heritage and remarkable architecture.",
                        latitude=float(d.latitude or 20.0) + (idx * 0.005),
                        longitude=float(d.longitude or 77.0) + (idx * 0.005),
                        opening_hours="06:00 AM - 06:00 PM",
                        estimated_duration_hours=2.5,
                        entry_fee=500.0 if idx % 2 != 0 else 250.0,
                        rating=4.7,
                        reviews_count=1250,
                        safety_score=95,
                        confidence_score=96,
                        is_wheelchair_accessible=True,
                        diet_confidence=95,
                        transport_options=[
                            {"mode": "Auto", "price": 50.0},
                            {"mode": "Taxi", "price": 200.0},
                            {"mode": "Metro", "price": 30.0}
                        ]
                    )
                    firestore.collection("places").document(pid).set(place.to_dict())

        # Seed emergency contacts if missing
        ec_docs = firestore.collection("emergency_contacts").get()
        if len(ec_docs) < 2:
            print("Seeding demo emergency contacts...")
            demo_contacts = [
                EmergencyContact(
                    id="contact-police-112",
                    user_id="demo-traveler-touristapp",
                    name="Tourist Police Helpline",
                    relationship="Helpline",
                    phone="+91-112",
                    is_sos_contact=True
                ),
                EmergencyContact(
                    id="contact-family-1",
                    user_id="demo-traveler-touristapp",
                    name="Family Primary Contact",
                    relationship="Spouse",
                    phone="+91-9876543219",
                    is_sos_contact=True
                )
            ]
            for c in demo_contacts:
                firestore.collection("emergency_contacts").document(c.id).set(c.to_dict())

        # Seed demo users if not existing
        from app.core.security import hash_password
        users_to_seed = [
            {
                "id": "demo-traveler-touristapp",
                "email": "traveler@touristapp.com",
                "hashed_password": hash_password("Tourist@123"),
                "full_name": "Aarav Sharma",
                "phone": "+91-9876543210",
                "emergency_recovery_pin": "1234",
                "role": "tourist",
                "is_active": True,
                "is_verified": True,
                "profile": UserProfile(
                    nationality="India",
                    passport_country="IND",
                    preferred_currency="INR",
                    dietary_preferences=["Vegetarian"],
                    travel_style="Cultural"
                ).to_dict()
            },
            {
                "id": "demo-tourist-001",
                "email": "tourist.demo@example.com",
                "hashed_password": hash_password("DemoPassword123!"),
                "full_name": "Demo Traveler",
                "phone": "+91-9876543211",
                "emergency_recovery_pin": "1234",
                "role": "tourist",
                "is_active": True,
                "is_verified": True,
                "profile": UserProfile(
                    nationality="India",
                    passport_country="IND",
                    preferred_currency="INR",
                    dietary_preferences=["Vegetarian"],
                    travel_style="Cultural"
                ).to_dict()
            }
        ]

        demo_contacts_dicts = [
            {
                "id": "contact-police-112",
                "user_id": "demo-traveler-touristapp",
                "name": "Tourist Police Helpline",
                "relationship": "Helpline",
                "phone": "+91-112",
                "is_sos_contact": True
            },
            {
                "id": "contact-family-1",
                "user_id": "demo-traveler-touristapp",
                "name": "Family Primary Contact",
                "relationship": "Spouse",
                "phone": "+91-9876543219",
                "is_sos_contact": True
            }
        ]

        for u in users_to_seed:
            u["emergency_contacts"] = demo_contacts_dicts
            existing_u = user_repo.get_by_email(u["email"])
            if not existing_u:
                user_repo.create(User(**u))
                print(f"Created demo user {u['email']}")
            else:
                user_repo.update(existing_u.id, {
                    "hashed_password": u["hashed_password"],
                    "emergency_recovery_pin": u["emergency_recovery_pin"],
                    "emergency_contacts": demo_contacts_dicts,
                    "is_active": True,
                    "is_verified": True
                })

        # Ensure demo trips exist for demo-traveler-touristapp
        user_trips = trip_repo.get_user_trips("demo-traveler-touristapp")
        if len(user_trips) == 0:
            print("Seeding demo trips for demo-traveler-touristapp...")
            today = datetime.date.today()
            agra_dest = destination_repo.search(query="Agra", limit=1)
            dest_id = agra_dest[0].id if agra_dest else "dest-agra"
            dest_name = agra_dest[0].name if agra_dest else "Agra"
            demo_trip = Trip(
                id="trip-agra-demo",
                user_id="demo-traveler-touristapp",
                destination_id=str(dest_id),
                destination_name=dest_name,
                title=f"Explore {dest_name}",
                start_date=str(today),
                end_date=str(today + datetime.timedelta(days=3)),
                status="planning",
                total_budget=15000.0,
                actual_spend=0.0,
                days=[
                    {
                        "id": "day-agra-1",
                        "day_number": 1,
                        "date": str(today),
                        "notes": "Arrival & Taj Mahal Visit",
                        "activities": [
                            {"id": "act-1", "trip_day_id": "day-agra-1", "title": "Visit Taj Mahal", "time_slot": "Morning", "estimated_cost": 500.0, "sequence_order": 1},
                            {"id": "act-2", "trip_day_id": "day-agra-1", "title": "Agra Fort Heritage Walk", "time_slot": "Afternoon", "estimated_cost": 300.0, "sequence_order": 2}
                        ]
                    },
                    {
                        "id": "day-agra-2",
                        "day_number": 2,
                        "date": str(today + datetime.timedelta(days=1)),
                        "notes": "Mehtab Bagh & Local Crafts",
                        "activities": [
                            {"id": "act-3", "trip_day_id": "day-agra-2", "title": "Mehtab Bagh Sunset", "time_slot": "Evening", "estimated_cost": 200.0, "sequence_order": 1}
                        ]
                    }
                ]
            )
            trip_repo.create(demo_trip)
            print("Created demo trip for demo-traveler-touristapp")

    finally:
        if is_mock:
            firestore.auto_persist = True
            firestore._persist()

    print("Seeding to Firestore complete!")

if __name__ == "__main__":
    seed_firestore()
