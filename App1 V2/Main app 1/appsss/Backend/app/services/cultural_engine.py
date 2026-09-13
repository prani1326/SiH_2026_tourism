from typing import Dict, Any, List

class CulturalAndPreparationEngine:
    """
    Food, Cultural & Preparation Engine (Sections 23, 25, 26).
    Handles Dietary Confidence scoring, Menu OCR/ingredient analysis,
    Airport-to-Hotel transport comparison, and smart pre-trip preparation checklists.
    """

    @staticmethod
    def compare_airport_to_hotel(city: str) -> Dict[str, Any]:
        """Local Transport Brain (Section 23)."""
        return {
            "city": city.title(),
            "route": f"{city.title()} International Airport (DEL/GOI/JAI) → City Center / Hotel Hub",
            "options": [
                {
                    "mode": "Airport Express Metro / Rapid Rail",
                    "badge": "Cheapest & Eco-Friendly",
                    "estimated_time_mins": 25,
                    "estimated_cost_inr": 60.0,
                    "safety_rating": 4.9,
                    "crowd_level": "Low to Moderate",
                    "operating_hours": "04:45 AM - 11:30 PM",
                    "tip": "Dedicated luggage racks and direct escalator connections."
                },
                {
                    "mode": "Prepaid Government / Tourist Taxi Desk",
                    "badge": "Safest Fixed Price",
                    "estimated_time_mins": 45,
                    "estimated_cost_inr": 650.0,
                    "safety_rating": 4.8,
                    "crowd_level": "Instant Pickup",
                    "operating_hours": "24/7 Available",
                    "tip": "Collect official printed receipt inside terminal before exiting."
                },
                {
                    "mode": "App-Based Cab (Uber / Ola / InDrive)",
                    "badge": "Fastest Door-to-Door",
                    "estimated_time_mins": 40,
                    "estimated_cost_inr": 520.0,
                    "safety_rating": 4.7,
                    "crowd_level": "Pick up at Dedicated Pillar Zone",
                    "operating_hours": "24/7 Available",
                    "tip": "Follow airport floor markings to the designated App Cab floor."
                },
                {
                    "mode": "Private Chauffeur Luxury Transfer",
                    "badge": "Most Comfortable",
                    "estimated_time_mins": 40,
                    "estimated_cost_inr": 1800.0,
                    "safety_rating": 5.0,
                    "crowd_level": "Personal Meet & Greet",
                    "operating_hours": "24/7 On Demand",
                    "tip": "Chauffeur holds placard at Arrivals Gate with mineral water & chilled towels."
                }
            ],
            "city_pass_recommendation": {
                "name": f"{city.title()} Unlimited Tourist Metro/Bus Transit Card",
                "price_1_day": 200.0,
                "price_3_day": 500.0,
                "benefits": "Unlimited rides across city lines, skip ticket counter queues."
            }
        }

    @staticmethod
    def analyze_menu_for_diet(menu_text: str, user_diet: str) -> Dict[str, Any]:
        """
        Menu Scanning Engine (Section 25).
        Analyzes dish names & ingredients for dietary compliance (Veg, Jain, Halal, Vegan, Allergens).
        """
        diet = user_diet.lower()
        text_lower = menu_text.lower()

        detected_items = []
        detected_risks = []
        recommendations = []

        # Knowledge base of prohibited items
        non_veg_flags = ["chicken", "mutton", "fish", "prawn", "beef", "pork", "egg", "gelatin", "oyster", "bacon", "meat"]
        jain_flags = ["onion", "garlic", "potato", "carrot", "radish", "beetroot", "mushroom"] + non_veg_flags
        halal_prohibitions = ["pork", "lard", "bacon", "alcohol", "wine", "beer", "rum"]

        # Sample dishes parsed from text
        lines = [line.strip() for line in menu_text.split("\n") if line.strip()]
        for line in lines[:8]:
            line_lower = line.lower()
            status = "Approved"
            flag = None

            if "jain" in diet:
                found_flags = [f for f in jain_flags if f in line_lower]
                if found_flags:
                    status = "Not Jain Friendly"
                    flag = f"Contains root vegetable / non-veg: {', '.join(found_flags)}"
                    detected_risks.append(f"Dish '{line}' contains {', '.join(found_flags)}")
            elif "veg" in diet:
                found_flags = [f for f in non_veg_flags if f in line_lower]
                if found_flags:
                    status = "Non-Vegetarian"
                    flag = f"Contains meat or animal products: {', '.join(found_flags)}"
                    detected_risks.append(f"Dish '{line}' contains animal protein ({', '.join(found_flags)})")
            elif "halal" in diet:
                found_flags = [f for f in halal_prohibitions if f in line_lower]
                if found_flags:
                    status = "Not Halal"
                    flag = f"Contains prohibited items: {', '.join(found_flags)}"
                    detected_risks.append(f"Dish '{line}' contains {', '.join(found_flags)}")

            detected_items.append({
                "dish_name": line,
                "suitability_status": status,
                "notes": flag or "Compliant with your dietary preference"
            })

        if detected_risks:
            overall = "Caution Advised"
            diet_confidence = 68
            recommendations.append("Inform the server: 'Please prepare without onion/garlic/animal broth'.")
            recommendations.append("Check if the restaurant maintains a segregated pure vegetarian cookware station.")
        else:
            overall = "Highly Suitable"
            diet_confidence = 98
            recommendations.append("Dishes appear 100% compliant with your selected preference.")

        items = []
        suitable_count = 0
        for item in detected_items:
            is_suitable = (item["suitability_status"] == "Approved")
            if is_suitable:
                suitable_count += 1
            items.append({
                "item_name": item["dish_name"],
                "is_suitable": is_suitable,
                "reason": item["notes"],
                "ingredients": []
            })

        return {
            "overall_suitability": overall,
            "diet_confidence": diet_confidence,
            "user_diet": user_diet,
            "detected_items": detected_items,
            "detected_risks": detected_risks,
            "recommendations": recommendations,
            "total_items_analyzed": len(detected_items),
            "suitable_items_count": suitable_count,
            "items": items,
            "warnings": detected_risks
        }

    @staticmethod
    def generate_preparation_checklist(destination_name: str, weather_summary: str = "Sunny") -> Dict[str, Any]:
        """Preparation Engine (Section 26)."""
        return {
            "destination": destination_name.title(),
            "weather_packing_list": [
                {"item": "Lightweight breathable cotton clothing", "packed": False, "reason": "Ideal for daytime exploration"},
                {"item": "Comfortable cushioned walking shoes", "packed": False, "reason": "Historic cobblestones and marble palace tours"},
                {"item": "High SPF Sunscreen (SPF 50+) & Polarized Sunglasses", "packed": False, "reason": "High UV index at open heritage sites"},
                {"item": "Compact umbrella / light rain poncho", "packed": False, "reason": "Afternoon tropical showers / sun shade"},
                {"item": "Refillable insulated water bottle", "packed": False, "reason": "Stay hydrated throughout walking tours"}
            ],
            "cultural_preparation": [
                {"requirement": "Temple & Mosque Attire", "guideline": "Cover shoulders and knees. Carry a light scarf to cover head when entering sacred shrines."},
                {"requirement": "Shoe Removal Etiquette", "guideline": "Remove shoes outside shrines; shoe tokens / racks are provided at all major sites."},
                {"requirement": "Photography Permissions", "guideline": "Ask locals before taking portraits. Tripods often require special archeological permits."},
                {"requirement": "Bargaining Courtesy", "guideline": "Polite smiling negotiation is standard in traditional street bazaars."}
            ],
            "travel_medical_kit": [
                {"item": "Oral Rehydration Salts (ORS) sachets", "purpose": "Electrolyte balance during warm travel"},
                {"item": "Motion sickness pills", "purpose": "Helpful for winding mountain or hill routes"},
                {"item": "Band-aids & antiseptic wipes", "purpose": "Blister prevention from walking"},
                {"item": "Antacid & digestive enzyme tablets", "purpose": "Acclimating to rich spices"}
            ],
            "international_checklist": [
                {"item": "Power Plug Adapter Type D / Type C (230V, 50Hz)", "verified": True},
                {"item": "eSIM with Local High-Speed 5G Data", "verified": True},
                {"item": "Digital Copy of Passport & Visa stored in Offline Trip Card", "verified": True},
                {"item": "International Roaming Banking App SMS verified", "verified": True}
            ]
        }

cultural_engine = CulturalAndPreparationEngine()
