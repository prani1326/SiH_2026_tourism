import sys, json
sys.path.insert(0, '.')
from scripts.seed_destinations import DESTINATIONS_DATA

def esc(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')

with open('destinations_kotlin.txt', 'w', encoding='utf-8') as out:
    out.write("        val initialList = listOf(\n")
    for d in DESTINATIONS_DATA:
        tags_str = ", ".join([f'"{esc(t)}"' for t in d.get('tags', [])])
        attr_str = ", ".join([f'"{esc(a)}"' for a in d.get('top_attractions', [])])
        act_str = ", ".join([f'"{esc(a)}"' for a in d.get('activities', [])])
        food_str = ", ".join([f'"{esc(f)}"' for f in d.get('famous_food', [])])
        trans_str = ", ".join([f'"{esc(t)}"' for t in d.get('local_transport', [])])
        desc_clean = esc(d.get('description', ''))
        known_clean = esc(d.get('known_for', ''))
        
        out.write(f'''            DestinationDto(
                id = "{d['id']}",
                name = "{d['name']}",
                state = "{d['state']}",
                country = "{d.get('country', 'India')}",
                latitude = {d.get('latitude', 0.0)},
                longitude = {d.get('longitude', 0.0)},
                heroImageUrl = "{d.get('hero_image_url', '')}",
                description = "{desc_clean}",
                knownFor = "{known_clean}",
                rating = {d.get('rating', 4.8)},
                bestTimeToVisit = "{d.get('best_time_to_visit', 'October to March')}",
                idealStay = "{d.get('ideal_stay', '2-4 days')}",
                budgetPerDay = "{d.get('budget_per_day', '₹3,000 - ₹6,000')}",
                estimatedBudgetTier = "{d.get('estimated_budget_tier', 'Moderate')}",
                weatherTemperature = "{d.get('weather_temperature', '25°C')}",
                weatherCondition = "{d.get('weather_condition', 'Pleasant')}",
                safetyScore = {d.get('safety_score', 92)},
                tags = listOf({tags_str}),
                topAttractions = listOf({attr_str}),
                activities = listOf({act_str}),
                famousFood = listOf({food_str}),
                localTransport = listOf({trans_str}),
                isFeatured = {str(d.get('is_featured', True)).lower()},
                isPopular = {str(d.get('is_popular', True)).lower()}
            ),\n''')
    out.write("        )\n")
print("Done generating destinations_kotlin.txt")
