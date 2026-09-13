import json
import logging
import math
import httpx
from typing import Dict, Any, Optional, Tuple
from app.core.config import settings
from app.core.redis import redis_client

logger = logging.getLogger("tourist_app.maps")

class MapsService:
    """
    Google Maps Platform integration service with Redis caching and fallback.
    - Geocoding & Reverse Geocoding
    - Distance Matrix (Travel duration & road distance between attractions)
    - Route ETA and Traffic Considerations
    """

    def __init__(self):
        self.api_key = settings.GOOGLE_MAPS_API_KEY
        self.base_url = "https://maps.googleapis.com/maps/api"

    def _haversine_distance(self, lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """Calculate great-circle distance in kilometers between two GPS coordinates."""
        R = 6371.0
        dlat = math.radians(lat2 - lat1)
        dlon = math.radians(lon2 - lon1)
        a = (math.sin(dlat / 2) ** 2 +
             math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2) ** 2)
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        return round(R * c, 2)

    def get_travel_distance_and_time(
        self,
        origin_lat: float,
        origin_lon: float,
        dest_lat: float,
        dest_lon: float,
        mode: str = "driving"
    ) -> Dict[str, Any]:
        """
        Calculates travel distance and duration between two coordinates.
        Uses 24-hour Redis caching to reduce external API costs.
        """
        cache_key = f"maps:dist:{origin_lat:.4f},{origin_lon:.4f}:{dest_lat:.4f},{dest_lon:.4f}:{mode}"
        cached = redis_client.get_cache(cache_key)
        if cached:
            try:
                return json.loads(cached)
            except Exception:
                pass

        if self.api_key:
            try:
                with httpx.Client(timeout=5.0) as client:
                    resp = client.get(
                        f"{self.base_url}/distancematrix/json",
                        params={
                            "origins": f"{origin_lat},{origin_lon}",
                            "destinations": f"{dest_lat},{dest_lon}",
                            "mode": mode,
                            "key": self.api_key
                        }
                    )
                    if resp.status_code == 200:
                        data = resp.json()
                        elements = data.get("rows", [{}])[0].get("elements", [{}])[0]
                        if elements.get("status") == "OK":
                            dist_meters = elements["distance"]["value"]
                            duration_seconds = elements["duration"]["value"]
                            res = {
                                "distance_km": round(dist_meters / 1000.0, 2),
                                "duration_minutes": max(1, round(duration_seconds / 60)),
                                "mode": mode,
                                "is_simulated": False
                            }
                            redis_client.set_cache(cache_key, json.dumps(res), ttl_seconds=86400)
                            return res
            except Exception as e:
                logger.warning(f"Google Maps API call failed: {e}. Using geometric fallback.")

        # Fallback haversine estimation: average speed 30 km/h in Indian cities
        km = self._haversine_distance(origin_lat, origin_lon, dest_lat, dest_lon)
        # Average speeds: walking: 4.5 km/h, driving: 25 km/h
        speed = 4.5 if mode == "walking" else 25.0
        minutes = max(5, int(round((km / speed) * 60)))
        res = {
            "distance_km": km,
            "duration_minutes": minutes,
            "mode": mode,
            "is_simulated": True
        }
        redis_client.set_cache(cache_key, json.dumps(res), ttl_seconds=86400)
        return res

    def geocode(self, address: str) -> Optional[Dict[str, Any]]:
        """Geocodes an address string to GPS coordinates."""
        if not address:
            return None
        cache_key = f"maps:geocode:{address.strip().lower()}"
        cached = redis_client.get_cache(cache_key)
        if cached:
            try:
                return json.loads(cached)
            except Exception:
                pass

        if self.api_key:
            try:
                with httpx.Client(timeout=5.0) as client:
                    resp = client.get(
                        f"{self.base_url}/geocode/json",
                        params={"address": address, "key": self.api_key}
                    )
                    if resp.status_code == 200:
                        data = resp.json()
                        results = data.get("results", [])
                        if results:
                            first = results[0]
                            location = first.get("geometry", {}).get("location", {})
                            res = {
                                "latitude": location.get("lat"),
                                "longitude": location.get("lng"),
                                "formatted_address": first.get("formatted_address"),
                                "place_id": first.get("place_id")
                            }
                            redis_client.set_cache(cache_key, json.dumps(res), ttl_seconds=86400)
                            return res
            except Exception as e:
                logger.warning(f"Google Maps geocoding API error: {e}")
        return None

    def search_places(self, query: str, lat: Optional[float] = None, lon: Optional[float] = None) -> List[Dict[str, Any]]:
        """Searches places via Google Places API."""
        if not query or not self.api_key:
            return []
        try:
            with httpx.Client(timeout=5.0) as client:
                params = {"query": query, "key": self.api_key}
                if lat is not None and lon is not None:
                    params["location"] = f"{lat},{lon}"
                    params["radius"] = "10000"
                resp = client.get(f"{self.base_url}/place/textsearch/json", params=params)
                if resp.status_code == 200:
                    data = resp.json()
                    results = []
                    for item in data.get("results", [])[:10]:
                        loc = item.get("geometry", {}).get("location", {})
                        results.append({
                            "name": item.get("name"),
                            "formatted_address": item.get("formatted_address"),
                            "latitude": loc.get("lat"),
                            "longitude": loc.get("lng"),
                            "rating": item.get("rating", 4.0),
                            "place_id": item.get("place_id")
                        })
                    return results
        except Exception as e:
            logger.warning(f"Google Places textsearch API error: {e}")
        return []

maps_service = MapsService()
