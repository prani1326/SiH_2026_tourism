import urllib.request
import json

def run_tests():
    # 1. Test popular destinations
    req = urllib.request.urlopen("http://127.0.0.1:8000/api/v1/destinations/popular")
    dests = json.loads(req.read().decode())
    print(f"Popular destinations count: {len(dests)}")
    for d in dests[:5]:
        print(f" - {d['id']}: {d['name']} ({d.get('known_for')}) Budget: {d.get('budget_per_day')}")

    # 2. Test destination detail for Goa
    req = urllib.request.urlopen("http://127.0.0.1:8000/api/v1/destinations/goa")
    goa = json.loads(req.read().decode())
    print(f"\nGoa Detail:")
    print(f" Name: {goa['name']}")
    print(f" Known for: {goa.get('known_for')}")
    print(f" Top Attractions: {goa.get('top_attractions')}")
    print(f" Famous Food: {goa.get('famous_food')}")
    print(f" Local Transport: {goa.get('local_transport')}")

    # 3. Test search
    req = urllib.request.urlopen("http://127.0.0.1:8000/api/v1/search/?q=Goa")
    search_res = json.loads(req.read().decode())
    print(f"\nSearch results for 'Goa': {len(search_res)} items found.")

    # 4. Test AI Planner
    ai_payload = json.dumps({
        "destination": "Goa",
        "start_date": "2026-10-01",
        "end_date": "2026-10-03",
        "traveler_count": 2,
        "budget": 30000.0,
        "currency": "INR",
        "travel_style": "Couple",
        "interests": ["Beaches", "Adventure", "Food"],
        "food_preference": "Vegetarian",
        "walking_tolerance": "Moderate",
        "pace": "Moderate"
    }).encode("utf-8")
    req = urllib.request.Request("http://127.0.0.1:8000/api/v1/trips/ai-plan", data=ai_payload, headers={"Content-Type": "application/json"})
    ai_res = json.loads(urllib.request.urlopen(req).read().decode())
    print(f"\nAI Planner Test:")
    print(f" Trip ID: {ai_res.get('trip_id')}")
    print(f" Title: {ai_res.get('title')}")
    print(f" Duration: {ai_res.get('duration_days')} days")
    print(f" Days count: {len(ai_res.get('days', []))}")
    if ai_res.get("days"):
        d1 = ai_res["days"][0]
        print(f" Day 1 activities: {[a['title'] for a in d1.get('activities', [])]}")

if __name__ == "__main__":
    run_tests()
