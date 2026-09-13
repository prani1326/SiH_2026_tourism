from typing import Dict, List, Any, Optional

class LocalExperienceService:
    """
    Curates verified local community experiences, heritage walks, artisan workshops,
    and traditional home kitchens ensuring authentic cultural immersion.
    """

    EXPERIENCES: List[Dict[str, Any]] = [
        {
            "id": "exp_bagru_block_print",
            "destination": "Jaipur",
            "title": "Bagru Natural Dye & Hand Block Printing Workshop",
            "category": "Handicrafts & Art",
            "host_name": "Master Artisan Ramkishan Chippa",
            "verified_badge": True,
            "rating": 4.9,
            "reviews_count": 128,
            "price_inr": 850,
            "duration_hours": 3.0,
            "location": "Bagru Artisan Village, 22 km from Jaipur",
            "languages": ["Hindi", "English"],
            "accessibility": "Step-free workshop floor, seated activity",
            "eco_score": 98,
            "description": "Learn the 350-year-old traditional hand block printing technique using natural mud-resist (Dabu) and vegetable dyes. Take home your custom printed scarf.",
            "image_url": "https://images.unsplash.com/photo-1605809705973-c60317e3be9f?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "exp_blue_pottery",
            "destination": "Jaipur",
            "title": "Traditional Jaipur Blue Pottery Masterclass",
            "category": "Handicrafts & Art",
            "host_name": "Kripal Kumbh Studio",
            "verified_badge": True,
            "rating": 4.8,
            "reviews_count": 94,
            "price_inr": 1200,
            "duration_hours": 2.5,
            "location": "Bani Park, Jaipur",
            "languages": ["Hindi", "English"],
            "accessibility": "Wheelchair accessible studio",
            "eco_score": 92,
            "description": "Discover the Persian-origin art of quartz-based blue pottery with cobalt oxide pigment glazing.",
            "image_url": "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "exp_heritage_food_walk",
            "destination": "Jaipur",
            "title": "Old Pink City Morning Culinary & Heritage Walk",
            "category": "Food & Culture",
            "host_name": "Jaipur Virasat Food Collective",
            "verified_badge": True,
            "rating": 5.0,
            "reviews_count": 210,
            "price_inr": 750,
            "duration_hours": 2.0,
            "location": "Starting at Hawa Mahal Courtyard",
            "languages": ["Hindi", "English", "Marwari"],
            "accessibility": "Gentle 1.5 km walking route with shaded rest stops",
            "eco_score": 95,
            "description": "Taste authentic Pyaaz Kachori, century-old Rabri Ghevar, and kulhad lassi while walking through 18th-century bazaar lanes.",
            "image_url": "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=600&q=80"
        },
        {
            "id": "exp_shekhawati_fresco",
            "destination": "Jaipur",
            "title": "Havelis Fresco Painting & Restoration Walk",
            "category": "Heritage & History",
            "host_name": "Heritage Conservation Trust",
            "verified_badge": True,
            "rating": 4.7,
            "reviews_count": 65,
            "price_inr": 600,
            "duration_hours": 2.5,
            "location": "Chandpole Heritage Precinct, Jaipur",
            "languages": ["English", "Hindi"],
            "accessibility": "Step-free main courtyard access",
            "eco_score": 90,
            "description": "Guided walking exploration of restored 19th-century Marwari merchant havelis with intricate mineral-pigment frescoes.",
            "image_url": "https://images.unsplash.com/photo-1599661046289-e31897846e41?auto=format&fit=crop&w=600&q=80"
        }
    ]

    def get_experiences(self, destination: Optional[str] = None, category: Optional[str] = None) -> List[Dict[str, Any]]:
        results = self.EXPERIENCES
        if destination:
            results = [e for e in results if destination.lower() in e["destination"].lower()]
        if category:
            results = [e for e in results if category.lower() in e["category"].lower()]
        return results

    def get_experience_by_id(self, exp_id: str) -> Optional[Dict[str, Any]]:
        for e in self.EXPERIENCES:
            if e["id"] == exp_id:
                return e
        return self.EXPERIENCES[0]

local_experience_service = LocalExperienceService()
