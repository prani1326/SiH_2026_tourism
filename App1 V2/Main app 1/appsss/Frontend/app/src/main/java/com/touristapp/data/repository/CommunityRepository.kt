package com.touristapp.data.repository

import com.touristapp.data.models.TripModel
import com.touristapp.data.remote.BackendApiClient
import com.touristapp.data.remote.model.CommunityForumDto
import com.touristapp.data.remote.model.CommunityPostDto
import com.touristapp.data.remote.model.CreatorItineraryDto
import java.util.UUID

class CommunityRepository(
    private val backendApiClient: BackendApiClient = BackendApiClient(),
    private val tripRepository: TripRepository? = null
) {
    suspend fun getForums(): Result<List<CommunityForumDto>> {
        val res = backendApiClient.getCommunityForums()
        if (res.isSuccess) return res

        // Fallback default forums
        return Result.success(
            listOf(
                CommunityForumDto(
                    id = "forum-heritage-lovers",
                    title = "Heritage & Monument Enthusiasts",
                    description = "Architecture, hidden gems, and photography tips across forts and palaces.",
                    coverImage = "https://images.unsplash.com/photo-1599661046827-dacff0c0f09a?auto=format&fit=crop&w=600&q=80",
                    category = "Culture",
                    memberCount = 1420
                ),
                CommunityForumDto(
                    id = "forum-solo-backpackers",
                    title = "Solo & Budget Travelers",
                    description = "Safe travel advice, hostel recommendations, and travel buddy finding.",
                    coverImage = "https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=600&q=80",
                    category = "Solo Travel",
                    memberCount = 2890
                ),
                CommunityForumDto(
                    id = "forum-foodies",
                    title = "Vegetarian & Street Food Explorers",
                    description = "Reviews of authentic regional culinary trails and pure vegetarian kitchens.",
                    coverImage = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=600&q=80",
                    category = "Food",
                    memberCount = 3150
                )
            )
        )
    }

    suspend fun getPosts(forumId: String? = null): Result<List<CommunityPostDto>> {
        val res = backendApiClient.getCommunityPosts(forumId)
        if (res.isSuccess) return res

        return Result.success(
            listOf(
                CommunityPostDto(
                    id = "post-1",
                    forumId = forumId ?: "forum-heritage-lovers",
                    userName = "Priya Sharma",
                    title = "Top 3 Sunset Viewpoints in Jaipur you can't miss",
                    content = "Nahargarh Fort at sunset gives the most breathtaking panoramic view of the Pink City. Make sure to reach by 5:15 PM.",
                    likesCount = 42,
                    commentsCount = 8,
                    createdAt = "2 hours ago"
                ),
                CommunityPostDto(
                    id = "post-2",
                    forumId = forumId ?: "forum-foodies",
                    userName = "Rohit Verma",
                    title = "Verified Pure Vegetarian Gems in Old Delhi",
                    content = "Visited Kake Di Hatti and Haldiram Chandni Chowk. Authentic flavors and verified segregated vegetarian preparation.",
                    likesCount = 38,
                    commentsCount = 12,
                    createdAt = "5 hours ago"
                )
            )
        )
    }

    suspend fun createPost(forumId: String, title: String, content: String): Result<CommunityPostDto> {
        return backendApiClient.createCommunityPost(forumId, title, content)
    }

    suspend fun getCreatorItineraries(): Result<List<CreatorItineraryDto>> {
        val res = backendApiClient.getCreatorItineraries()
        if (res.isSuccess) return res

        return Result.success(
            listOf(
                CreatorItineraryDto(
                    id = "ci-golden-triangle",
                    creatorName = "Aarav Sharma (Verified Guide)",
                    creatorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
                    title = "Golden Triangle Express in 4 Days (Agra, Jaipur, Delhi)",
                    destinationName = "Agra & Jaipur",
                    durationDays = 4,
                    totalEstimatedCost = 18500.0,
                    price = 0.0,
                    rating = 4.9,
                    copyCount = 540
                ),
                CreatorItineraryDto(
                    id = "ci-goa-slow",
                    creatorName = "Maya Sen (Traveler)",
                    creatorAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=400&q=80",
                    title = "South Goa Relaxed Coastal Discovery (3 Days)",
                    destinationName = "Goa",
                    durationDays = 3,
                    totalEstimatedCost = 12000.0,
                    price = 0.0,
                    rating = 4.8,
                    copyCount = 310
                )
            )
        )
    }

    suspend fun copyCreatorItinerary(itineraryId: String): Result<String> {
        val res = backendApiClient.copyCreatorItinerary(itineraryId)
        val tripId = if (res.isSuccess) res.getOrThrow() else UUID.randomUUID().toString()

        val sampleDaysJson = """
            {
              "days": [
                {
                  "day": 1,
                  "theme": "Arrival & Historical Exploration",
                  "activities": [
                    {"time_slot": "Morning", "start_time": "09:00", "end_time": "12:00", "title": "Old Delhi & Red Fort Exploration", "description": "Marvel at Mughal architecture and historical ramparts with audio guide.", "transport_mode": "Metro / Cab", "travel_time": "20 mins", "cost": "₹250"},
                    {"time_slot": "Afternoon", "start_time": "13:00", "end_time": "15:30", "title": "Chandni Chowk Authentic Food Trail", "description": "Savor authentic regional parathas and sweets at iconic heritage eateries.", "transport_mode": "Walk", "travel_time": "5 mins", "cost": "₹400"},
                    {"time_slot": "Evening", "start_time": "17:00", "end_time": "19:30", "title": "India Gate & Sunset Stroll", "description": "Sunset boulevard walk with panoramic lighting and vibrant street atmosphere.", "transport_mode": "Cab", "travel_time": "25 mins", "cost": "₹200"}
                  ]
                },
                {
                  "day": 2,
                  "theme": "Agra & The Taj Mahal Wonder",
                  "activities": [
                    {"time_slot": "Morning", "start_time": "06:00", "end_time": "09:30", "title": "Taj Mahal Sunrise Experience", "description": "Experience the world wonder at sunrise with serene morning reflection.", "transport_mode": "Express Train / Cab", "travel_time": "1.5 hrs", "cost": "₹1100"},
                    {"time_slot": "Afternoon", "start_time": "12:00", "end_time": "14:30", "title": "Agra Fort Mughal Heritage", "description": "Explore the royal red sandstone fortress and Emperor chambers.", "transport_mode": "Auto", "travel_time": "15 mins", "cost": "₹500"},
                    {"time_slot": "Evening", "start_time": "17:00", "end_time": "18:30", "title": "Mehtab Bagh Sunset Across Yamuna", "description": "Spectacular photography spot capturing the Taj silhouette.", "transport_mode": "Auto", "travel_time": "20 mins", "cost": "₹300"}
                  ]
                },
                {
                  "day": 3,
                  "theme": "Pink City Forts & Heritage",
                  "activities": [
                    {"time_slot": "Morning", "start_time": "08:30", "end_time": "12:30", "title": "Amber Palace & Sheesh Mahal", "description": "Hilltop fort with ornate mirror palace and courtyards.", "transport_mode": "Cab", "travel_time": "30 mins", "cost": "₹600"},
                    {"time_slot": "Afternoon", "start_time": "14:00", "end_time": "16:30", "title": "Hawa Mahal & City Palace", "description": "Honeycomb facade and royal textile museum.", "transport_mode": "Auto", "travel_time": "20 mins", "cost": "₹700"},
                    {"time_slot": "Evening", "start_time": "17:30", "end_time": "19:30", "title": "Nahargarh Fort Sunset Point", "description": "Sunset views over the entire Jaipur skyline.", "transport_mode": "Cab", "travel_time": "35 mins", "cost": "₹400"}
                  ]
                },
                {
                  "day": 4,
                  "theme": "Bazaars, Cuisine & Departure",
                  "activities": [
                    {"time_slot": "Morning", "start_time": "09:30", "end_time": "12:00", "title": "Johari Bazaar Handicrafts & Gems", "description": "Traditional block prints, blue pottery and souvenirs.", "transport_mode": "Walk", "travel_time": "10 mins", "cost": "₹500"},
                    {"time_slot": "Afternoon", "start_time": "13:00", "end_time": "15:00", "title": "Rajasthani Thali Experience", "description": "Authentic multi-course traditional royal lunch.", "transport_mode": "Auto", "travel_time": "15 mins", "cost": "₹800"}
                  ]
                }
              ]
            }
        """.trimIndent()

        val newTrip = TripModel(
            id = tripId,
            title = "Golden Triangle in 4 Days (Agra, Jaipur, Delhi)",
            destinationName = "Agra & Jaipur",
            destinationId = "golden-triangle",
            startDate = "2026-10-15",
            endDate = "2026-10-19",
            status = "active",
            daysCount = 4,
            travelersCount = 2,
            budgetTotal = 18500.0,
            budgetSpent = 0.0,
            style = "Curated",
            interests = listOf("Heritage", "Culture", "Photography"),
            itineraryJson = sampleDaysJson
        )
        tripRepository?.createTrip(newTrip)
        return Result.success(tripId)
    }
}
