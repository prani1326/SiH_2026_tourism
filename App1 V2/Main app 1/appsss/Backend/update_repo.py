with open('c:/Users/97018/OneDrive/Desktop/version 2/App1 V2/Main app 1/appsss/frontend torist/app/src/main/java/com/touristapp/data/repository/DestinationRepository.kt', 'r', encoding='utf-8') as f:
    repo_code = f.read()

target = '    private suspend fun seedDefaultDestinations() {'
idx = repo_code.find(target)
if idx == -1:
    raise Exception("Could not find seedDefaultDestinations")

prefix = repo_code[:idx]

with open('destinations_kotlin.txt', 'r', encoding='utf-8') as f:
    dests_code = f.read()

suffix = """    private suspend fun seedDefaultDestinations() {
""" + dests_code + """
        val batch = firestore.batch()
        for (dest in initialList) {
            val docRef = firestore.collection(FirestoreCollections.DESTINATIONS).document(dest.id)
            val data = mapOf(
                "id" to dest.id,
                "name" to dest.name,
                "state" to dest.state,
                "country" to dest.country,
                "latitude" to dest.latitude,
                "longitude" to dest.longitude,
                "hero_image_url" to dest.heroImageUrl,
                "description" to dest.description,
                "known_for" to dest.knownFor,
                "rating" to dest.rating,
                "best_time_to_visit" to dest.bestTimeToVisit,
                "ideal_stay" to dest.idealStay,
                "budget_per_day" to dest.budgetPerDay,
                "estimated_budget_tier" to dest.estimatedBudgetTier,
                "weather_temperature" to dest.weatherTemperature,
                "weather_condition" to dest.weatherCondition,
                "safety_score" to dest.safetyScore,
                "tags" to dest.tags,
                "top_attractions" to dest.topAttractions,
                "activities" to dest.activities,
                "famous_food" to dest.famousFood,
                "local_transport" to dest.localTransport,
                "is_featured" to dest.isFeatured,
                "is_popular" to dest.isPopular,
                "created_at" to com.google.firebase.Timestamp.now()
            )
            batch.set(docRef, data)
        }
        batch.commit().await()
    }
}
"""

new_repo = prefix + suffix
with open('c:/Users/97018/OneDrive/Desktop/version 2/App1 V2/Main app 1/appsss/frontend torist/app/src/main/java/com/touristapp/data/repository/DestinationRepository.kt', 'w', encoding='utf-8') as f:
    f.write(new_repo)

print("Successfully updated DestinationRepository.kt with 20 destinations!")
