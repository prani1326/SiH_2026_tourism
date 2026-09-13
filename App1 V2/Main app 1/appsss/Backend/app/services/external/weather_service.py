import json
import logging
import httpx
from typing import Dict, Any, Optional
from app.core.config import settings
from app.core.redis import redis_client

logger = logging.getLogger("tourist_app.weather")

class WeatherService:
    """
    Live Weather & Forecast service with Redis caching.
    """

    def __init__(self):
        self.api_key = settings.OPENWEATHER_API_KEY
        self.base_url = "https://api.openweathermap.org/data/2.5"

    def get_destination_weather(self, city_or_dest: str, latitude: float = 0.0, longitude: float = 0.0) -> Dict[str, Any]:
        """
        Fetches current weather condition, temperature, and advice with 1-hour Redis cache.
        """
        cache_key = f"weather:current:{city_or_dest.strip().lower()}"
        cached = redis_client.get_cache(cache_key)
        if cached:
            try:
                return json.loads(cached)
            except Exception:
                pass

        if self.api_key:
            try:
                with httpx.Client(timeout=4.0) as client:
                    params = {"appid": self.api_key, "units": "metric"}
                    if latitude and longitude:
                        params.update({"lat": latitude, "lon": longitude})
                    else:
                        params["q"] = city_or_dest

                    resp = client.get(f"{self.base_url}/weather", params=params)
                    if resp.status_code == 200:
                        data = resp.json()
                        temp = round(data.get("main", {}).get("temp", 28))
                        condition = data.get("weather", [{}])[0].get("main", "Clear")
                        desc = data.get("weather", [{}])[0].get("description", "pleasant")
                        humidity = data.get("main", {}).get("humidity", 50)
                        
                        res = {
                            "destination": city_or_dest,
                            "temperature_celsius": temp,
                            "condition": condition,
                            "description": desc.title(),
                            "humidity_pct": humidity,
                            "is_outdoor_suitable": condition not in ["Rain", "Thunderstorm", "Extreme"],
                            "traveler_advice": "Ideal for monuments and sightseeing." if condition == "Clear" else "Carry an umbrella and light waterproof gear.",
                            "is_simulated": False
                        }
                        redis_client.set_cache(cache_key, json.dumps(res), ttl_seconds=3600)
                        return res
            except Exception as exc:
                logger.warning(f"OpenWeather API call failed: {exc}. Using baseline data.")

        # Baseline weather estimation for Indian destinations
        res = {
            "destination": city_or_dest,
            "temperature_celsius": 28,
            "condition": "Pleasant & Clear",
            "description": "Sunny with clear skies",
            "humidity_pct": 45,
            "is_outdoor_suitable": True,
            "traveler_advice": "Comfortable weather for heritage walks and photography.",
            "is_simulated": True
        }
        redis_client.set_cache(cache_key, json.dumps(res), ttl_seconds=3600)
        return res

weather_service = WeatherService()
