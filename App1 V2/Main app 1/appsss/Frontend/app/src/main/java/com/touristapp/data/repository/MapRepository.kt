package com.touristapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.touristapp.data.firebase.FirestoreCollections
import com.touristapp.data.models.PlaceModel
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

class MapRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val cache = ConcurrentHashMap<String, PlaceModel>()

    init {
        // Pre-warm in-memory cache with curated realistic travel points for popular destinations
        DEFAULT_PLACES.forEach { cache[it.id] = it }
    }

    suspend fun getPlaces(
        category: String? = null,
        destinationId: String? = null,
        query: String? = null
    ): List<PlaceModel> {
        // 1. Try to fetch from remote Firestore
        try {
            val remoteSnapshot = withTimeoutOrNull(2500L) {
                var queryRef = firestore.collection(FirestoreCollections.PLACES)
                    .whereEqualTo("is_active", true)

                if (!destinationId.isNullOrBlank()) {
                    queryRef = queryRef.whereEqualTo("destination_id", destinationId)
                }
                queryRef.get().await()
            }

            if (remoteSnapshot != null && !remoteSnapshot.isEmpty) {
                val remotePlaces = remoteSnapshot.documents.map { PlaceModel.fromFirestore(it) }
                remotePlaces.forEach { cache[it.id] = it }
            } else if (remoteSnapshot != null && remoteSnapshot.isEmpty) {
                // Seed default places to Firestore in background
                seedPlacesToFirestore()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Filter cached items
        var result = cache.values.toList()

        if (!destinationId.isNullOrBlank()) {
            result = result.filter { it.destinationId.equals(destinationId, ignoreCase = true) }
        }

        if (!category.isNullOrBlank() && !category.equals("All", ignoreCase = true)) {
            val normalizedCat = when (category.lowercase()) {
                "hotels", "hotel" -> "hotel"
                "food", "restaurants", "restaurant" -> "food"
                "activities", "activity" -> "activity"
                "attractions", "attraction" -> "attraction"
                "transport", "transit" -> "transport"
                "meeting", "meeting point" -> "meeting"
                else -> category.lowercase()
            }
            result = result.filter { it.category.equals(normalizedCat, ignoreCase = true) }
        }

        if (!query.isNullOrBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.destinationName.lowercase().contains(q) ||
                (it.address?.lowercase()?.contains(q) == true)
            }
        }

        return result.sortedByDescending { it.rating }
    }

    suspend fun getPlaceById(id: String): PlaceModel? {
        cache[id]?.let { return it }
        return try {
            val doc = withTimeoutOrNull(2500L) {
                firestore.collection(FirestoreCollections.PLACES).document(id).get().await()
            }
            if (doc != null && doc.exists()) {
                val place = PlaceModel.fromFirestore(doc)
                cache[id] = place
                place
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun savePlace(place: PlaceModel): Result<PlaceModel> {
        cache[place.id] = place
        try {
            withTimeoutOrNull(2500L) {
                firestore.collection(FirestoreCollections.PLACES)
                    .document(place.id)
                    .set(place.toFirestoreMap())
                    .await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Result.success(place)
    }

    private suspend fun seedPlacesToFirestore() {
        try {
            val batch = firestore.batch()
            DEFAULT_PLACES.take(20).forEach { place ->
                val docRef = firestore.collection(FirestoreCollections.PLACES).document(place.id)
                batch.set(docRef, place.toFirestoreMap())
            }
            batch.commit().await()
        } catch (_: Exception) {}
    }

    companion object {
        val DEFAULT_PLACES = listOf(
            // AGRA
            PlaceModel(
                id = "place_agra_taj_mahal",
                name = "Taj Mahal",
                description = "Iconic 17th-century white marble mausoleum built by Mughal emperor Shah Jahan. UNESCO World Heritage Site.",
                latitude = 27.1751,
                longitude = 78.0421,
                imageUrl = "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=800&auto=format&fit=crop&q=80",
                category = "attraction",
                rating = 4.9,
                price = "₹50 (Indians) / ₹1100 (Foreigners)",
                destinationId = "dest_agra",
                destinationName = "Agra",
                availability = "Sunrise to Sunset (Closed Fridays)",
                address = "Dharmapuri, Forest Colony, Tajganj, Agra"
            ),
            PlaceModel(
                id = "place_agra_fort",
                name = "Agra Fort",
                description = "Historical fortress in the city of Agra. It was the main residence of the emperors of the Mughal Dynasty until 1638.",
                latitude = 27.1795,
                longitude = 78.0211,
                imageUrl = "https://images.unsplash.com/photo-1585135497273-1a86b09fe70e?w=800&auto=format&fit=crop&q=80",
                category = "attraction",
                rating = 4.7,
                price = "₹40 (Indians) / ₹550 (Foreigners)",
                destinationId = "dest_agra",
                destinationName = "Agra",
                availability = "6:00 AM - 6:00 PM Daily",
                address = "Agra Fort, Rakabganj, Agra"
            ),
            PlaceModel(
                id = "place_agra_oberoi",
                name = "The Oberoi Amarvilas",
                description = "Luxury 5-star resort located just 600 meters from the Taj Mahal, with private balcony views of the monument.",
                latitude = 27.1685,
                longitude = 78.0488,
                imageUrl = "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&auto=format&fit=crop&q=80",
                category = "hotel",
                rating = 4.9,
                price = "₹32,000 / night",
                destinationId = "dest_agra",
                destinationName = "Agra",
                availability = "24/7 Check-in",
                address = "Taj East Gate Road, Agra"
            ),
            PlaceModel(
                id = "place_agra_peshawri",
                name = "Peshawri at ITC Mughal",
                description = "Award-winning restaurant bringing the authentic flavors of the rugged North-West Frontier cuisine.",
                latitude = 27.1603,
                longitude = 78.0338,
                imageUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800&auto=format&fit=crop&q=80",
                category = "food",
                rating = 4.8,
                price = "₹2,500 for two",
                destinationId = "dest_agra",
                destinationName = "Agra",
                availability = "12:30 PM - 11:00 PM",
                address = "Fatehabad Rd, Tajganj, Agra"
            ),
            PlaceModel(
                id = "place_agra_heritage_walk",
                name = "Kachhpura Heritage Village Walk",
                description = "Community-based cultural walking tour through Mughal-era heritage villages, rooftop sunset views, and local handicrafts.",
                latitude = 27.1830,
                longitude = 78.0450,
                imageUrl = "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800&auto=format&fit=crop&q=80",
                category = "activity",
                rating = 4.6,
                price = "₹850 per person",
                destinationId = "dest_agra",
                destinationName = "Agra",
                availability = "Morning 7:30 AM & Evening 4:00 PM",
                address = "Kachhpura, Near Mehtab Bagh, Agra"
            ),
            PlaceModel(
                id = "place_agra_cantt",
                name = "Agra Cantt Railway Station Pickup",
                description = "Primary transit point for Gatimaan Express and Vande Bharat. Pre-paid taxi and official tourist assistance booth available.",
                latitude = 27.1580,
                longitude = 77.9904,
                imageUrl = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800&auto=format&fit=crop&q=80",
                category = "transport",
                rating = 4.4,
                price = "Free Entry",
                destinationId = "dest_agra",
                destinationName = "Agra",
                availability = "Open 24 Hours",
                address = "Station Road, Cantonment, Agra"
            ),

            // GOA
            PlaceModel(
                id = "place_goa_baga",
                name = "Baga Beach",
                description = "Vibrant sandy shoreline famous for water sports, beach shacks, lively nightlife, and dolphin cruises.",
                latitude = 15.5553,
                longitude = 73.7517,
                imageUrl = "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=800&auto=format&fit=crop&q=80",
                category = "attraction",
                rating = 4.6,
                price = "Free Entry",
                destinationId = "dest_goa",
                destinationName = "Goa",
                availability = "Open 24 Hours",
                address = "Baga, North Goa"
            ),
            PlaceModel(
                id = "place_goa_aguada",
                name = "Fort Aguada & Lighthouse",
                description = "17th-century Portuguese fortress overlooking the Arabian Sea, featuring an old four-story lighthouse and freshwater cisterns.",
                latitude = 15.4920,
                longitude = 73.7736,
                imageUrl = "https://images.unsplash.com/photo-1587974928442-77dc3e0dba72?w=800&auto=format&fit=crop&q=80",
                category = "attraction",
                rating = 4.7,
                price = "₹50 per person",
                destinationId = "dest_goa",
                destinationName = "Goa",
                availability = "9:30 AM - 6:00 PM Daily",
                address = "Candolim, Sinquerim, Goa"
            ),
            PlaceModel(
                id = "place_goa_watersports",
                name = "Calangute Water Sports Complex",
                description = "Parasailing, Jet Skiing, Banana boat rides and Bumper tube adventures supervised by certified coastal lifeguards.",
                latitude = 15.5440,
                longitude = 73.7550,
                imageUrl = "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800&auto=format&fit=crop&q=80",
                category = "activity",
                rating = 4.8,
                price = "₹1,800 Combo Pack",
                destinationId = "dest_goa",
                destinationName = "Goa",
                availability = "9:00 AM - 5:30 PM",
                address = "Calangute Beach Promenade, North Goa"
            ),
            PlaceModel(
                id = "place_goa_taj_fort",
                name = "Taj Fort Aguada Resort & Spa",
                description = "Beachfront heritage resort set within the ramparts of a 16th-century Portuguese coastal fort.",
                latitude = 15.4980,
                longitude = 73.7680,
                imageUrl = "https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800&auto=format&fit=crop&q=80",
                category = "hotel",
                rating = 4.9,
                price = "₹22,000 / night",
                destinationId = "dest_goa",
                destinationName = "Goa",
                availability = "24/7 Check-in",
                address = "Sinquerim, Candolim, Goa"
            ),
            PlaceModel(
                id = "place_goa_fishermans",
                name = "The Fisherman's Wharf",
                description = "Riverside dining serving authentic Goan fish curry, butter garlic prawns, and live acoustics.",
                latitude = 15.1764,
                longitude = 73.9460,
                imageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800&auto=format&fit=crop&q=80",
                category = "food",
                rating = 4.7,
                price = "₹1,600 for two",
                destinationId = "dest_goa",
                destinationName = "Goa",
                availability = "12:00 PM - 11:30 PM",
                address = "Mobor Beach, Cavelossim, South Goa"
            ),
            PlaceModel(
                id = "place_goa_mopa",
                name = "Manohar International Airport Pickup",
                description = "North Goa Mopa Airport tourist taxi hub and electric express bus terminal.",
                latitude = 15.7667,
                longitude = 73.8667,
                imageUrl = "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=800&auto=format&fit=crop&q=80",
                category = "transport",
                rating = 4.6,
                price = "Fixed Tariff",
                destinationId = "dest_goa",
                destinationName = "Goa",
                availability = "Open 24 Hours",
                address = "Mopa, Pernem, North Goa"
            ),

            // DELHI / JAIPUR
            PlaceModel(
                id = "place_jaipur_hawa_mahal",
                name = "Hawa Mahal (Palace of Winds)",
                description = "Five-story pink sandstone palace with 953 intricately carved windows designed for royal women to observe street life.",
                latitude = 26.9239,
                longitude = 75.8267,
                imageUrl = "https://images.unsplash.com/photo-1603258849062-103328e10410?w=800&auto=format&fit=crop&q=80",
                category = "attraction",
                rating = 4.7,
                price = "₹50 (Indians) / ₹200 (Foreigners)",
                destinationId = "dest_jaipur",
                destinationName = "Jaipur",
                availability = "9:00 AM - 5:00 PM Daily",
                address = "Hawa Mahal Rd, Badi Choupad, Jaipur"
            ),
            PlaceModel(
                id = "place_jaipur_amber_fort",
                name = "Amber Palace & Fort",
                description = "Majestic hilltop fort featuring artistic Hindu-style elements, Sheesh Mahal (Mirror Palace), and panoramic valley views.",
                latitude = 26.9855,
                longitude = 75.8513,
                imageUrl = "https://images.unsplash.com/photo-1599661046289-e31897846e41?w=800&auto=format&fit=crop&q=80",
                category = "attraction",
                rating = 4.8,
                price = "₹100 (Indians) / ₹550 (Foreigners)",
                destinationId = "dest_jaipur",
                destinationName = "Jaipur",
                availability = "8:00 AM - 6:00 PM Daily",
                address = "Devisinghpura, Amer, Jaipur"
            ),
            PlaceModel(
                id = "place_delhi_red_fort",
                name = "Red Fort (Lal Qila)",
                description = "Historic Mughal citadel in Old Delhi built by Shah Jahan in 1639, featuring grand sandstone gates and museum pavilions.",
                latitude = 28.6562,
                longitude = 77.2410,
                imageUrl = "https://images.unsplash.com/photo-1587474260584-136574528ed5?w=800&auto=format&fit=crop&q=80",
                category = "attraction",
                rating = 4.6,
                price = "₹35 (Indians) / ₹500 (Foreigners)",
                destinationId = "dest_delhi",
                destinationName = "Delhi",
                availability = "9:30 AM - 4:30 PM (Closed Mondays)",
                address = "Netaji Subhash Marg, Chandni Chowk, New Delhi"
            )
        )
    }
}
