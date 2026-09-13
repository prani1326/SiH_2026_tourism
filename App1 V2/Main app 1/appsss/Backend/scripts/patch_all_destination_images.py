import os
import sys
import json
import logging

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.firestore_db import FirestoreManager

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("patch_destinations")

DESTINATION_METADATA = {
    "Agra": {
        "state": "Uttar Pradesh",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Taj Mahal, Agra Fort, Mughal architecture & Petha",
        "tags": ["Heritage", "Culture", "Monuments", "Popular"]
    },
    "Leh-Ladakh": {
        "state": "Ladakh",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1581793745862-99fde7fa73d2?auto=format&fit=crop&w=1200&q=80",
        "known_for": "High-altitude desert, Pangong Lake, monasteries & biking expeditions",
        "tags": ["Adventure", "Mountains", "Nature", "Popular"]
    },
    "Delhi": {
        "state": "Delhi",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1587474260584-136574528ed5?auto=format&fit=crop&w=1200&q=80",
        "known_for": "India Gate, Red Fort, street food, bazaars & historic monuments",
        "tags": ["Heritage", "Culture", "Food", "Shopping", "Popular"]
    },
    "Mumbai": {
        "state": "Maharashtra",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1570168007204-dfb528c6958f?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Gateway of India, Marine Drive, Bollywood, street food & nightlife",
        "tags": ["Beaches", "Nightlife", "Culture", "Food", "Popular"]
    },
    "Jaipur": {
        "state": "Rajasthan",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1477587458883-47145ed94245?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Hawa Mahal, Amber Fort, City Palace & royal Rajasthani heritage",
        "tags": ["Heritage", "Culture", "Shopping", "Food", "Popular"]
    },
    "Goa": {
        "state": "Goa",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Sun-kissed beaches, watersports, nightlife, Portuguese churches & seafood",
        "tags": ["Beaches", "Nightlife", "Adventure", "Food", "Popular"]
    },
    "Varanasi": {
        "state": "Uttar Pradesh",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1561361513-2d000a50f0dc?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Ganga Aarti, Dashashwamedh Ghat, Kashi Vishwanath & spiritual boat rides",
        "tags": ["Spiritual", "Heritage", "Culture", "Food", "Popular"]
    },
    "Udaipur": {
        "state": "Rajasthan",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80",
        "known_for": "City Palace, Lake Pichola, romantic boat rides & Rajput grandeur",
        "tags": ["Heritage", "Culture", "Romantic", "Food", "Popular"]
    },
    "Manali": {
        "state": "Himachal Pradesh",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1605649487212-47bdab064df7?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Solang Valley, snow adventures, Rohtang Pass, trekking & pine forests",
        "tags": ["Mountains", "Adventure", "Nature", "Popular"]
    },
    "Rishikesh": {
        "state": "Uttarakhand",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&w=1200&q=80",
        "known_for": "White-water rafting, yoga ashrams, Ganga Aarti & bungee jumping",
        "tags": ["Spiritual", "Adventure", "Mountains", "Popular"]
    },
    "Shimla": {
        "state": "Himachal Pradesh",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1597074866923-dc0589150358?auto=format&fit=crop&w=1200&q=80",
        "known_for": "The Ridge, Mall Road, colonial architecture & scenic toy train rides",
        "tags": ["Mountains", "Heritage", "Adventure", "Popular"]
    },
    "Kochi": {
        "state": "Kerala",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Chinese fishing nets, Fort Kochi, spice markets & backwater cruises",
        "tags": ["Heritage", "Culture", "Beaches", "Food", "Popular"]
    },
    "Munnar": {
        "state": "Kerala",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1590050752117-238cb0fb12b1?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Sprawling tea gardens, mist-covered hills, waterfalls & Eravikulam National Park",
        "tags": ["Nature", "Mountains", "Romantic", "Popular"]
    },
    "Coorg": {
        "state": "Karnataka",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1593693397690-362cb9666fc2?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Coffee plantations, Abbey Falls, misty Western Ghats & Kodava hospitality",
        "tags": ["Nature", "Mountains", "Relaxation", "Popular"]
    },
    "Bangalore": {
        "state": "Karnataka",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1596176530529-78163a4f7af2?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Silicon Valley of India, Lalbagh, craft breweries, cafés & pleasant climate",
        "tags": ["Food", "Culture", "Nightlife", "Shopping", "Popular"]
    },
    "Hyderabad": {
        "state": "Telangana",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1605379399642-870262d3d051?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Charminar, Golconda Fort, world-famous Hyderabadi Biryani & pearl bazaars",
        "tags": ["Heritage", "Food", "Culture", "Shopping", "Popular"]
    },
    "Kolkata": {
        "state": "West Bengal",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1558431382-27e303142255?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Victoria Memorial, Howrah Bridge, vintage trams, Durga Puja & delicious sweets",
        "tags": ["Culture", "Heritage", "Food", "Shopping", "Popular"]
    },
    "Chennai": {
        "state": "Tamil Nadu",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Marina Beach, Kapaleeshwarar Temple, Carnatic music & authentic South Indian filter coffee",
        "tags": ["Beaches", "Spiritual", "Heritage", "Culture", "Food"]
    },
    "Amritsar": {
        "state": "Punjab",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1514222134-b57cbb8ce073?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Harmandir Sahib (Golden Temple), Jallianwala Bagh, Wagah Border & Amritsari Kulcha",
        "tags": ["Spiritual", "Heritage", "Culture", "Food", "Popular"]
    },
    "Jodhpur": {
        "state": "Rajasthan",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1598971861713-54ad16a7e72e?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Mehrangarh Fort, Blue City painted houses, Umaid Bhawan & desert cuisine",
        "tags": ["Heritage", "Culture", "Shopping", "Food", "Popular"]
    },
    "Mysuru": {
        "state": "Karnataka",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1580835239846-5bb9ce03c8c3?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Grand Mysore Palace, Chamundi Hills, silk sarees, sandalwood & Mysore Pak",
        "tags": ["Heritage", "Culture", "Shopping", "Food", "Popular"]
    },
    "Darjeeling": {
        "state": "West Bengal",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&w=1200&q=80",
        "known_for": "World-famous tea gardens, Himalayan Toy Train, Kanchenjunga view & Tibetan cuisine",
        "tags": ["Mountains", "Nature", "Heritage", "Food", "Popular"]
    },
    "Puducherry": {
        "state": "Puducherry",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80",
        "known_for": "French colonial White Town, Promenade Beach, Auroville & seaside cafés",
        "tags": ["Beaches", "Culture", "Heritage", "Food", "Popular"]
    },
    "Hampi": {
        "state": "Karnataka",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1620766182966-c6eb5ed2b788?auto=format&fit=crop&w=1200&q=80",
        "known_for": "UNESCO Vijayanagara ruins, Stone Chariot, Virupaksha Temple & boulder landscape",
        "tags": ["Heritage", "Culture", "Monuments", "Popular"]
    },
    "Pushkar": {
        "state": "Rajasthan",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1509749837427-ac94a2553d0e?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Sacred Pushkar Lake, Brahma Temple, camel fair & desert camping",
        "tags": ["Spiritual", "Heritage", "Culture", "Popular"]
    },
    "Khajuraho": {
        "state": "Madhya Pradesh",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1608958435020-e8a7109ba809?auto=format&fit=crop&w=1200&q=80",
        "known_for": "UNESCO world heritage temples famous for intricate stone sculptures & Nagara architecture",
        "tags": ["Heritage", "Culture", "Monuments", "Popular"]
    },
    "Gangtok": {
        "state": "Sikkim",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1589182373726-e4f658ab50f0?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Rumtek Monastery, Nathula Pass, Tsomgo Lake & Kanchenjunga views",
        "tags": ["Mountains", "Nature", "Spiritual", "Popular"]
    },
    "Mahabalipuram": {
        "state": "Tamil Nadu",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1609137144813-7d9921338f24?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Shore Temple, Pancha Rathas, rock-cut cave monuments & seaside heritage",
        "tags": ["Heritage", "Culture", "Beaches", "Popular"]
    },
    "Mount Abu": {
        "state": "Rajasthan",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1548013146-72479768bada?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Dilwara Jain Temples, Nakki Lake, Guru Shikhar peak & Aravalli hill retreat",
        "tags": ["Mountains", "Heritage", "Spiritual", "Popular"]
    },
    "Ooty": {
        "state": "Tamil Nadu",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1589182373726-e4f658ab50f0?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Nilgiri Mountain Toy Train, Ooty Lake, botanical gardens & tea estates",
        "tags": ["Mountains", "Nature", "Romantic", "Popular"]
    },
    "Port Blair": {
        "state": "Andaman and Nicobar Islands",
        "country": "India",
        "image": "https://images.unsplash.com/photo-1589308078059-be1415eab4c3?auto=format&fit=crop&w=1200&q=80",
        "known_for": "Cellular Jail, Radhanagar Beach, turquoise waters, scuba diving & coral reefs",
        "tags": ["Beaches", "Adventure", "Heritage", "Popular"]
    }
}

