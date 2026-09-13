package com.touristapp.data.models

import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DestinationDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("country") val country: String = "India",
    @SerialName("latitude") val latitude: Double = 0.0,
    @SerialName("longitude") val longitude: Double = 0.0,
    @SerialName("hero_image_url") val heroImageUrl: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("known_for") val knownFor: String? = null,
    @SerialName("rating") val rating: Double = 4.5,
    @SerialName("best_time_to_visit") val bestTimeToVisit: String = "All year",
    @SerialName("ideal_stay") val idealStay: String? = null,
    @SerialName("budget_per_day") val budgetPerDay: String? = null,
    @SerialName("estimated_budget_tier") val estimatedBudgetTier: String = "Moderate",
    @SerialName("weather_temperature") val weatherTemperature: String = "24°C",
    @SerialName("weather_condition") val weatherCondition: String = "Sunny",
    @SerialName("safety_score") val safetyScore: Int = 90,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("top_attractions") val topAttractions: List<String> = emptyList(),
    @SerialName("activities") val activities: List<String> = emptyList(),
    @SerialName("famous_food") val famousFood: List<String> = emptyList(),
    @SerialName("local_transport") val localTransport: List<String> = emptyList(),
    @SerialName("nearby_places") val nearbyPlaces: List<String> = emptyList(),
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("is_popular") val isPopular: Boolean = false
) {
    fun getDisplayImageUrl(): String {
        if (heroImageUrl.isNotBlank() && !isBrokenOrDeadUrl(heroImageUrl)) {
            return heroImageUrl
        }
        return getCanonicalImageFor(name, state)
    }

    companion object {
        private val BROKEN_URL_SNIPPETS = listOf(
            "1588096344356-9a22d86f99cb",
            "1600100397608-f010f444f2b9",
            "1600100397839-a9692c8a2b5e",
            "1600100397937-64b58e72791d",
            "1600100397552-32a2f9cb13f2"
        )

        private fun isBrokenOrDeadUrl(url: String): Boolean {
            return BROKEN_URL_SNIPPETS.any { url.contains(it) }
        }

        fun getCanonicalImageFor(name: String, state: String = ""): String {
            val key = name.trim().lowercase()
            return when {
                key.contains("amritsar") -> "https://images.unsplash.com/photo-1514222134-b57cbb8ce073?auto=format&fit=crop&w=1200&q=80"
                key.contains("mysuru") || key.contains("mysore") -> "https://images.unsplash.com/photo-1580835239846-5bb9ce03c8c3?auto=format&fit=crop&w=1200&q=80"
                key.contains("hampi") -> "https://images.unsplash.com/photo-1620766182966-c6eb5ed2b788?auto=format&fit=crop&w=1200&q=80"
                key.contains("pushkar") -> "https://images.unsplash.com/photo-1509749837427-ac94a2553d0e?auto=format&fit=crop&w=1200&q=80"
                key.contains("khajuraho") -> "https://images.unsplash.com/photo-1608958435020-e8a7109ba809?auto=format&fit=crop&w=1200&q=80"
                key.contains("goa") -> "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=1200&q=80"
                key.contains("delhi") -> "https://images.unsplash.com/photo-1587474260584-136574528ed5?auto=format&fit=crop&w=1200&q=80"
                key.contains("mumbai") -> "https://images.unsplash.com/photo-1570168007204-dfb528c6958f?auto=format&fit=crop&w=1200&q=80"
                key.contains("jaipur") -> "https://images.unsplash.com/photo-1477587458883-47145ed94245?auto=format&fit=crop&w=1200&q=80"
                key.contains("agra") -> "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=1200&q=80"
                key.contains("varanasi") || key.contains("banaras") -> "https://images.unsplash.com/photo-1561361513-2d000a50f0dc?auto=format&fit=crop&w=1200&q=80"
                key.contains("udaipur") -> "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80"
                key.contains("jodhpur") -> "https://images.unsplash.com/photo-1598971861713-54ad16a7e72e?auto=format&fit=crop&w=1200&q=80"
                key.contains("manali") -> "https://images.unsplash.com/photo-1605649487212-47bdab064df7?auto=format&fit=crop&w=1200&q=80"
                key.contains("rishikesh") -> "https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&w=1200&q=80"
                key.contains("shimla") -> "https://images.unsplash.com/photo-1597074866923-dc0589150358?auto=format&fit=crop&w=1200&q=80"
                key.contains("kochi") || key.contains("cochin") -> "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1200&q=80"
                key.contains("bangalore") || key.contains("bengaluru") -> "https://images.unsplash.com/photo-1596176530529-78163a4f7af2?auto=format&fit=crop&w=1200&q=80"
                key.contains("hyderabad") -> "https://images.unsplash.com/photo-1605379399642-870262d3d051?auto=format&fit=crop&w=1200&q=80"
                key.contains("kolkata") || key.contains("calcutta") -> "https://images.unsplash.com/photo-1558431382-27e303142255?auto=format&fit=crop&w=1200&q=80"
                key.contains("chennai") || key.contains("madras") -> "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80"
                key.contains("darjeeling") -> "https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&w=1200&q=80"
                key.contains("puducherry") || key.contains("pondicherry") -> "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80"
                key.contains("ladakh") || key.contains("leh") -> "https://images.unsplash.com/photo-1581793745862-99fde7fa73d2?auto=format&fit=crop&w=1200&q=80"
                key.contains("munnar") -> "https://images.unsplash.com/photo-1590050752117-238cb0fb12b1?auto=format&fit=crop&w=1200&q=80"
                key.contains("coorg") || key.contains("kodagu") -> "https://images.unsplash.com/photo-1593693397690-362cb9666fc2?auto=format&fit=crop&w=1200&q=80"
                key.contains("gangtok") || key.contains("sikkim") -> "https://images.unsplash.com/photo-1589182373726-e4f658ab50f0?auto=format&fit=crop&w=1200&q=80"
                key.contains("mahabalipuram") || key.contains("mamallapuram") -> "https://images.unsplash.com/photo-1609137144813-7d9921338f24?auto=format&fit=crop&w=1200&q=80"
                key.contains("mount abu") -> "https://images.unsplash.com/photo-1548013146-72479768bada?auto=format&fit=crop&w=1200&q=80"
                key.contains("ooty") || key.contains("udhagamandalam") -> "https://images.unsplash.com/photo-1589182373726-e4f658ab50f0?auto=format&fit=crop&w=1200&q=80"
                key.contains("port blair") || key.contains("andaman") -> "https://images.unsplash.com/photo-1589308078059-be1415eab4c3?auto=format&fit=crop&w=1200&q=80"
                else -> "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?auto=format&fit=crop&w=1200&q=80"
            }
        }

        fun fromFirestore(doc: DocumentSnapshot): DestinationDto {
            val data = doc.data ?: emptyMap()
            val rawName = data["name"] as? String ?: ""
            val rawState = data["state"] as? String ?: ""

            var img = (data["hero_image_url"] as? String)
                ?: (data["cover_image"] as? String)
                ?: (data["image"] as? String)
                ?: (data["image_url"] as? String)
                ?: (data["imageUrl"] as? String)
                ?: (data["photo_url"] as? String)
                ?: ""

            if (img.isBlank() || isBrokenOrDeadUrl(img)) {
                img = getCanonicalImageFor(rawName, rawState)
            }

            @Suppress("UNCHECKED_CAST")
            return DestinationDto(
                id = doc.id,
                name = rawName,
                state = rawState,
                country = data["country"] as? String ?: "India",
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                heroImageUrl = img,
                description = data["description"] as? String ?: "",
                knownFor = data["known_for"] as? String,
                rating = (data["rating"] as? Number)?.toDouble() ?: 4.8,
                bestTimeToVisit = data["best_time_to_visit"] as? String ?: "October–March",
                idealStay = data["ideal_stay"] as? String ?: "2–3 days",
                budgetPerDay = data["budget_per_day"] as? String ?: "₹1,500–₹4,500/day",
                estimatedBudgetTier = data["estimated_budget_tier"] as? String ?: "₹1,500–₹4,500/day",
                weatherTemperature = data["weather_temperature"] as? String ?: "24°C",
                weatherCondition = data["weather_condition"] as? String ?: "Pleasant",
                safetyScore = (data["safety_score"] as? Number)?.toInt() ?: 94,
                tags = (data["tags"] as? List<String>) ?: (data["tags_json"] as? List<String>) ?: listOf("Popular", "Heritage"),
                topAttractions = (data["top_attractions"] as? List<String>) ?: (data["top_attractions_json"] as? List<String>) ?: emptyList(),
                activities = (data["activities"] as? List<String>) ?: (data["activities_json"] as? List<String>) ?: emptyList(),
                famousFood = (data["famous_food"] as? List<String>) ?: (data["famous_food_json"] as? List<String>) ?: emptyList(),
                localTransport = (data["local_transport"] as? List<String>) ?: (data["local_transport_json"] as? List<String>) ?: emptyList(),
                nearbyPlaces = (data["nearby_places"] as? List<String>) ?: (data["nearby_places_json"] as? List<String>) ?: emptyList(),
                isFeatured = (data["is_featured"] as? Boolean) ?: ((data["is_featured"] as? Number)?.toInt() == 1),
                isPopular = (data["is_popular"] as? Boolean) ?: ((data["is_popular"] as? Number)?.toInt() == 1)
            )
        }
    }
}
