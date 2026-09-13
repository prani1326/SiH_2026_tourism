package com.touristapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.touristapp.data.firebase.FirestoreCollections
import com.touristapp.data.models.DestinationDto
import kotlinx.coroutines.tasks.await

class DestinationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val backendApiClient: com.touristapp.data.remote.BackendApiClient = com.touristapp.data.remote.BackendApiClient()
) {
    private val cachedMap = java.util.concurrent.ConcurrentHashMap<String, DestinationDto>()

    init {
        // Pre-warm in-memory cache instantly for zero-latency UI rendering
        DEFAULT_DESTINATIONS.forEach { cachedMap[it.id] = it }
    }

    private fun cacheDestinations(list: List<DestinationDto>) {
        list.forEach { cachedMap[it.id] = it }
    }

    fun getCachedPopularDestinations(): List<DestinationDto> {
        val popular = cachedMap.values.filter { it.isPopular }
        return if (popular.isNotEmpty()) popular else cachedMap.values.toList()
    }

    fun getCachedDestinations(tag: String? = null, state: String? = null): List<DestinationDto> {
        var list = cachedMap.values.toList()
        if (!state.isNullOrEmpty()) {
            list = list.filter { it.state.equals(state, ignoreCase = true) }
        }
        if (!tag.isNullOrEmpty() && tag != "Destinations" && tag != "All") {
            list = list.filter { dest ->
                dest.tags.any { it.equals(tag, ignoreCase = true) } ||
                dest.description.contains(tag, ignoreCase = true) ||
                (dest.knownFor?.contains(tag, ignoreCase = true) == true)
            }
        }
        return list
    }

    suspend fun getPopularDestinations(): List<DestinationDto> {
        val apiRes = backendApiClient.getPopularDestinations()
        if (apiRes.isSuccess && apiRes.getOrNull()?.isNotEmpty() == true) {
            val list = apiRes.getOrThrow()
            cacheDestinations(list)
            return list
        }
        return getPopularDestinationsFromFirestore()
    }

    private suspend fun getPopularDestinationsFromFirestore(): List<DestinationDto> {
        return try {
            val allSnapshot = firestore.collection(FirestoreCollections.DESTINATIONS).get().await()
            if (allSnapshot.isEmpty || allSnapshot.size() < 15) {
                seedDefaultDestinations()
                val freshSnapshot = firestore.collection(FirestoreCollections.DESTINATIONS).get().await()
                val list = freshSnapshot.documents.map { DestinationDto.fromFirestore(it) }
                cacheDestinations(list)
                return list
            }
            val list = allSnapshot.documents.map { DestinationDto.fromFirestore(it) }
            cacheDestinations(list)
            list
        } catch (e: Exception) {
            cachedMap.values.filter { it.isPopular }.ifEmpty { cachedMap.values.toList() }
        }
    }

    suspend fun getDestinations(tag: String? = null, state: String? = null): List<DestinationDto> {
        val apiRes = backendApiClient.getDestinations(tag = tag)
        if (apiRes.isSuccess && apiRes.getOrNull()?.isNotEmpty() == true) {
            val list = apiRes.getOrThrow()
            cacheDestinations(list)
            return list
        }
        return getDestinationsFromFirestore(tag, state)
    }

    private suspend fun getDestinationsFromFirestore(tag: String? = null, state: String? = null): List<DestinationDto> {
        return try {
            var query = firestore.collection(FirestoreCollections.DESTINATIONS) as com.google.firebase.firestore.Query

            if (!state.isNullOrEmpty()) {
                query = query.whereEqualTo("state", state)
            }

            val snapshot = query.get().await()
            val destinations = snapshot.documents.map { DestinationDto.fromFirestore(it) }
            cacheDestinations(destinations)

            if (!tag.isNullOrEmpty() && tag != "Destinations" && tag != "All") {
                destinations.filter { dest ->
                    dest.tags.any { it.equals(tag, ignoreCase = true) } ||
                    dest.description.contains(tag, ignoreCase = true) ||
                    (dest.knownFor?.contains(tag, ignoreCase = true) == true)
                }
            } else {
                destinations
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (!tag.isNullOrEmpty() && tag != "Destinations" && tag != "All") {
                cachedMap.values.filter { dest ->
                    dest.tags.any { it.equals(tag, ignoreCase = true) } ||
                    (dest.knownFor?.contains(tag, ignoreCase = true) == true)
                }
            } else {
                cachedMap.values.toList()
            }
        }
    }

    suspend fun getDestinationById(id: String): DestinationDto? {
        val apiRes = backendApiClient.getDestinationById(id)
        if (apiRes.isSuccess) {
            val dest = apiRes.getOrNull()
            if (dest != null) {
                cachedMap[dest.id] = dest
                return dest
            }
        }
        return getDestinationByIdFromFirestore(id)
    }

    private suspend fun getDestinationByIdFromFirestore(id: String): DestinationDto? {
        return try {
            val doc = firestore.collection(FirestoreCollections.DESTINATIONS).document(id).get().await()
            if (doc.exists()) {
                val dest = DestinationDto.fromFirestore(doc)
                cachedMap[dest.id] = dest
                dest
            } else {
                cachedMap[id]
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cachedMap[id]
        }
    }

    suspend fun searchDestinations(query: String): List<DestinationDto> {
        if (query.isBlank()) return emptyList()
        val apiRes = backendApiClient.searchDestinations(query)
        if (apiRes.isSuccess && apiRes.getOrNull()?.isNotEmpty() == true) {
            val list = apiRes.getOrThrow()
            cacheDestinations(list)
            return list
        }
        return searchDestinationsFromFirestore(query)
    }

    private suspend fun searchDestinationsFromFirestore(query: String): List<DestinationDto> {
        return try {
            val snapshot = firestore.collection(FirestoreCollections.DESTINATIONS).get().await()
            val cleanQuery = query.trim().lowercase()

            snapshot.documents.map { DestinationDto.fromFirestore(it) }.filter { dest ->
                dest.name.lowercase().contains(cleanQuery) ||
                dest.state.lowercase().contains(cleanQuery) ||
                dest.description.lowercase().contains(cleanQuery) ||
                (dest.knownFor?.lowercase()?.contains(cleanQuery) == true) ||
                dest.tags.any { it.lowercase().contains(cleanQuery) } ||
                dest.topAttractions.any { it.lowercase().contains(cleanQuery) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val cleanQuery = query.trim().lowercase()
            cachedMap.values.filter { dest ->
                dest.name.lowercase().contains(cleanQuery) ||
                dest.state.lowercase().contains(cleanQuery) ||
                (dest.knownFor?.lowercase()?.contains(cleanQuery) == true) ||
                dest.tags.any { it.lowercase().contains(cleanQuery) } ||
                dest.topAttractions.any { it.lowercase().contains(cleanQuery) }
            }
        }
    }

    suspend fun seedDefaultDestinations() {
        val batch = firestore.batch()
        for (dest in DEFAULT_DESTINATIONS) {
            val docRef = firestore.collection(FirestoreCollections.DESTINATIONS).document(dest.id)
            val data = mapOf(
                "id" to dest.id,
                "name" to dest.name,
                "state" to dest.state,
                "country" to dest.country,
                "latitude" to dest.latitude,
                "longitude" to dest.longitude,
                "hero_image_url" to dest.heroImageUrl,
                "cover_image" to dest.heroImageUrl,
                "image" to dest.heroImageUrl,
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
            batch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
        }
        batch.commit().await()
    }

    companion object {
        val DEFAULT_DESTINATIONS = listOf(
            DestinationDto(
                id = "delhi",
                name = "Delhi",
                state = "Delhi",
                country = "India",
                latitude = 28.6139,
                longitude = 77.209,
                heroImageUrl = "https://images.unsplash.com/photo-1587474260584-136574528ed5?auto=format&fit=crop&w=1200&q=80",
                description = "India's vibrant capital blending ancient Mughal grandeur, colonial boulevards, world-class street food, and bustling shopping bazaars.",
                knownFor = "History, monuments, food, shopping, nightlife",
                rating = 4.8,
                bestTimeToVisit = "October–March",
                idealStay = "2–4 days",
                budgetPerDay = "₹1,500–₹5,000/day",
                estimatedBudgetTier = "₹1,500–₹5,000/day",
                weatherTemperature = "24°C",
                weatherCondition = "Pleasant & Sunny",
                safetyScore = 90,
                tags = listOf("Heritage", "Culture", "Food", "Shopping", "Nightlife", "Popular", "Trending"),
                topAttractions = listOf("Red Fort", "India Gate", "Qutub Minar", "Humayun's Tomb", "Lotus Temple", "Akshardham", "Jama Masjid", "Chandni Chowk"),
                activities = listOf("Heritage walks", "Food tours", "Shopping", "Museums", "Nightlife"),
                famousFood = listOf("Chole bhature", "Butter chicken", "Kebabs", "Parathas", "Chaat"),
                localTransport = listOf("Metro", "Buses", "Cabs", "Auto"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "mumbai",
                name = "Mumbai",
                state = "Maharashtra",
                country = "India",
                latitude = 18.922,
                longitude = 72.8347,
                heroImageUrl = "https://images.unsplash.com/photo-1570168007204-dfb528c6958f?auto=format&fit=crop&w=1200&q=80",
                description = "The City of Dreams with glittering Marine Drive, Victorian architecture, electric nightlife, thriving arts, and irresistible street food.",
                knownFor = "Bollywood, beaches, nightlife, business",
                rating = 4.8,
                bestTimeToVisit = "October–February",
                idealStay = "2–4 days",
                budgetPerDay = "₹2,000–₹6,000/day",
                estimatedBudgetTier = "₹2,000–₹6,000/day",
                weatherTemperature = "28°C",
                weatherCondition = "Coastal Breeze",
                safetyScore = 92,
                tags = listOf("Beaches", "Nightlife", "Culture", "Food", "Shopping", "Popular", "Trending"),
                topAttractions = listOf("Gateway of India", "Marine Drive", "Elephanta Caves", "Colaba Causeway", "Chhatrapati Shivaji Maharaj Terminus", "Juhu Beach"),
                activities = listOf("Bollywood tours", "Sightseeing", "Shopping", "Nightlife", "Street-food tours"),
                famousFood = listOf("Vada pav", "Pav bhaji", "Misal pav", "Bombay sandwich"),
                localTransport = listOf("Local trains", "Metro", "Buses", "Taxis"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "jaipur",
                name = "Jaipur",
                state = "Rajasthan",
                country = "India",
                latitude = 26.9124,
                longitude = 75.7873,
                heroImageUrl = "https://images.unsplash.com/photo-1477587458883-47145ed94245?auto=format&fit=crop&w=1200&q=80",
                description = "Rajasthan's enchanting Pink City celebrated for formidable hilltop forts, ornate royal palaces, vibrant gemstones, and regal hospitality.",
                knownFor = "Royal heritage, forts, palaces",
                rating = 4.9,
                bestTimeToVisit = "October–March",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹5,000/day",
                estimatedBudgetTier = "₹1,500–₹5,000/day",
                weatherTemperature = "25°C",
                weatherCondition = "Clear Sky",
                safetyScore = 94,
                tags = listOf("Heritage", "Culture", "Shopping", "Food", "Popular", "Trending"),
                topAttractions = listOf("Amber Fort", "Hawa Mahal", "City Palace", "Jantar Mantar", "Jal Mahal", "Nahargarh Fort"),
                activities = listOf("Fort tours", "Shopping", "Cultural shows", "Photography", "Desert experiences"),
                famousFood = listOf("Dal baati churma", "Ghewar", "Pyaaz kachori", "Laal maas"),
                localTransport = listOf("Metro", "Auto", "Taxi", "Buses"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "goa",
                name = "Goa",
                state = "Goa",
                country = "India",
                latitude = 15.2993,
                longitude = 74.124,
                heroImageUrl = "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?auto=format&fit=crop&w=1200&q=80",
                description = "India's beach capital offering golden shores, Portuguese colonial heritage, exciting watersports, seaside shacks, and sun-soaked nightlife.",
                knownFor = "Beaches, nightlife, parties, water sports",
                rating = 4.9,
                bestTimeToVisit = "November–February",
                idealStay = "3–5 days",
                budgetPerDay = "₹2,000–₹7,000/day",
                estimatedBudgetTier = "₹2,000–₹7,000/day",
                weatherTemperature = "29°C",
                weatherCondition = "Sunny & Breezy",
                safetyScore = 93,
                tags = listOf("Beaches", "Nightlife", "Adventure", "Food", "Culture", "Popular", "Trending"),
                topAttractions = listOf("Baga Beach", "Calangute", "Anjuna", "Fort Aguada", "Basilica of Bom Jesus", "Panjim"),
                activities = listOf("Scuba diving", "Parasailing", "Surfing", "Beach hopping", "Nightlife"),
                famousFood = listOf("Goan fish curry", "Pork vindaloo", "Bebinca"),
                localTransport = listOf("Scooters", "Taxis", "Buses"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "agra",
                name = "Agra",
                state = "Uttar Pradesh",
                country = "India",
                latitude = 27.1751,
                longitude = 78.0421,
                heroImageUrl = "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=1200&q=80",
                description = "Historic jewel on the Yamuna river, home to the iconic Taj Mahal, majestic Agra Fort, and UNESCO World Heritage Mughal architecture.",
                knownFor = "Taj Mahal and Mughal history",
                rating = 4.9,
                bestTimeToVisit = "October–March",
                idealStay = "1–2 days",
                budgetPerDay = "₹1,200–₹4,000/day",
                estimatedBudgetTier = "₹1,200–₹4,000/day",
                weatherTemperature = "23°C",
                weatherCondition = "Sunny",
                safetyScore = 91,
                tags = listOf("Heritage", "Culture", "Food", "Shopping", "Popular", "Trending"),
                topAttractions = listOf("Taj Mahal", "Agra Fort", "Mehtab Bagh", "Itmad-ud-Daulah", "Fatehpur Sikri"),
                activities = listOf("Heritage tours", "Photography", "Local shopping"),
                famousFood = listOf("Petha", "Mughlai cuisine", "Bedai"),
                localTransport = listOf("Auto", "Taxi", "E-rickshaw"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "varanasi",
                name = "Varanasi",
                state = "Uttar Pradesh",
                country = "India",
                latitude = 25.3176,
                longitude = 82.9739,
                heroImageUrl = "https://images.unsplash.com/photo-1561361513-2d000a50f0dc?auto=format&fit=crop&w=1200&q=80",
                description = "One of the world's oldest living cities, spiritual heart of India renowned for sacred Ganga Aarti, mystical sunrise boat rides, and ancient shrines.",
                knownFor = "Spirituality, Ganga ghats, temples",
                rating = 4.8,
                bestTimeToVisit = "October–March",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,000–₹4,000/day",
                estimatedBudgetTier = "₹1,000–₹4,000/day",
                weatherTemperature = "22°C",
                weatherCondition = "Mild & Pleasant",
                safetyScore = 89,
                tags = listOf("Spiritual", "Heritage", "Culture", "Food", "Popular", "Trending"),
                topAttractions = listOf("Kashi Vishwanath Temple", "Dashashwamedh Ghat", "Assi Ghat", "Manikarnika Ghat", "Sarnath"),
                activities = listOf("Ganga Aarti", "Boat ride", "Temple visits", "Heritage walks"),
                famousFood = listOf("Kachori sabzi", "Lassi", "Chaat", "Banarasi paan"),
                localTransport = listOf("Auto", "E-rickshaw", "Boat", "Taxi"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "amritsar",
                name = "Amritsar",
                state = "Punjab",
                country = "India",
                latitude = 31.634,
                longitude = 74.8723,
                heroImageUrl = "https://images.unsplash.com/photo-1514222134-b57cbb8ce073?auto=format&fit=crop&w=1200&q=80",
                description = "Sacred Sikh spiritual center, home to the breathtaking Harmandir Sahib (Golden Temple), poignant Jallianwala Bagh, and electric Wagah Border ceremony.",
                knownFor = "Harmandir Sahib (Golden Temple), Jallianwala Bagh, Wagah Border & Amritsari Kulcha",
                rating = 4.9,
                bestTimeToVisit = "October–March",
                idealStay = "2 days",
                budgetPerDay = "₹1,000–₹4,000/day",
                estimatedBudgetTier = "₹1,000–₹4,000/day",
                weatherTemperature = "20°C",
                weatherCondition = "Sunny & Crisp",
                safetyScore = 96,
                tags = listOf("Spiritual", "Heritage", "Culture", "Food", "Popular", "Trending"),
                topAttractions = listOf("Golden Temple", "Jallianwala Bagh", "Wagah Border", "Partition Museum"),
                activities = listOf("Temple visit", "Food tours", "Heritage tours"),
                famousFood = listOf("Amritsari kulcha", "Chole", "Lassi", "Tandoori food"),
                localTransport = listOf("Auto", "E-rickshaws", "Cabs"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "mysuru",
                name = "Mysuru",
                state = "Karnataka",
                country = "India",
                latitude = 12.2958,
                longitude = 76.6394,
                heroImageUrl = "https://images.unsplash.com/photo-1580835239846-5bb9ce03c8c3?auto=format&fit=crop&w=1200&q=80",
                description = "Heritage royal capital of the Wodeyars, famed for the grand illuminated Mysore Palace, aromatic sandalwood, silk sarees, and vibrant Dasara festivities.",
                knownFor = "Grand Mysore Palace, Chamundi Hills, silk sarees, sandalwood & Mysore Pak",
                rating = 4.8,
                bestTimeToVisit = "October–February",
                idealStay = "2 days",
                budgetPerDay = "₹1,200–₹4,000/day",
                estimatedBudgetTier = "₹1,200–₹4,000/day",
                weatherTemperature = "24°C",
                weatherCondition = "Pleasant",
                safetyScore = 95,
                tags = listOf("Heritage", "Culture", "Shopping", "Food", "Popular"),
                topAttractions = listOf("Mysore Palace", "Chamundi Hills", "Brindavan Gardens", "St. Philomena's Church"),
                activities = listOf("Palace tours", "Cultural sightseeing", "Shopping"),
                famousFood = listOf("Mysore pak", "Dosa", "South Indian meals"),
                localTransport = listOf("Auto", "KSRTC Buses", "Cabs"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "hampi",
                name = "Hampi",
                state = "Karnataka",
                country = "India",
                latitude = 15.335,
                longitude = 76.46,
                heroImageUrl = "https://images.unsplash.com/photo-1620766182966-c6eb5ed2b788?auto=format&fit=crop&w=1200&q=80",
                description = "UNESCO Vijayanagara ruins, majestic Stone Chariot, Virupaksha Temple, and scenic boulder-strewn riverscapes.",
                knownFor = "UNESCO Vijayanagara ruins, Stone Chariot, Virupaksha Temple & boulder landscape",
                rating = 4.8,
                bestTimeToVisit = "October–February",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,000–₹3,500/day",
                estimatedBudgetTier = "₹1,000–₹3,500/day",
                weatherTemperature = "26°C",
                weatherCondition = "Warm & Dry",
                safetyScore = 95,
                tags = listOf("Heritage", "Culture", "Monuments", "Popular"),
                topAttractions = listOf("Stone Chariot", "Virupaksha Temple", "Vittala Temple", "Matanga Hill"),
                activities = listOf("Bouldering", "Cycling tours", "Temple walks", "Coracle boat ride"),
                famousFood = listOf("South Indian thali", "Filter coffee", "Papad"),
                localTransport = listOf("Bicycles", "Auto", "Mopeds"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "pushkar",
                name = "Pushkar",
                state = "Rajasthan",
                country = "India",
                latitude = 26.4897,
                longitude = 74.5511,
                heroImageUrl = "https://images.unsplash.com/photo-1509749837427-ac94a2553d0e?auto=format&fit=crop&w=1200&q=80",
                description = "Sacred lake town revered for the ancient Brahma Temple, 52 holy bathing ghats, camel safaris, and colorful desert bazaars.",
                knownFor = "Sacred Pushkar Lake, Brahma Temple, camel fair & desert camping",
                rating = 4.8,
                bestTimeToVisit = "October–March",
                idealStay = "2 days",
                budgetPerDay = "₹1,000–₹3,500/day",
                estimatedBudgetTier = "₹1,000–₹3,500/day",
                weatherTemperature = "24°C",
                weatherCondition = "Desert Breeze",
                safetyScore = 94,
                tags = listOf("Spiritual", "Heritage", "Culture", "Popular"),
                topAttractions = listOf("Pushkar Lake", "Brahma Temple", "Savitri Temple", "Desert Safari"),
                activities = listOf("Ghat walks", "Camel safari", "Shopping", "Sunset views"),
                famousFood = listOf("Malpua", "Rabdi", "Kachori", "Lassi"),
                localTransport = listOf("Walking", "Auto", "Camel cart"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "khajuraho",
                name = "Khajuraho",
                state = "Madhya Pradesh",
                country = "India",
                latitude = 24.8318,
                longitude = 79.9199,
                heroImageUrl = "https://images.unsplash.com/photo-1608958435020-e8a7109ba809?auto=format&fit=crop&w=1200&q=80",
                description = "UNESCO World Heritage temple complex world-renowned for stunning Nagara stone architecture and intricate sculptures.",
                knownFor = "UNESCO world heritage temples famous for intricate stone sculptures & Nagara architecture",
                rating = 4.8,
                bestTimeToVisit = "October–March",
                idealStay = "2 days",
                budgetPerDay = "₹1,200–₹4,000/day",
                estimatedBudgetTier = "₹1,200–₹4,000/day",
                weatherTemperature = "22°C",
                weatherCondition = "Pleasant & Clear",
                safetyScore = 94,
                tags = listOf("Heritage", "Culture", "Monuments", "Popular"),
                topAttractions = listOf("Kandariya Mahadeva", "Lakshmana Temple", "Western Group of Temples", "Raneh Falls"),
                activities = listOf("Temple tours", "Light & sound show", "Archaeological museum"),
                famousFood = listOf("Poha", "Jalebi", "Dal Bafla"),
                localTransport = listOf("Auto", "Bicycles", "Taxis"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "rishikesh",
                name = "Rishikesh",
                state = "Uttarakhand",
                country = "India",
                latitude = 30.0869,
                longitude = 78.2676,
                heroImageUrl = "https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&w=1200&q=80",
                description = "World Capital of Yoga on the emerald Ganges foothills, famous for white-water rafting, tranquil ashrams, cliff jumping, and spiritual aartis.",
                knownFor = "White-water rafting, yoga ashrams, Ganga Aarti & bungee jumping",
                rating = 4.8,
                bestTimeToVisit = "September–June",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,000–₹4,000/day",
                estimatedBudgetTier = "₹1,000–₹4,000/day",
                weatherTemperature = "21°C",
                weatherCondition = "Fresh Mountain Breeze",
                safetyScore = 96,
                tags = listOf("Spiritual", "Adventure", "Mountains", "Food", "Popular", "Trending"),
                topAttractions = listOf("Laxman Jhula area", "Ram Jhula", "Triveni Ghat", "Beatles Ashram", "Neer Garh Waterfall"),
                activities = listOf("River rafting", "Bungee jumping", "Yoga", "Camping"),
                famousFood = listOf("North Indian food", "Cafes", "Vegetarian dishes"),
                localTransport = listOf("Auto", "Vikram", "Taxis"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "udaipur",
                name = "Udaipur",
                state = "Rajasthan",
                country = "India",
                latitude = 24.5854,
                longitude = 73.7125,
                heroImageUrl = "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80",
                description = "The romantic City of Lakes crowned with white marble palaces, shimmering Pichola waters, royal courtyards, and scenic mountain backdrops.",
                knownFor = "Lakes, palaces, romantic tourism",
                rating = 4.9,
                bestTimeToVisit = "October–March",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹5,000/day",
                estimatedBudgetTier = "₹1,500–₹5,000/day",
                weatherTemperature = "24°C",
                weatherCondition = "Serene & Clear",
                safetyScore = 95,
                tags = listOf("Heritage", "Culture", "Romantic", "Food", "Popular", "Trending"),
                topAttractions = listOf("City Palace", "Lake Pichola", "Jag Mandir", "Sajjangarh Palace", "Fateh Sagar Lake"),
                activities = listOf("Boat rides", "Palace tours", "Photography", "Cultural shows"),
                famousFood = listOf("Dal baati churma", "Gatte ki sabzi", "Kachori"),
                localTransport = listOf("Auto", "Taxi", "Cabs"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "manali",
                name = "Manali",
                state = "Himachal Pradesh",
                country = "India",
                latitude = 32.2432,
                longitude = 77.1892,
                heroImageUrl = "https://images.unsplash.com/photo-1605649487212-47bdab064df7?auto=format&fit=crop&w=1200&q=80",
                description = "Himalayan adventure haven nestled in the Beas River valley, renowned for snow sports, pine forests, Rohtang Pass, and scenic treks.",
                knownFor = "Mountains, snow, adventure",
                rating = 4.8,
                bestTimeToVisit = "March–June (pleasant), December–February (snow)",
                idealStay = "3–5 days",
                budgetPerDay = "₹1,500–₹5,000/day",
                estimatedBudgetTier = "₹1,500–₹5,000/day",
                weatherTemperature = "12°C",
                weatherCondition = "Cool & Mountain Air",
                safetyScore = 94,
                tags = listOf("Mountains", "Adventure", "Nature", "Popular", "Trending"),
                topAttractions = listOf("Solang Valley", "Rohtang region", "Hidimba Temple", "Old Manali", "Mall Road"),
                activities = listOf("Trekking", "Skiing", "Paragliding", "Rafting", "Snow activities"),
                famousFood = listOf("Siddu", "Thukpa", "Momos"),
                localTransport = listOf("Bus", "Taxi", "Local cabs"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "shimla",
                name = "Shimla",
                state = "Himachal Pradesh",
                country = "India",
                latitude = 31.1048,
                longitude = 77.1734,
                heroImageUrl = "https://images.unsplash.com/photo-1597074866923-dc0589150358?auto=format&fit=crop&w=1200&q=80",
                description = "Former British summer capital perched amidst cedar ridges, featuring the heritage Ridge, Mall Road, colonial churches, and scenic toy trains.",
                knownFor = "Hill station, snow, colonial architecture",
                rating = 4.7,
                bestTimeToVisit = "March–June (pleasant), December–February (snow)",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹5,000/day",
                estimatedBudgetTier = "₹1,500–₹5,000/day",
                weatherTemperature = "14°C",
                weatherCondition = "Crisp Mountain Chill",
                safetyScore = 95,
                tags = listOf("Mountains", "Heritage", "Adventure", "Shopping", "Popular"),
                topAttractions = listOf("Mall Road", "Ridge", "Christ Church", "Jakhoo Temple", "Kufri"),
                activities = listOf("Snow activities", "Hiking", "Sightseeing", "Shopping"),
                famousFood = listOf("Chana Madra", "Dhaam", "Sidu", "Momos"),
                localTransport = listOf("Toy Train", "Buses", "Taxis"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "kochi",
                name = "Kochi",
                state = "Kerala",
                country = "India",
                latitude = 9.9312,
                longitude = 76.2673,
                heroImageUrl = "https://images.unsplash.com/photo-1602216056096-3b40cc0c9944?auto=format&fit=crop&w=1200&q=80",
                description = "The Queen of the Arabian Sea featuring cantilevered Chinese fishing nets, Portuguese spice warehouses, Dutch palaces, and serene backwaters.",
                knownFor = "Backwaters, heritage, Kerala culture",
                rating = 4.8,
                bestTimeToVisit = "October–March",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹5,000/day",
                estimatedBudgetTier = "₹1,500–₹5,000/day",
                weatherTemperature = "27°C",
                weatherCondition = "Tropical Sea Breeze",
                safetyScore = 96,
                tags = listOf("Heritage", "Culture", "Beaches", "Food", "Popular"),
                topAttractions = listOf("Fort Kochi", "Chinese fishing nets", "Mattancherry Palace", "Jew Town", "Marine Drive"),
                activities = listOf("Backwater cruises", "Heritage walks", "Food tours"),
                famousFood = listOf("Appam", "Kerala fish curry", "Puttu", "Seafood"),
                localTransport = listOf("Metro", "Ferries", "Auto", "Taxis"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "bangalore",
                name = "Bangalore",
                state = "Karnataka",
                country = "India",
                latitude = 12.9716,
                longitude = 77.5946,
                heroImageUrl = "https://images.unsplash.com/photo-1596176530529-78163a4f7af2?auto=format&fit=crop&w=1200&q=80",
                description = "The Garden City & Silicon Valley of India, famed for year-round pleasant weather, expansive botanical parks, craft breweries, and thriving café culture.",
                knownFor = "Technology, gardens, cafes and nightlife",
                rating = 4.7,
                bestTimeToVisit = "October–February",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹5,000/day",
                estimatedBudgetTier = "₹1,500–₹5,000/day",
                weatherTemperature = "23°C",
                weatherCondition = "Pleasant & Breezy",
                safetyScore = 93,
                tags = listOf("Food", "Culture", "Nightlife", "Shopping", "Popular"),
                topAttractions = listOf("Bangalore Palace", "Lalbagh", "Cubbon Park", "Vidhana Soudha", "ISKCON Temple"),
                activities = listOf("Café hopping", "Nightlife", "Shopping", "City tours"),
                famousFood = listOf("Dosa", "Idli", "Filter coffee", "Biryani"),
                localTransport = listOf("Namma Metro", "Buses", "Auto", "Cabs"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "hyderabad",
                name = "Hyderabad",
                state = "Telangana",
                country = "India",
                latitude = 17.385,
                longitude = 78.4867,
                heroImageUrl = "https://images.unsplash.com/photo-1605379399642-870262d3d051?auto=format&fit=crop&w=1200&q=80",
                description = "City of Pearls balancing 400-year-old Qutb Shahi heritage, Charminar, Golconda acoustic fortress, modern tech hubs, and world-famous Biryani.",
                knownFor = "History, architecture and food",
                rating = 4.8,
                bestTimeToVisit = "October–February",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,200–₹4,000/day",
                estimatedBudgetTier = "₹1,200–₹4,000/day",
                weatherTemperature = "26°C",
                weatherCondition = "Sunny",
                safetyScore = 92,
                tags = listOf("Heritage", "Food", "Culture", "Shopping", "Popular"),
                topAttractions = listOf("Charminar", "Golconda Fort", "Hussain Sagar", "Chowmahalla Palace", "Salar Jung Museum"),
                activities = listOf("Heritage tours", "Food tours", "Shopping"),
                famousFood = listOf("Hyderabadi biryani", "Haleem", "Kebabs", "Double ka meetha"),
                localTransport = listOf("Metro", "Auto", "Buses", "Cabs"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "kolkata",
                name = "Kolkata",
                state = "West Bengal",
                country = "India",
                latitude = 22.5726,
                longitude = 88.3639,
                heroImageUrl = "https://images.unsplash.com/photo-1558431382-27e303142255?auto=format&fit=crop&w=1200&q=80",
                description = "The City of Joy and cultural soul of India, known for colonial marble monuments, literary salons, vintage trams, grand bridges, and unmatched sweets.",
                knownFor = "Culture, literature, colonial heritage and food",
                rating = 4.7,
                bestTimeToVisit = "October–March",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,200–₹4,000/day",
                estimatedBudgetTier = "₹1,200–₹4,000/day",
                weatherTemperature = "24°C",
                weatherCondition = "Comfortable & Mild",
                safetyScore = 93,
                tags = listOf("Culture", "Heritage", "Food", "Shopping", "Popular"),
                topAttractions = listOf("Victoria Memorial", "Howrah Bridge", "Indian Museum", "St. Paul's Cathedral", "Park Street"),
                activities = listOf("Heritage walks", "Food tours", "Cultural experiences"),
                famousFood = listOf("Kathi rolls", "Mishti doi", "Rasgulla", "Fish curry"),
                localTransport = listOf("Metro", "Yellow Taxis", "Trams", "Ferries"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "jodhpur",
                name = "Jodhpur",
                state = "Rajasthan",
                country = "India",
                latitude = 26.2389,
                longitude = 73.0243,
                heroImageUrl = "https://images.unsplash.com/photo-1598971861713-54ad16a7e72e?auto=format&fit=crop&w=1200&q=80",
                description = "Rajasthan's iconic Sun City with indigo-washed old quarters, the colossal cliff-top Mehrangarh Fort, marble cenotaphs, and Thar Desert adventures.",
                knownFor = "Mehrangarh Fort, Blue City painted houses, Umaid Bhawan & desert cuisine",
                rating = 4.8,
                bestTimeToVisit = "October–March",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹4,500/day",
                estimatedBudgetTier = "₹1,500–₹4,500/day",
                weatherTemperature = "25°C",
                weatherCondition = "Clear Sky",
                safetyScore = 94,
                tags = listOf("Heritage", "Culture", "Shopping", "Food", "Adventure", "Popular"),
                topAttractions = listOf("Mehrangarh Fort", "Jaswant Thada", "Umaid Bhawan Palace", "Clock Tower Market"),
                activities = listOf("Fort tours", "Photography", "Shopping", "Desert experiences"),
                famousFood = listOf("Makhaniya lassi", "Mirchi bada", "Dal baati"),
                localTransport = listOf("Auto", "Taxis", "Walking"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "darjeeling",
                name = "Darjeeling",
                state = "West Bengal",
                country = "India",
                latitude = 27.041,
                longitude = 88.2663,
                heroImageUrl = "https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&w=1200&q=80",
                description = "The Queen of the Hills overlooking Mount Kanchenjunga, world-famous emerald tea estates, UNESCO heritage steam toy trains, and Tibetan culture.",
                knownFor = "World-famous tea gardens, Himalayan Toy Train, Kanchenjunga view & Tibetan cuisine",
                rating = 4.8,
                bestTimeToVisit = "March–May, October–December",
                idealStay = "2–4 days",
                budgetPerDay = "₹1,500–₹5,000/day",
                estimatedBudgetTier = "₹1,500–₹5,000/day",
                weatherTemperature = "13°C",
                weatherCondition = "Crisp Mountain Mist",
                safetyScore = 95,
                tags = listOf("Mountains", "Nature", "Heritage", "Food", "Adventure", "Popular"),
                topAttractions = listOf("Tiger Hill", "Darjeeling Himalayan Railway", "Batasia Loop", "Himalayan Mountaineering Institute", "Tea gardens"),
                activities = listOf("Trekking", "Tea tours", "Toy Train ride", "Mountain sightseeing"),
                famousFood = listOf("Momos", "Thukpa", "Tibetan cuisine"),
                localTransport = listOf("Shared jeeps", "Taxis"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "puducherry",
                name = "Puducherry",
                state = "Puducherry",
                country = "India",
                latitude = 11.9416,
                longitude = 79.8083,
                heroImageUrl = "https://images.unsplash.com/photo-1582510003544-4d00b7f74220?auto=format&fit=crop&w=1200&q=80",
                description = "Charming French colonial coastal haven with pastel villas, cobblestone boulevards, peaceful seaside promenade, Auroville township, and serene beaches.",
                knownFor = "French colonial White Town, Promenade Beach, Auroville & seaside cafes",
                rating = 4.8,
                bestTimeToVisit = "October–March",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹5,000/day",
                estimatedBudgetTier = "₹1,500–₹5,000/day",
                weatherTemperature = "27°C",
                weatherCondition = "Balmy Sea Breeze",
                safetyScore = 94,
                tags = listOf("Beaches", "Culture", "Heritage", "Food", "Spiritual", "Popular"),
                topAttractions = listOf("White Town", "Promenade Beach", "Auroville", "Sri Aurobindo Ashram", "Paradise Beach"),
                activities = listOf("Café hopping", "Cycling", "Beach visits", "Photography"),
                famousFood = listOf("French cuisine", "South Indian food", "Seafood"),
                localTransport = listOf("Bicycles", "Rented scooters", "Auto"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "leh-ladakh",
                name = "Leh-Ladakh",
                state = "Ladakh",
                country = "India",
                latitude = 34.1526,
                longitude = 77.5771,
                heroImageUrl = "https://images.unsplash.com/photo-1581793745862-99fde7fa73d2?auto=format&fit=crop&w=1200&q=80",
                description = "High-altitude desert wonderland featuring azure Pangong Lake, ancient Tibetan Buddhist monasteries, and high mountain passes.",
                knownFor = "High-altitude desert, Pangong Lake, monasteries & biking expeditions",
                rating = 4.8,
                bestTimeToVisit = "May–September",
                idealStay = "4–6 days",
                budgetPerDay = "₹2,000–₹6,000/day",
                estimatedBudgetTier = "₹2,000–₹6,000/day",
                weatherTemperature = "15°C",
                weatherCondition = "Sunny & Crisp",
                safetyScore = 95,
                tags = listOf("Adventure", "Mountains", "Nature", "Popular"),
                topAttractions = listOf("Pangong Lake", "Nubra Valley", "Magnetic Hill", "Thiksey Monastery", "Khardung La"),
                activities = listOf("Biking", "Trekking", "Monastery visits", "Camping", "River rafting"),
                famousFood = listOf("Thukpa", "Momos", "Butter tea", "Skyu"),
                localTransport = listOf("Taxis", "Rented bikes", "Buses"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "munnar",
                name = "Munnar",
                state = "Kerala",
                country = "India",
                latitude = 10.0889,
                longitude = 77.0595,
                heroImageUrl = "https://images.unsplash.com/photo-1590050752117-238cb0fb12b1?auto=format&fit=crop&w=1200&q=80",
                description = "South India's tea capital nestled in misty Western Ghats, renowned for rolling emerald plantations and waterfalls.",
                knownFor = "Sprawling tea gardens, mist-covered hills, waterfalls & Eravikulam National Park",
                rating = 4.8,
                bestTimeToVisit = "September–March",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹4,500/day",
                estimatedBudgetTier = "₹1,500–₹4,500/day",
                weatherTemperature = "18°C",
                weatherCondition = "Cool & Misty",
                safetyScore = 96,
                tags = listOf("Nature", "Mountains", "Romantic", "Popular"),
                topAttractions = listOf("Eravikulam National Park", "Mattupetty Dam", "Tea Museum", "Anamudi Peak"),
                activities = listOf("Tea plantation walks", "Trekking", "Bird watching"),
                famousFood = listOf("Kerala Appam", "Puttu", "Fresh Cardamom Tea"),
                localTransport = listOf("Auto", "Taxis", "Jeeps"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "coorg",
                name = "Coorg",
                state = "Karnataka",
                country = "India",
                latitude = 12.3375,
                longitude = 75.8069,
                heroImageUrl = "https://images.unsplash.com/photo-1593693397690-362cb9666fc2?auto=format&fit=crop&w=1200&q=80",
                description = "Scotland of India famed for aromatic coffee plantations, Abbey Falls, misty mountain ridges, and Kodava culture.",
                knownFor = "Coffee plantations, Abbey Falls, misty Western Ghats & Kodava hospitality",
                rating = 4.8,
                bestTimeToVisit = "October–March",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹5,000/day",
                estimatedBudgetTier = "₹1,500–₹5,000/day",
                weatherTemperature = "20°C",
                weatherCondition = "Pleasant & Breezy",
                safetyScore = 95,
                tags = listOf("Nature", "Mountains", "Relaxation", "Popular"),
                topAttractions = listOf("Abbey Falls", "Raja's Seat", "Dubare Elephant Camp", "Talakaveri"),
                activities = listOf("Coffee tours", "Trekking", "River rafting"),
                famousFood = listOf("Pandi Curry", "Kadambuttu", "Filter Coffee"),
                localTransport = listOf("Taxis", "Auto"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "gangtok",
                name = "Gangtok",
                state = "Sikkim",
                country = "India",
                latitude = 27.3389,
                longitude = 88.6065,
                heroImageUrl = "https://images.unsplash.com/photo-1589182373726-e4f658ab50f0?auto=format&fit=crop&w=1200&q=80",
                description = "Clean Himalayan city offering majestic Kanchenjunga panoramas, tranquil Buddhist monasteries, and high-altitude alpine lakes.",
                knownFor = "Rumtek Monastery, Nathula Pass, Tsomgo Lake & Kanchenjunga views",
                rating = 4.8,
                bestTimeToVisit = "March–May, October–December",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹4,500/day",
                estimatedBudgetTier = "₹1,500–₹4,500/day",
                weatherTemperature = "15°C",
                weatherCondition = "Crisp & Sunny",
                safetyScore = 96,
                tags = listOf("Mountains", "Nature", "Spiritual", "Popular"),
                topAttractions = listOf("Rumtek Monastery", "Tsomgo Lake", "Nathula Pass", "MG Marg"),
                activities = listOf("Cable car ride", "Monastery walks", "Lake tours"),
                famousFood = listOf("Momos", "Thukpa", "Phagshapa"),
                localTransport = listOf("Shared Taxis", "Local Cabs"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "mahabalipuram",
                name = "Mahabalipuram",
                state = "Tamil Nadu",
                country = "India",
                latitude = 12.6269,
                longitude = 80.1928,
                heroImageUrl = "https://images.unsplash.com/photo-1609137144813-7d9921338f24?auto=format&fit=crop&w=1200&q=80",
                description = "UNESCO seaside complex famous for 7th-century rock-cut monolithic rathas, cave sanctuaries, and the majestic Shore Temple.",
                knownFor = "Shore Temple, Pancha Rathas, rock-cut cave monuments & seaside heritage",
                rating = 4.8,
                bestTimeToVisit = "November–February",
                idealStay = "1–2 days",
                budgetPerDay = "₹1,200–₹3,500/day",
                estimatedBudgetTier = "₹1,200–₹3,500/day",
                weatherTemperature = "28°C",
                weatherCondition = "Warm & Coastal",
                safetyScore = 94,
                tags = listOf("Heritage", "Culture", "Beaches", "Popular"),
                topAttractions = listOf("Shore Temple", "Pancha Rathas", "Arjuna's Penance", "Butter Ball"),
                activities = listOf("Heritage walks", "Beach visits", "Stone carving workshops"),
                famousFood = listOf("Seafood", "South Indian meals"),
                localTransport = listOf("Auto", "Bicycles", "Walking"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "mount-abu",
                name = "Mount Abu",
                state = "Rajasthan",
                country = "India",
                latitude = 24.5925,
                longitude = 72.7156,
                heroImageUrl = "https://images.unsplash.com/photo-1548013146-72479768bada?auto=format&fit=crop&w=1200&q=80",
                description = "Rajasthan's only hill retreat surrounded by green Aravalli forests, tranquil Nakki Lake, and exquisitely carved marble Dilwara Jain temples.",
                knownFor = "Dilwara Jain Temples, Nakki Lake, Guru Shikhar peak & Aravalli hill retreat",
                rating = 4.7,
                bestTimeToVisit = "September–March",
                idealStay = "2 days",
                budgetPerDay = "₹1,200–₹4,000/day",
                estimatedBudgetTier = "₹1,200–₹4,000/day",
                weatherTemperature = "19°C",
                weatherCondition = "Pleasant Mountain Climate",
                safetyScore = 95,
                tags = listOf("Mountains", "Heritage", "Spiritual", "Popular"),
                topAttractions = listOf("Dilwara Temples", "Nakki Lake", "Guru Shikhar", "Sunset Point"),
                activities = listOf("Boating", "Temple sightseeing", "Nature walks"),
                famousFood = listOf("Dal Baati", "Rabdi", "Ghewar"),
                localTransport = listOf("Auto", "Taxis", "Walking"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "ooty",
                name = "Ooty",
                state = "Tamil Nadu",
                country = "India",
                latitude = 11.4102,
                longitude = 76.695,
                heroImageUrl = "https://images.unsplash.com/photo-1589182373726-e4f658ab50f0?auto=format&fit=crop&w=1200&q=80",
                description = "Queen of Nilgiri Hill Stations with rolling eucalyptus hills, expansive botanical gardens, colonial charm, and heritage toy trains.",
                knownFor = "Nilgiri Mountain Toy Train, Ooty Lake, botanical gardens & tea estates",
                rating = 4.8,
                bestTimeToVisit = "October–June",
                idealStay = "2–3 days",
                budgetPerDay = "₹1,500–₹4,500/day",
                estimatedBudgetTier = "₹1,500–₹4,500/day",
                weatherTemperature = "16°C",
                weatherCondition = "Crisp & Cool",
                safetyScore = 95,
                tags = listOf("Mountains", "Nature", "Romantic", "Popular"),
                topAttractions = listOf("Nilgiri Mountain Railway", "Ooty Lake", "Botanical Garden", "Doddabetta Peak"),
                activities = listOf("Toy Train ride", "Boating", "Tea garden visits"),
                famousFood = listOf("Homemade Chocolates", "Fresh Tea", "South Indian thali"),
                localTransport = listOf("Auto", "Taxis", "Toy Train"),
                isFeatured = true,
                isPopular = true
            ),
            DestinationDto(
                id = "port-blair",
                name = "Port Blair",
                state = "Andaman and Nicobar Islands",
                country = "India",
                latitude = 11.6234,
                longitude = 92.7265,
                heroImageUrl = "https://images.unsplash.com/photo-1589308078059-be1415eab4c3?auto=format&fit=crop&w=1200&q=80",
                description = "Tropical island paradise gateway to turquoise waters, coral reefs, pristine Radhanagar Beach, and poignant historic Cellular Jail.",
                knownFor = "Cellular Jail, Radhanagar Beach, turquoise waters, scuba diving & coral reefs",
                rating = 4.8,
                bestTimeToVisit = "October–May",
                idealStay = "3–5 days",
                budgetPerDay = "₹2,000–₹6,000/day",
                estimatedBudgetTier = "₹2,000–₹6,000/day",
                weatherTemperature = "28°C",
                weatherCondition = "Tropical Island Breeze",
                safetyScore = 95,
                tags = listOf("Beaches", "Adventure", "Heritage", "Popular"),
                topAttractions = listOf("Cellular Jail", "Ross Island", "Corbyn's Cove", "Havelock Ferry"),
                activities = listOf("Scuba diving", "Snorkeling", "Island hopping", "Light and sound show"),
                famousFood = listOf("Seafood", "Coconut water", "Curry"),
                localTransport = listOf("Ferries", "Taxis", "Auto"),
                isFeatured = true,
                isPopular = true
            )
        )
    }
}