def get_best_meta(name: str):
    for key, meta in DESTINATION_METADATA.items():
        if key.lower() in name.lower() or name.lower() in key.lower():
            return meta
    return None

def patch_local_file(filepath: str):
    if not os.path.exists(filepath):
        return
    logger.info(f"Patching local file: {filepath}")
    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)
    
    dests = data.get("destinations", {})
    updated_count = 0
    for doc_id, item in dests.items():
        name = item.get("name", "")
        meta = get_best_meta(name)
        if meta:
            item["hero_image_url"] = meta["image"]
            item["cover_image"] = meta["image"]
            item["image"] = meta["image"]
            item["state"] = meta["state"]
            item["country"] = meta["country"]
            if meta.get("known_for"):
                item["known_for"] = meta["known_for"]
            updated_count += 1
    
    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    logger.info(f"Updated {updated_count} destinations in {filepath}")

def patch_live_firestore():
    logger.info("Patching live Firebase Firestore collection 'destinations'...")
    fs = FirestoreManager.get_instance().client
    docs = list(fs.collection("destinations").stream())
    logger.info(f"Found {len(docs)} documents in live Firestore.")
    
    patched = 0
    for doc in docs:
        d_dict = doc.to_dict()
        name = d_dict.get("name", "")
        meta = get_best_meta(name)
        if meta:
            update_payload = {
                "hero_image_url": meta["image"],
                "cover_image": meta["image"],
                "image": meta["image"],
                "state": meta["state"],
                "country": meta["country"],
                "known_for": meta.get("known_for", d_dict.get("known_for", ""))
            }
            fs.collection("destinations").document(doc.id).set(update_payload, merge=True)
            patched += 1
            logger.info(f"Live Updated: {doc.id} ({name}) -> {meta['state']} | {meta['image']}")
    logger.info(f"Successfully patched {patched} documents in live Firestore.")

if __name__ == "__main__":
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    patch_local_file(os.path.join(base_dir, "firestore_local_store.json"))
    patch_local_file(os.path.join(base_dir, "backups", "firestore_local_store_backup.json"))
    try:
        patch_live_firestore()
    except Exception as e:
        logger.error(f"Error during live Firestore patch: {e}")
