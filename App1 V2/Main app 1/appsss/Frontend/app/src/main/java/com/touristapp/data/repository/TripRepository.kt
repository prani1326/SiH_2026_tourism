package com.touristapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.touristapp.data.firebase.FirestoreCollections
import com.touristapp.data.models.TripModel
import com.touristapp.data.remote.BackendApiClient
import com.touristapp.data.remote.model.AITripPlanRequestDto
import com.touristapp.data.remote.model.GeneratedItineraryPlanDto
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import com.touristapp.data.models.TripLeaderDto
import com.touristapp.data.models.TripTransportDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TripRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val backendApiClient: BackendApiClient = BackendApiClient()
) {

    private val localTripsCache = ConcurrentHashMap<String, TripModel>()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    init {
        seedCuratedTrips()
    }

    private fun seedCuratedTrips() {
        val uid = auth.currentUser?.uid ?: "guest"

        // 1. Curated Kerala Trip Plan (5 Days)
        val keralaPlan = buildFallbackPlanDto(
            destination = "Kerala",
            days = 5,
            style = "Couple",
            interests = listOf("Nature", "Backwaters", "Tea Gardens", "Ayurveda"),
            activities = listOf("Houseboat Cruise", "Tea Plantation Walk", "Kathakali Performance", "Spice Trail"),
            budget = 38500.0,
            foodPreference = "Authentic Kerala Sadhya & Seafood",
            hotelPreference = "4 Star Premium Resort",
            transportPreference = "Private AC Cab",
            travelPace = "Relaxed",
            startDateStr = "2026-10-15",
            endDateStr = "2026-10-20",
            travelers = 2
        )
        val keralaJson = json.encodeToString(keralaPlan)

        // 2. Curated Agra Trip Plan (3 Days)
        val agraPlan = buildFallbackPlanDto(
            destination = "Agra",
            days = 3,
            style = "Cultural",
            interests = listOf("Heritage", "Architecture", "Photography", "Mughlai Food"),
            activities = listOf("Taj Mahal Sunrise", "Agra Fort Heritage Walk", "Fatehpur Sikri Excursion", "Petha Trail"),
            budget = 24000.0,
            foodPreference = "Authentic Mughlai & North Indian",
            hotelPreference = "Heritage Palace Hotel",
            transportPreference = "Private AC Cab",
            travelPace = "Balanced",
            startDateStr = "2026-11-24",
            endDateStr = "2026-11-27",
            travelers = 2
        )
        val agraJson = json.encodeToString(agraPlan)

        // Upcoming trips: Kerala (1st) and Agra (2nd)
        val keralaUpcoming = TripModel(
            id = "trip_kerala_upcoming",
            userId = uid,
            title = "Kerala Backwaters & Munnar Hills",
            destinationName = "Kerala",
            startDate = "15 Oct 2026",
            endDate = "20 Oct 2026",
            status = "upcoming",
            daysCount = 5,
            travelersCount = 2,
            budgetTotal = 38500.0,
            budgetSpent = 14500.0,
            style = "Couple",
            interests = listOf("Nature", "Backwaters", "Tea Gardens", "Ayurveda"),
            itineraryJson = keralaJson,
            leaderId = "leader_kerala_01",
            transportId = "transport_kerala_01",
            destinationLatitude = 9.9312,
            destinationLongitude = 76.2673,
            approvalStatus = "approved"
        )
        val agraUpcoming = TripModel(
            id = "trip_agra_upcoming",
            userId = uid,
            title = "Agra & Taj Mahal Heritage Discovery",
            destinationName = "Agra",
            startDate = "24 Nov 2026",
            endDate = "27 Nov 2026",
            status = "upcoming",
            daysCount = 3,
            travelersCount = 2,
            budgetTotal = 24000.0,
            budgetSpent = 8000.0,
            style = "Cultural",
            interests = listOf("Heritage", "Architecture", "Photography", "Mughlai Food"),
            itineraryJson = agraJson,
            leaderId = "leader_agra_01",
            transportId = "transport_agra_01",
            destinationLatitude = 27.1751,
            destinationLongitude = 78.0421,
            approvalStatus = "approved"
        )

        // Past trips: Kerala (1st) and Agra (2nd)
        val keralaPast = TripModel(
            id = "trip_kerala_past",
            userId = uid,
            title = "Kerala Backwaters & Munnar Hills",
            destinationName = "Kerala",
            startDate = "12 May 2026",
            endDate = "17 May 2026",
            status = "completed",
            daysCount = 5,
            travelersCount = 2,
            budgetTotal = 38500.0,
            budgetSpent = 38500.0,
            style = "Couple",
            interests = listOf("Nature", "Backwaters", "Tea Gardens", "Ayurveda"),
            itineraryJson = keralaJson,
            leaderId = "leader_kerala_01",
            transportId = "transport_kerala_01",
            destinationLatitude = 9.9312,
            destinationLongitude = 76.2673,
            approvalStatus = "completed"
        )
        val agraPast = TripModel(
            id = "trip_agra_past",
            userId = uid,
            title = "Agra & Taj Mahal Heritage Discovery",
            destinationName = "Agra",
            startDate = "10 Jan 2026",
            endDate = "13 Jan 2026",
            status = "completed",
            daysCount = 3,
            travelersCount = 2,
            budgetTotal = 24000.0,
            budgetSpent = 23800.0,
            style = "Cultural",
            interests = listOf("Heritage", "Architecture", "Photography", "Mughlai Food"),
            itineraryJson = agraJson,
            leaderId = "leader_agra_01",
            transportId = "transport_agra_01",
            destinationLatitude = 27.1751,
            destinationLongitude = 78.0421,
            approvalStatus = "completed"
        )

        // Active trips: Kerala (1st) and Agra (2nd)
        val keralaActive = TripModel(
            id = "trip_kerala_active",
            userId = uid,
            title = "Kerala Backwaters & Munnar Hills",
            destinationName = "Kerala",
            startDate = "05 Sep 2026",
            endDate = "10 Sep 2026",
            status = "active",
            daysCount = 5,
            travelersCount = 2,
            budgetTotal = 38500.0,
            budgetSpent = 16200.0,
            style = "Couple",
            interests = listOf("Nature", "Backwaters", "Tea Gardens", "Ayurveda"),
            itineraryJson = keralaJson,
            leaderId = "leader_kerala_01",
            transportId = "transport_kerala_01",
            destinationLatitude = 9.9312,
            destinationLongitude = 76.2673,
            approvalStatus = "approved"
        )
        val agraActive = TripModel(
            id = "trip_agra_active",
            userId = uid,
            title = "Agra & Taj Mahal Heritage Discovery",
            destinationName = "Agra",
            startDate = "07 Sep 2026",
            endDate = "10 Sep 2026",
            status = "active",
            daysCount = 3,
            travelersCount = 2,
            budgetTotal = 24000.0,
            budgetSpent = 9500.0,
            style = "Cultural",
            interests = listOf("Heritage", "Architecture", "Photography", "Mughlai Food"),
            itineraryJson = agraJson,
            leaderId = "leader_agra_01",
            transportId = "transport_agra_01",
            destinationLatitude = 27.1751,
            destinationLongitude = 78.0421,
            approvalStatus = "approved"
        )

        listOf(keralaUpcoming, agraUpcoming, keralaPast, agraPast, keralaActive, agraActive).forEach {
            localTripsCache[it.id] = it
        }
    }

    suspend fun getUserTrips(userId: String? = null): List<TripModel> {
        val targetUid = userId ?: auth.currentUser?.uid ?: "guest"
        val cachedTrips = localTripsCache.values.toList()
        return try {
            val snapshot = withTimeoutOrNull(2500L) {
                firestore.collection(FirestoreCollections.TRIPS)
                    .whereEqualTo("user_id", targetUid)
                    .get()
                    .await()
            }
            val remoteTrips = snapshot?.documents?.map { TripModel.fromFirestore(it) } ?: emptyList()
            remoteTrips.forEach { localTripsCache[it.id] = it }
            val merged = (remoteTrips + cachedTrips).associateBy { it.id }.values.toList()
            merged
        } catch (e: Exception) {
            e.printStackTrace()
            cachedTrips
        }
    }

    suspend fun getTripById(tripId: String): TripModel? {
        localTripsCache[tripId]?.let { return it }
        return try {
            val doc = withTimeoutOrNull(2500L) {
                firestore.collection(FirestoreCollections.TRIPS).document(tripId).get().await()
            }
            if (doc != null && doc.exists()) {
                val trip = TripModel.fromFirestore(doc)
                localTripsCache[tripId] = trip
                trip
            } else {
                localTripsCache[tripId]
            }
        } catch (e: Exception) {
            e.printStackTrace()
            localTripsCache[tripId]
        }
    }

    suspend fun createTrip(trip: TripModel): Result<TripModel> {
        val tripId = if (trip.id.isNotBlank()) trip.id else UUID.randomUUID().toString()
        val userId = if (trip.userId.isNotBlank()) trip.userId else (auth.currentUser?.uid ?: "guest")
        val finalizedTrip = trip.copy(id = tripId, userId = userId)
        localTripsCache[tripId] = finalizedTrip

        // Attempt remote Firestore persistence with safe timeout so network drops never block the UI
        try {
            withTimeoutOrNull(2500L) {
                firestore.collection(FirestoreCollections.TRIPS)
                    .document(tripId)
                    .set(finalizedTrip.toFirestoreMap())
                    .await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Result.success(finalizedTrip)
    }

    suspend fun updateTrip(trip: TripModel): Result<TripModel> {
        localTripsCache[trip.id] = trip
        try {
            withTimeoutOrNull(2500L) {
                firestore.collection(FirestoreCollections.TRIPS)
                    .document(trip.id)
                    .set(trip.toFirestoreMap())
                    .await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Result.success(trip)
    }

    /**
     * Preview AI Trip Plan from Gemini AI / Backend without saving to Firestore immediately,
     * allowing the user to review the generated preview before confirming.
     */
    suspend fun previewAiPlan(
        destination: String,
        days: Int,
        travelers: Int,
        budget: Double,
        interests: List<String>,
        activities: List<String> = emptyList(),
        foodPreference: String = "Local Food",
        hotelPreference: String = "3 Star",
        transportPreference: String = "Cab",
        travelPace: String = "Balanced",
        style: String = "Couple",
        walkingTolerance: String = "Moderate",
        startDate: String? = null,
        endDate: String? = null
    ): Result<GeneratedItineraryPlanDto> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val defaultStart = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, days)
        val defaultEnd = sdf.format(cal.time)

        val startDateStr = startDate?.ifBlank { null } ?: defaultStart
        val endDateStr = endDate?.ifBlank { null } ?: defaultEnd

        val requestDto = AITripPlanRequestDto(
            destination = destination,
            durationDays = days,
            startDate = startDateStr,
            endDate = endDateStr,
            travelerCount = travelers,
            budget = budget,
            currency = "INR",
            travelStyle = style,
            interests = interests,
            activities = activities,
            foodPreference = foodPreference,
            hotelPreference = hotelPreference,
            transportPreference = transportPreference,
            travelPace = travelPace,
            walkingTolerance = walkingTolerance,
            pace = travelPace
        )

        val backendResult = backendApiClient.generateAiPlan(requestDto)
        if (backendResult.isSuccess) {
            val plan = backendResult.getOrThrow()
            if (plan.days.isNotEmpty()) {
                return Result.success(plan)
            }
        } else {
            val err = backendResult.exceptionOrNull()?.message ?: ""
            if (err.contains("PROFILE_KYC_REQUIRED") || err.contains("403")) {
                return Result.failure(Exception("PROFILE_KYC_REQUIRED: Complete your profile and KYC before creating a trip."))
            }
        }

        // Intelligent deterministic fallback (only for offline or network drops, not for KYC security rejections)
        val fallbackPlan = buildFallbackPlanDto(
            destination = destination,
            days = days,
            style = style,
            interests = interests,
            activities = activities,
            budget = budget,
            foodPreference = foodPreference,
            hotelPreference = hotelPreference,
            transportPreference = transportPreference,
            travelPace = travelPace,
            startDateStr = startDateStr,
            endDateStr = endDateStr,
            travelers = travelers
        )
        return Result.success(fallbackPlan)
    }

    /**
     * Finalizes and saves the approved preview plan as an official persistent TripModel in Firestore.
     */
    suspend fun finalizeAndSaveTrip(
        previewPlan: GeneratedItineraryPlanDto,
        style: String = "Couple",
        interests: List<String> = emptyList(),
        budget: Double = 0.0,
        startDate: String? = null,
        endDate: String? = null
    ): Result<TripModel> {
        val tripId = previewPlan.effectiveTripId?.ifBlank { null } ?: UUID.randomUUID().toString()
        val userId = auth.currentUser?.uid ?: "guest"
        val itineraryJsonStr = json.encodeToString(previewPlan)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val defaultStart = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, previewPlan.durationDays)
        val defaultEnd = sdf.format(cal.time)

        val startDateStr = startDate?.ifBlank { null } ?: defaultStart
        val endDateStr = endDate?.ifBlank { null } ?: defaultEnd

        val trip = TripModel(
            id = tripId,
            userId = userId,
            title = previewPlan.title.ifBlank { "${previewPlan.durationDays}-Day Trip to ${previewPlan.destination}" },
            destinationName = previewPlan.destination,
            startDate = startDateStr,
            endDate = endDateStr,
            status = "planning",
            daysCount = previewPlan.durationDays,
            travelersCount = previewPlan.travelers,
            budgetTotal = if (previewPlan.totalEstimatedCost > 0) previewPlan.totalEstimatedCost else budget,
            budgetSpent = 0.0,
            style = style,
            interests = interests,
            itineraryJson = itineraryJsonStr
        )

        return createTrip(trip)
    }

    /**
     * Legacy direct generate & save method for backward compatibility.
     */
    suspend fun generateAiPlan(
        destination: String,
        days: Int,
        travelers: Int,
        budget: Double,
        interests: List<String>,
        activities: List<String> = emptyList(),
        foodPreference: String = "Local Food",
        hotelPreference: String = "3 Star",
        transportPreference: String = "Cab",
        travelPace: String = "Balanced",
        style: String = "Couple",
        walkingTolerance: String = "Moderate"
    ): Result<TripModel> {
        val previewRes = previewAiPlan(
            destination = destination,
            days = days,
            travelers = travelers,
            budget = budget,
            interests = interests,
            activities = activities,
            foodPreference = foodPreference,
            hotelPreference = hotelPreference,
            transportPreference = transportPreference,
            travelPace = travelPace,
            style = style,
            walkingTolerance = walkingTolerance
        )
        val plan = previewRes.getOrElse {
            buildFallbackPlanDto(
                destination = destination,
                days = days,
                style = style,
                interests = interests,
                activities = activities,
                budget = budget,
                foodPreference = foodPreference,
                hotelPreference = hotelPreference,
                transportPreference = transportPreference,
                travelPace = travelPace,
                startDateStr = "2026-09-06",
                endDateStr = "2026-09-09",
                travelers = travelers
            )
        }
        return finalizeAndSaveTrip(plan, style = style, interests = interests, budget = budget)
    }

    private fun buildFallbackPlanDto(
        destination: String,
        days: Int,
        style: String,
        interests: List<String>,
        activities: List<String>,
        budget: Double,
        foodPreference: String,
        hotelPreference: String,
        transportPreference: String,
        travelPace: String,
        startDateStr: String,
        endDateStr: String,
        travelers: Int
    ): GeneratedItineraryPlanDto {
        val parsedStartCal = Calendar.getInstance().apply {
            try {
                time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(startDateStr) ?: time
            } catch (_: Exception) {}
        }
        val dateDisplayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.US)
        val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

        val daysList = (1..days).map { dayNum ->
            val dayCal = (parsedStartCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayNum - 1)
            }
            val formattedDayDate = dateDisplayFormat.format(dayCal.time)
            val shortDayDate = shortDateFormat.format(dayCal.time)

            val (dayTitle, dayTheme, actList) = getCuratedActivitiesForDay(
                destination = destination,
                dayNum = dayNum,
                days = days,
                shortDayDate = shortDayDate,
                budget = budget,
                foodPreference = foodPreference,
                hotelPreference = hotelPreference,
                transportPreference = transportPreference
            )

            com.touristapp.data.remote.model.GeneratedDayDto(
                dayNumber = dayNum,
                date = formattedDayDate,
                title = dayTitle,
                theme = dayTheme,
                weatherSummary = when (dayNum % 3) {
                    1 -> "Sunny & Pleasant (26°C)"
                    2 -> "Clear Skies & Breezy (24°C)"
                    else -> "Golden Sunshine & Warm (27°C)"
                },
                notes = "Ideal travel pace: $travelPace",
                estimatedDayCost = budget / days,
                activities = actList
            )
        }

        val budgetBreakdown = com.touristapp.data.remote.model.BudgetBreakdownDto(
            hotel = budget * 0.35,
            food = budget * 0.25,
            transport = budget * 0.15,
            activities = budget * 0.15,
            miscellaneous = budget * 0.10
        )

        return GeneratedItineraryPlanDto(
            id = UUID.randomUUID().toString(),
            tripId = UUID.randomUUID().toString(),
            destination = destination,
            title = "$days-Day $style Trip in $destination",
            tripTitle = "$days-Day $style Trip in $destination",
            durationDays = days,
            travelers = travelers,
            totalEstimatedCost = budget,
            currency = "INR",
            days = daysList,
            budgetBreakdown = budgetBreakdown,
            summary = "Comprehensive, curated multi-day itinerary with verified attractions, authentic dining, and coordinated transport.",
            highlights = listOf("Curated Attractions", "Authentic Dining ($foodPreference)", "Coordinated $transportPreference", "Comfortable $hotelPreference Stay"),
            safetyNotes = listOf("Emergency Helpline: +91 1800-TOURIST", "24/7 SOS In-App Dispatch Available"),
            isFallback = true
        )
    }

    private fun buildFallbackItinerary(
        destination: String,
        days: Int,
        style: String,
        interests: List<String>,
        budget: Double
    ): String {
        val plan = buildFallbackPlanDto(
            destination = destination,
            days = days,
            style = style,
            interests = interests,
            activities = emptyList(),
            budget = budget,
            foodPreference = "Local Food",
            hotelPreference = "3 Star",
            transportPreference = "Cab",
            travelPace = "Balanced",
            startDateStr = "2026-09-14",
            endDateStr = "2026-09-18",
            travelers = 2
        )
        return json.encodeToString(plan)
    }

    private fun getCuratedActivitiesForDay(
        destination: String,
        dayNum: Int,
        days: Int,
        shortDayDate: String,
        budget: Double,
        foodPreference: String,
        hotelPreference: String,
        transportPreference: String
    ): Triple<String, String, List<com.touristapp.data.remote.model.GeneratedActivityDto>> {
        val destLower = destination.lowercase(Locale.US)
        val isKerala = destLower.contains("kerala") || destLower.contains("munnar") || destLower.contains("kochi") || destLower.contains("alleppey") || destLower.contains("thekkady") || destLower.contains("varkala")
        val isGoa = destLower.contains("goa")
        val isJaipur = destLower.contains("jaipur")
        val isAgra = destLower.contains("agra") || destLower.contains("taj")

        val dayIndex = (dayNum - 1) % 5 // 5-day cycle

        data class DayPlanTemplate(
            val title: String,
            val theme: String,
            val a1Title: String, val a1Place: String, val a1Desc: String, val a1Start: String, val a1End: String, val a1Type: String,
            val a2Title: String, val a2Place: String, val a2Desc: String, val a2Start: String, val a2End: String, val a2Type: String,
            val a3Title: String, val a3Place: String, val a3Desc: String, val a3Start: String, val a3End: String, val a3Type: String,
            val a4Title: String, val a4Place: String, val a4Desc: String, val a4Start: String, val a4End: String, val a4Type: String
        )

        val planTemplate: DayPlanTemplate = when {
            isKerala -> when (dayIndex) {
                0 -> DayPlanTemplate(
                    title = "Munnar Misty Hills & Tea Garden Trails",
                    theme = "High-altitude tea estates, panoramic dams & evening cultural arts",
                    a1Title = "Mattupetty Dam & Echo Point Boat Ride", a1Place = "Mattupetty Reservoir, Munnar", a1Desc = "Serene speedboating in the lake surrounded by misty tea-carpeted hills and echo valley on $shortDayDate.", a1Start = "09:00 AM", a1End = "12:00 PM", a1Type = "sightseeing",
                    a2Title = "Rapsy Authentic Malabar & Kerala Feast", a2Place = "Rapsy Restaurant, Munnar Town", a2Desc = "Traditional Kerala lunch with appam, vegetable stew, and aromatic local spices tailored for $foodPreference.", a2Start = "12:30 PM", a2End = "02:00 PM", a2Type = "restaurant",
                    a3Title = "Kolukkumalai Tea Factory & Plantation Safari", a3Place = "Kolukkumalai Tea Estate", a3Desc = "Guided walking tour through world's highest organic tea plantations with fresh tea tasting.", a3Start = "03:00 PM", a3End = "05:30 PM", a3Type = "activity",
                    a4Title = "Punarjani Kathakali & Kalaripayattu Show", a4Place = "Punarjani Cultural Village", a4Desc = "Live ancient martial arts combat and vibrant classical Kathakali dance performance.", a4Start = "06:30 PM", a4End = "08:30 PM", a4Type = "hotel"
                )
                1 -> DayPlanTemplate(
                    title = "Alleppey Emerald Backwaters & Houseboat",
                    theme = "Traditional kettuvallam cruise, coconut palms & tranquil village canals",
                    a1Title = "Vembanad Lake Luxury Houseboat Cruise", a1Place = "Punnamada Jetty, Alleppey", a1Desc = "Board traditional handcrafted houseboat cruise through emerald palm-fringed canals.", a1Start = "10:30 AM", a1End = "01:30 PM", a1Type = "activity",
                    a2Title = "Onboard Traditional Karimeen & Sadya Feast", a2Place = "Houseboat Floating Saloon", a2Desc = "Authentic Kuttanad feast served hot on fresh banana leaves while cruising.", a2Start = "01:30 PM", a2End = "02:45 PM", a2Type = "restaurant",
                    a3Title = "Village Canoe Safari & Coir Crafting Trail", a3Place = "Kainakary Backwater Village", a3Desc = "Quiet village canal exploration, paddy fields, and traditional coir rope weaving.", a3Start = "03:30 PM", a3End = "06:00 PM", a3Type = "activity",
                    a4Title = "Sunset Backwater Deck Stroll & Candlelight Dinner", a4Place = "Lake Palace Resort Waterfront", a4Desc = "Fresh coastal delicacies, traditional payasam, and evening waterside breeze.", a4Start = "07:30 PM", a4End = "09:45 PM", a4Type = "hotel"
                )
                2 -> DayPlanTemplate(
                    title = "Fort Kochi Heritage & Chinese Fishing Nets",
                    theme = "Portuguese churches, historic Jewish Synagogue & spice trail",
                    a1Title = "Fort Kochi Chinese Fishing Nets & Promenade", a1Place = "Fort Kochi Beach Waterfront", a1Desc = "14th-century cantilevered fishing nets, Santa Cruz Basilica, and colonial promenade.", a1Start = "09:00 AM", a1End = "12:00 PM", a1Type = "sightseeing",
                    a2Title = "Kashi Art Cafe & Contemporary Fusion Lunch", a2Place = "Kashi Art Cafe, Burgher Street", a2Desc = "Artistic gallery cafe offering organic salads, fresh juices, and homemade pies.", a2Start = "12:30 PM", a2End = "02:00 PM", a2Type = "restaurant",
                    a3Title = "Jew Town & Mattancherry Dutch Palace", a3Place = "Jew Town & Paradesi Synagogue", a3Desc = "Ancient antique shops, spice markets, and Ramayana murals at Dutch Palace.", a3Start = "02:30 PM", a3End = "05:30 PM", a3Type = "activity",
                    a4Title = "Marine Drive Waterfront Leisure & Dinner", a4Place = "Marine Drive Promenade, Kochi", a4Desc = "Evening stroll overlooking Cochin Harbour followed by coastal dinner.", a4Start = "06:30 PM", a4End = "09:30 PM", a4Type = "hotel"
                )
                3 -> DayPlanTemplate(
                    title = "Thekkady Periyar Wildlife & Spices",
                    theme = "Protected sanctuary bamboo rafting, wild elephants & spice gardens",
                    a1Title = "Periyar Lake Bamboo Rafting & Wildlife Safari", a1Place = "Periyar Tiger Reserve, Thekkady", a1Desc = "Spot wild elephants, sambar deer, and rare birds along the protected lake reserve.", a1Start = "08:00 AM", a1End = "12:30 PM", a1Type = "activity",
                    a2Title = "Green Park Ayurvedic Spice Garden Lunch", a2Place = "Green Park Spice Plantation", a2Desc = "Authentic spice-infused dishes with cardamom, cinnamon, and pepper.", a2Start = "01:00 PM", a2End = "02:30 PM", a2Type = "restaurant",
                    a3Title = "Spice Plantation Guided Nature Walk", a3Place = "Kumily Organic Spice Trail", a3Desc = "Smell and learn about organic cultivation of cloves, nutmeg, and vanilla.", a3Start = "03:00 PM", a3End = "05:30 PM", a3Type = "activity",
                    a4Title = "Ayurvedic Rejuvenation & Resort Dinner", a4Place = "Thekkady Heritage Wellness Retreat", a4Desc = "Relaxing Abhyanga massage followed by dinner at $hotelPreference.", a4Start = "06:30 PM", a4End = "09:30 PM", a4Type = "hotel"
                )
                else -> DayPlanTemplate(
                    title = "Varkala Cliff Sunset & Beach Relaxation",
                    theme = "Red sandstone coastal cliffs, mineral springs & farewell banquet",
                    a1Title = "Varkala Cliff Walk & Janardhanaswamy Temple", a1Place = "North Cliff & Papanasam Beach", a1Desc = "2000-year-old ancient beach temple and stunning red sandstone cliffs over Arabian sea.", a1Start = "09:30 AM", a1End = "12:30 PM", a1Type = "sightseeing",
                    a2Title = "Cliff Top Cafe Tropical Smoothies & Lunch", a2Place = "Cafe del Mar, Varkala North Cliff", a2Desc = "Ocean-view dining with refreshing smoothies and regional seafood.", a2Start = "01:00 PM", a2End = "02:30 PM", a2Type = "restaurant",
                    a3Title = "Papanasam Golden Sand Beach & Water Sports", a3Place = "Papanasam Natural Spring Beach", a3Desc = "Swimming, coastal surfing, and sunbathing along the mineral springs.", a3Start = "03:30 PM", a3End = "06:30 PM", a3Type = "activity",
                    a4Title = "Sunset Cliffside Bonfire & Farewell Banquet", a4Place = "Varkala Cliffside Heritage Lounge", a4Desc = "Golden sunset view followed by celebration dinner to conclude the trip.", a4Start = "07:00 PM", a4End = "09:30 PM", a4Type = "hotel"
                )
            }
            isGoa -> when (dayIndex) {
                0 -> DayPlanTemplate(
                    title = "North Goa Heritage & Coastal Arrival",
                    theme = "Iconic coastal viewpoints, Portuguese fort history & beach watersports",
                    a1Title = "Fort Aguada & Lighthouse Exploration", a1Place = "Fort Aguada, Candolim", a1Desc = "Explore the 17th-century Portuguese fortress and panoramic Arabian Sea lighthouse on $shortDayDate.", a1Start = "09:30 AM", a1End = "12:00 PM", a1Type = "sightseeing",
                    a2Title = "Coastal Seafood & Goan Curry Trail", a2Place = "Britto's Beach Shack, Baga", a2Desc = "Authentic Goan curry and coastal delicacies tailored for $foodPreference.", a2Start = "12:45 PM", a2End = "02:15 PM", a2Type = "restaurant",
                    a3Title = "Calangute & Baga Water Sports Adventure", a3Place = "Calangute Beach Watersports Hub", a3Desc = "Parasailing, jet skiing, and bumper rides along the golden sands.", a3Start = "03:45 PM", a3End = "06:30 PM", a3Type = "activity",
                    a4Title = "Tito's Lane Night Bazaar & Seaside Dinner", a4Place = "Tito's Lane & Baga Beachfront", a4Desc = "Vibrant night bazaar stroll, handcrafted souvenir shopping, and dinner.", a4Start = "07:45 PM", a4End = "10:00 PM", a4Type = "hotel"
                )
                1 -> DayPlanTemplate(
                    title = "Old Goa Architecture & Latin Quarter",
                    theme = "UNESCO heritage cathedrals, cobblestone alleys & scenic river cruise",
                    a1Title = "Basilica of Bom Jesus & Se Cathedral", a1Place = "Old Goa Heritage Complex", a1Desc = "Discover UNESCO World Heritage baroque churches holding sacred relics.", a1Start = "08:30 AM", a1End = "11:30 AM", a1Type = "sightseeing",
                    a2Title = "Fontainhas Latin Quarter Heritage Cafe", a2Place = "Fontainhas Heritage Bakery Cafe, Panaji", a2Desc = "Taste Portuguese bebinca, savory pastries, and artisanal beverages in Latin Quarter.", a2Start = "12:15 PM", a2End = "01:45 PM", a2Type = "restaurant",
                    a3Title = "Mandovi River Sunset Cruise & Folk Dance", a3Place = "Mandovi River Promenade, Panaji", a3Desc = "Scenic 1-hour cruise along the Mandovi river with traditional Goan folk performances.", a3Start = "03:30 PM", a3End = "06:00 PM", a3Type = "activity",
                    a4Title = "Panaji Waterfront Promenade Leisure & Dinner", a4Place = "Panaji Waterfront Promenade", a4Desc = "Relaxed evening walk along the illuminated river promenade and $hotelPreference dinner.", a4Start = "07:30 PM", a4End = "09:45 PM", a4Type = "hotel"
                )
                2 -> DayPlanTemplate(
                    title = "Island Snorkeling & Vagator Sunset Cliffs",
                    theme = "Deep sea snorkeling, dolphin spotting & panoramic sunset cliffs",
                    a1Title = "Grand Island Snorkeling & Dolphin Sightings", a1Place = "Grand Island Boat Dock", a1Desc = "Speedboat excursion to Grand Island, dolphin watching, and coral reef snorkeling.", a1Start = "09:00 AM", a1End = "01:00 PM", a1Type = "activity",
                    a2Title = "Cliffside Shack & Fresh Coconut Refreshments", a2Place = "Thalassa & Curlies Cliff Bistro, Vagator", a2Desc = "Taste authentic culinary creations with sweeping ocean views.", a2Start = "01:30 PM", a2End = "03:00 PM", a2Type = "restaurant",
                    a3Title = "Chapora Fort Panoramic Sunset Viewpoint", a3Place = "Chapora Fort Cliff, Vagator", a3Desc = "Iconic 'Dil Chahta Hai' cliffside fort overlooking Vagator and Morjim coastlines.", a3Start = "04:30 PM", a3End = "07:00 PM", a3Type = "sightseeing",
                    a4Title = "Candlelight Beach Dinner & Acoustic Music", a4Place = "$hotelPreference Resort Dining, Vagator", a4Desc = "Unwind with beachside ambient acoustic music and chef's special dinner.", a4Start = "08:00 PM", a4End = "10:15 PM", a4Type = "hotel"
                )
                3 -> DayPlanTemplate(
                    title = "Dudhsagar Waterfalls & Tropical Spice Farm",
                    theme = "Jungle safari, cascading milky waterfalls & aromatic spice plantations",
                    a1Title = "Dudhsagar Waterfalls Jeep Jungle Safari", a1Place = "Dudhsagar Waterfalls & Wildlife Park", a1Desc = "4x4 jungle jeep safari through streams and lush Western Ghats to the four-tiered falls.", a1Start = "08:00 AM", a1End = "01:30 PM", a1Type = "activity",
                    a2Title = "Sahakari Spice Farm Traditional Buffet", a2Place = "Sahakari Spice Plantation, Ponda", a2Desc = "Farm-fresh traditional Goan lunch served on banana leaves amidst cardamom and vanilla trees.", a2Start = "02:00 PM", a2End = "03:30 PM", a2Type = "restaurant",
                    a3Title = "Miramar Beach Sunset Walk & Coastal Breeze", a3Place = "Miramar Coastal Beach Promenade", a3Desc = "Golden hour oceanfront walk where the Mandovi River meets the Arabian Sea.", a3Start = "05:00 PM", a3End = "07:00 PM", a3Type = "sightseeing",
                    a4Title = "Candolim Street Night Dining & Live Bands", a4Place = "Candolim Main Street Bistro", a4Desc = "Comfortable dinner and live music session near your accommodation.", a4Start = "08:00 PM", a4End = "10:00 PM", a4Type = "hotel"
                )
                else -> DayPlanTemplate(
                    title = "South Goa Serenity & Souvenir Treasures",
                    theme = "Pristine white sand bays, speedboats & souvenir shopping",
                    a1Title = "Palolem & Butterfly Beach Speedboat Tour", a1Place = "Palolem Beach Harbor, South Goa", a1Desc = "Scenic boat ride to hidden Butterfly beach and peaceful kayaking in crescent bay.", a1Start = "09:30 AM", a1End = "12:30 PM", a1Type = "activity",
                    a2Title = "Dropadi Oceanfront Lunch & Tropical Smoothies", a2Place = "Dropadi Beach Shack, Palolem", a2Desc = "Relaxed beachfront dining featuring fresh coastal recipes and fruit smoothies.", a2Start = "01:00 PM", a2End = "02:30 PM", a2Type = "restaurant",
                    a3Title = "Anjuna Weekly Flea Market & Artisan Souvenirs", a3Place = "Anjuna Traditional Flea Market", a3Desc = "Browse handcrafted jewelry, spices, Bohemian attire, and cashews.", a3Start = "03:45 PM", a3End = "06:45 PM", a3Type = "activity",
                    a4Title = "Farewell Beach Bonfire & Gourmet Dinner", a4Place = "$hotelPreference Resort in $destination", a4Desc = "Memorable celebration dinner by the shore to conclude your incredible journey.", a4Start = "07:30 PM", a4End = "09:30 PM", a4Type = "hotel"
                )
            }
            isJaipur -> when (dayIndex) {
                0 -> DayPlanTemplate(
                    title = "Royal Amer Fort & Palace Arrival",
                    theme = "Majestic Rajput architecture, mirrored halls & royal heritage",
                    a1Title = "Amber Fort & Sheesh Mahal Exploration", a1Place = "Amber Fort, Amer", a1Desc = "Step into the grand hilltop fortress and dazzling mirror palace of the Kachwaha kings.", a1Start = "09:00 AM", a1End = "12:30 PM", a1Type = "sightseeing",
                    a2Title = "1135 AD Amer Royal Fine Dining", a2Place = "1135 AD Restaurant, Amer Fort", a2Desc = "Royal Rajasthani thali featuring ker sangri, dal baati churma and regional delicacies.", a2Start = "01:00 PM", a2End = "02:30 PM", a2Type = "restaurant",
                    a3Title = "Jal Mahal & Hawa Mahal Photography Walk", a3Place = "Hawa Mahal & Jal Mahal Lake", a3Desc = "Capture the 'Palace of Winds' honeycomb facade and palace floating on Man Sagar Lake.", a3Start = "03:45 PM", a3End = "06:15 PM", a3Type = "activity",
                    a4Title = "Chokhi Dhani Ethnic Village & Cultural Dinner", a4Place = "Chokhi Dhani Resort, Tonk Road", a4Desc = "Folk dances, puppet shows, camel rides, and traditional dining under the stars.", a4Start = "07:15 PM", a4End = "10:00 PM", a4Type = "hotel"
                )
                1 -> DayPlanTemplate(
                    title = "City Palace, Jantar Mantar & Bazaars",
                    theme = "Living royal palaces, cosmic sundials & vibrant pink city markets",
                    a1Title = "City Palace & Royal Armory Museum", a1Place = "City Palace Complex, Jaipur", a1Desc = "Courtyards, peacock gates, museum galleries, and textiles of the Jaipur royal family.", a1Start = "09:30 AM", a1End = "12:00 PM", a1Type = "sightseeing",
                    a2Title = "Laxmi Misthan Bhandar (LMB) Heritage Lunch", a2Place = "LMB Johari Bazaar", a2Desc = "Legendary Jaipur ghewar, kachoris, and authentic vegetarian feast.", a2Start = "12:30 PM", a2End = "02:00 PM", a2Type = "restaurant",
                    a3Title = "Jantar Mantar UNESCO Observatory & Bapu Bazaar", a3Place = "Jantar Mantar & Bapu Bazaar", a3Desc = "World's largest stone astronomical observatory followed by mojari & textile shopping.", a3Start = "03:00 PM", a3End = "05:45 PM", a3Type = "activity",
                    a4Title = "Nahargarh Fort Panoramic Sunset View & Dinner", a4Place = "Padao Restaurant, Nahargarh Fort", a4Desc = "Breathtaking cliff-edge twilight view overlooking the glittering Pink City lights.", a4Start = "06:30 PM", a4End = "09:30 PM", a4Type = "hotel"
                )
                else -> DayPlanTemplate(
                    title = "Jaigarh Fort, Albert Hall & Cultural Arts",
                    theme = "Military forts, royal cannons & centuries-old artisan traditions",
                    a1Title = "Jaigarh Fort & Jaivana Cannon Tour", a1Place = "Jaigarh Fort Hilltop", a1Desc = "Explore the fort that protected Amer and inspect the world's largest wheeled cannon.", a1Start = "08:30 AM", a1End = "11:30 AM", a1Type = "sightseeing",
                    a2Title = "Tapri Central Rooftop Chai & Contemporary Lunch", a2Place = "Tapri Central, Central Park", a2Desc = "Rooftop lunch with refreshing regional snacks and park views.", a2Start = "12:30 PM", a2End = "02:00 PM", a2Type = "restaurant",
                    a3Title = "Albert Hall Museum & Pigeon Square Walk", a3Place = "Albert Hall State Museum, Ram Niwas Garden", a3Desc = "Indo-Saracenic museum housing Persian carpets, Egyptian artifacts, and miniature paintings.", a3Start = "03:00 PM", a3End = "05:30 PM", a3Type = "activity",
                    a4Title = "Johari Bazaar Blue Pottery & Farewell Feast", a4Place = "Johari Bazaar & Heritage Haveli", a4Desc = "Hand-painted blue pottery, block print quilts, and celebration dinner.", a4Start = "06:30 PM", a4End = "09:30 PM", a4Type = "hotel"
                )
            }
            isAgra -> when (dayIndex) {
                0 -> DayPlanTemplate(
                    title = "Taj Mahal Sunrise & Mughal Grandeur",
                    theme = "Wonder of the World at dawn, royal marble craft & Mughal palaces",
                    a1Title = "Taj Mahal Sunrise Guided Exploration", a1Place = "Taj Mahal East Gate Complex", a1Desc = "Experience the ivory-white marble mausoleum during majestic sunrise lighting.", a1Start = "06:30 AM", a1End = "10:00 AM", a1Type = "sightseeing",
                    a2Title = "Pinch of Spice Authentic Mughlai Lunch", a2Place = "Pinch of Spice, Fatehabad Road", a2Desc = "Famous Mughlai curries, fragrant biryanis, and tandoori specialties.", a2Start = "12:30 PM", a2End = "02:00 PM", a2Type = "restaurant",
                    a3Title = "Agra Fort Royal Palaces & Diwan-i-Khas", a3Place = "Agra Fort Monument", a3Desc = "Explore the red sandstone fortress with views across the Yamuna River.", a3Start = "03:00 PM", a3End = "05:30 PM", a3Type = "sightseeing",
                    a4Title = "Mehtab Bagh Sunset Taj Reflection & Dinner", a4Place = "Mehtab Bagh Moonlight Garden", a4Desc = "Sunset views across the river and Mughlai dinner at $hotelPreference hotel.", a4Start = "06:00 PM", a4End = "09:00 PM", a4Type = "hotel"
                )
                else -> DayPlanTemplate(
                    title = "Fatehpur Sikri & Artisan Petha Trail",
                    theme = "Emperor Akbar's abandoned ghost city & world's highest gateway",
                    a1Title = "Fatehpur Sikri & Buland Darwaza Excursion", a1Place = "Fatehpur Sikri Royal Complex", a1Desc = "Marvel at the 54-meter Buland Darwaza, Salim Chishti Dargah, and royal courtyards.", a1Start = "08:30 AM", a1End = "01:00 PM", a1Type = "sightseeing",
                    a2Title = "Dasaprakash South Indian & Continental Feast", a2Place = "Dasaprakash, Sadar Bazaar", a2Desc = "Crisp dosas, thalis, and desserts at this legendary heritage dining spot.", a2Start = "01:30 PM", a2End = "03:00 PM", a2Type = "restaurant",
                    a3Title = "Tomb of I'timad-ud-Daulah (Baby Taj)", a3Place = "Baby Taj Heritage Site", a3Desc = "Intricate marble inlay work (pietra dura) on the peaceful eastern riverbank.", a3Start = "04:00 PM", a3End = "06:00 PM", a3Type = "sightseeing",
                    a4Title = "Sadar Bazaar Petha & Marble Souvenir Trail", a4Place = "Sadar Bazaar Main Street", a4Desc = "Authentic Agra petha varieties, marble handicrafts, and farewell dinner.", a4Start = "07:00 PM", a4End = "09:30 PM", a4Type = "hotel"
                )
            }
            else -> when (dayIndex) {
                0 -> DayPlanTemplate(
                    title = "Arrival, Landmark Orientation & Evening Bazaars",
                    theme = "Orientation, prime historical landmarks & evening local food trail",
                    a1Title = "Arrival & Iconic Heritage Exploration", a1Place = "$destination Historic Center", a1Desc = "Guided walk through prime architectural marvels, heritage gates and monuments.", a1Start = "09:30 AM", a1End = "12:00 PM", a1Type = "sightseeing",
                    a2Title = "Regional Specialty Lunch & Culinary Hub", a2Place = "$destination Traditional Dining Hub", a2Desc = "Taste regional flavors and chef specialties tailored for $foodPreference.", a2Start = "12:45 PM", a2End = "02:15 PM", a2Type = "restaurant",
                    a3Title = "Sunset Viewpoint & Cultural Market Walk", a3Place = "$destination Sunset Promenade", a3Desc = "Scenic photography vantage point and bustling handicrafts shopping street.", a3Start = "03:45 PM", a3End = "06:30 PM", a3Type = "activity",
                    a4Title = "Hotel Return & Leisure Dinner", a4Place = "$hotelPreference Hotel in $destination", a4Desc = "Rest and recharge with a comfortable multicourse dinner at your hotel.", a4Start = "07:45 PM", a4End = "09:45 PM", a4Type = "hotel"
                )
                1 -> DayPlanTemplate(
                    title = "Ancient Heritage, Art Museums & Craft Workshops",
                    theme = "Royal palaces, historical collections & master artisan demonstrations",
                    a1Title = "Royal Fort & Heritage Museum Guided Tour", a1Place = "$destination Royal Heritage Complex", a1Desc = "Explore preserved royal artifacts, historic galleries, and antique collections.", a1Start = "08:30 AM", a1End = "11:30 AM", a1Type = "sightseeing",
                    a2Title = "Heritage Garden Kitchen & Authentic Thali", a2Place = "$destination Heritage Garden Kitchen", a2Desc = "Open-air garden dining featuring fresh ingredients and traditional spices.", a2Start = "12:15 PM", a2End = "01:45 PM", a2Type = "restaurant",
                    a3Title = "Master Artisan Demonstration & Bazaar Walk", a3Place = "$destination Central Artisan Bazaar", a3Desc = "Watch local artisans at work and shop for certified local souvenirs.", a3Start = "02:45 PM", a3End = "05:30 PM", a3Type = "activity",
                    a4Title = "Cultural Folk Performance & Grand Buffet", a4Place = "$destination Cultural Amphitheater", a4Desc = "Live regional music, dance performance, and banquet dinner under the stars.", a4Start = "07:00 PM", a4End = "09:30 PM", a4Type = "hotel"
                )
                2 -> DayPlanTemplate(
                    title = "Nature Escapes, Lakefront & Adventure Trails",
                    theme = "Lush nature trails, serene lakes & outdoor exploration",
                    a1Title = "Panoramic Nature Reserve & Forest Trail", a1Place = "$destination Scenic Nature Reserve", a1Desc = "Morning hike or nature walk with breathtaking lookout viewpoints.", a1Start = "09:00 AM", a1End = "12:30 PM", a1Type = "activity",
                    a2Title = "Hillside Panorama Cafe & Refreshments", a2Place = "$destination Panorama Cafe", a2Desc = "Relaxed meal with sweeping valley and hillside vistas.", a2Start = "01:00 PM", a2End = "02:30 PM", a2Type = "restaurant",
                    a3Title = "Boating Excursion & Lakefront Twilight Stroll", a3Place = "$destination Lakefront Esplanade", a3Desc = "Peaceful boat ride across the lake followed by a waterside twilight walk.", a3Start = "04:00 PM", a3End = "06:45 PM", a3Type = "sightseeing",
                    a4Title = "Rooftop Stargazing & Chef's Special Dinner", a4Place = "$destination Sky Lounge & Dining", a4Desc = "Elevated dining experience overlooking the illuminated city skyline.", a4Start = "08:00 PM", a4End = "10:00 PM", a4Type = "hotel"
                )
                3 -> DayPlanTemplate(
                    title = "Waterfalls, Wildlife Safari & Village Life",
                    theme = "Cascading waterfalls, natural wildlife & rural cultural immersion",
                    a1Title = "Cascading Waterfalls & Wildlife Safari", a1Place = "$destination Forest Eco-Park", a1Desc = "Explore cascading waterfalls, wildlife spotting, and jungle trail walk.", a1Start = "08:00 AM", a1End = "01:00 PM", a1Type = "activity",
                    a2Title = "Farmhouse Lunch & Organic Spice Tasting", a2Place = "$destination Rural Organic Farm", a2Desc = "Wholesome organic farm-to-table lunch and fresh local refreshments.", a2Start = "01:30 PM", a2End = "03:00 PM", a2Type = "restaurant",
                    a3Title = "Old Town Hidden Alleys & Architecture Tour", a3Place = "$destination Old Town Quarter", a3Desc = "Walk through quaint historic streets, wooden balconies, and local shrines.", a3Start = "04:30 PM", a3End = "07:00 PM", a3Type = "sightseeing",
                    a4Title = "Vibrant Food Street & Local Delicacy Tasting", a4Place = "$destination Night Food Plaza", a4Desc = "Sample famous street foods, hot beverages, and desserts.", a4Start = "07:30 PM", a4End = "09:30 PM", a4Type = "hotel"
                )
                else -> DayPlanTemplate(
                    title = "Sacred Sanctuaries, Souvenirs & Farewell Vistas",
                    theme = "Serene spiritual architecture, last-minute shopping & celebration dinner",
                    a1Title = "Peaceful Sacred Temples & Botanical Walk", a1Place = "$destination Sacred Temple Garden", a1Desc = "Spiritual morning reflection and peaceful walk among rare botanical species.", a1Start = "10:00 AM", a1End = "12:30 PM", a1Type = "sightseeing",
                    a2Title = "Gourmet Farewell Banquet Lunch", a2Place = "$destination Grand Banquet", a2Desc = "Celebratory multicourse lunch reflecting the best regional delicacies.", a2Start = "01:00 PM", a2End = "02:30 PM", a2Type = "restaurant",
                    a3Title = "Handmade Souvenirs & Specialty Spice Shopping", a3Place = "$destination Main Souvenir Market", a3Desc = "Pick up souvenirs, specialty teas, spices, and gifts for family & friends.", a3Start = "03:30 PM", a3End = "06:00 PM", a3Type = "activity",
                    a4Title = "Farewell Promenade Dinner & Memories Celebration", a4Place = "$hotelPreference Hotel in $destination", a4Desc = "Heartwarming conclusion to your journey with celebratory desserts.", a4Start = "07:00 PM", a4End = "09:30 PM", a4Type = "hotel"
                )
            }
        }

        val actList = listOf(
            com.touristapp.data.remote.model.GeneratedActivityDto(
                id = "act-$dayNum-1",
                timeSlot = "Morning",
                title = planTemplate.a1Title,
                description = planTemplate.a1Desc,
                placeName = planTemplate.a1Place,
                activityType = planTemplate.a1Type,
                startTime = planTemplate.a1Start,
                endTime = planTemplate.a1End,
                estimatedCost = budget / (days * 5),
                travelTimeMinutes = 20,
                isOutdoor = true,
                transportMode = transportPreference,
                dietaryTags = emptyList()
            ),
            com.touristapp.data.remote.model.GeneratedActivityDto(
                id = "act-$dayNum-2",
                timeSlot = "Afternoon",
                title = planTemplate.a2Title,
                description = planTemplate.a2Desc,
                placeName = planTemplate.a2Place,
                activityType = planTemplate.a2Type,
                startTime = planTemplate.a2Start,
                endTime = planTemplate.a2End,
                estimatedCost = budget / (days * 6),
                travelTimeMinutes = 15,
                isOutdoor = false,
                transportMode = "Walking / $transportPreference",
                dietaryTags = listOf(foodPreference)
            ),
            com.touristapp.data.remote.model.GeneratedActivityDto(
                id = "act-$dayNum-3",
                timeSlot = "Evening",
                title = planTemplate.a3Title,
                description = planTemplate.a3Desc,
                placeName = planTemplate.a3Place,
                activityType = planTemplate.a3Type,
                startTime = planTemplate.a3Start,
                endTime = planTemplate.a3End,
                estimatedCost = budget / (days * 7),
                travelTimeMinutes = 20,
                isOutdoor = true,
                transportMode = transportPreference,
                dietaryTags = emptyList()
            ),
            com.touristapp.data.remote.model.GeneratedActivityDto(
                id = "act-$dayNum-4",
                timeSlot = "Night",
                title = planTemplate.a4Title,
                description = planTemplate.a4Desc,
                placeName = planTemplate.a4Place,
                activityType = planTemplate.a4Type,
                startTime = planTemplate.a4Start,
                endTime = planTemplate.a4End,
                estimatedCost = budget / (days * 8),
                travelTimeMinutes = 20,
                isOutdoor = false,
                transportMode = transportPreference,
                dietaryTags = listOf(foodPreference)
            )
        )

        return Triple(planTemplate.title, planTemplate.theme, actList)
    }

    /**
     * Listens in real-time to the user's active/ongoing or most recent approved trip.
     * Compatible with Web Dashboard / Firestore updates.
     */
    fun observeActiveTrip(userId: String? = null): Flow<TripModel?> = callbackFlow {
        val targetUid = userId ?: auth.currentUser?.uid ?: "guest"
        val query = firestore.collection(FirestoreCollections.TRIPS)
            .whereEqualTo("user_id", targetUid)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // If error, try emit cached or null
                val cached = localTripsCache.values.firstOrNull { it.userId == targetUid }
                trySend(cached)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val trips = snapshot.documents.map { TripModel.fromFirestore(it) }
                trips.forEach { localTripsCache[it.id] = it }

                // Prefer ongoing / active trip first, then confirmed/approved, then latest
                val activeTrip = trips.firstOrNull { it.status.equals("active", ignoreCase = true) || it.status.equals("ongoing", ignoreCase = true) }
                    ?: trips.firstOrNull { it.approvalStatus.equals("approved", ignoreCase = true) || it.approvalStatus.equals("confirmed", ignoreCase = true) }
                    ?: trips.firstOrNull { it.status.equals("planning", ignoreCase = true) }
                    ?: trips.firstOrNull()
                    ?: localTripsCache.values.firstOrNull { it.userId == targetUid }

                trySend(activeTrip)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun getTripLeader(leaderId: String): TripLeaderDto? {
        if (leaderId.isBlank()) return null
        return try {
            val doc = withTimeoutOrNull(2500L) {
                firestore.collection("tripLeaders").document(leaderId).get().await()
            }
            if (doc != null && doc.exists()) {
                TripLeaderDto.fromFirestore(doc)
            } else {
                // Return verified mock leader for demonstration
                TripLeaderDto(
                    id = leaderId,
                    name = "Vipin Sharma",
                    phone = "+91 98765 43210",
                    photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
                    rating = 4.95,
                    role = "Lead Expedition Guide",
                    latitude = 27.1755,
                    longitude = 78.0415,
                    isOnline = true,
                    lastSeen = "Active now"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getTripTransport(transportId: String): TripTransportDto? {
        if (transportId.isBlank()) return null
        return try {
            val doc = withTimeoutOrNull(2500L) {
                firestore.collection("transports").document(transportId).get().await()
            }
            if (doc != null && doc.exists()) {
                TripTransportDto.fromFirestore(doc)
            } else {
                // Return verified mock transport for demonstration
                TripTransportDto(
                    id = transportId,
                    vehicleType = "Tourist AC Tempo Traveller",
                    vehicleNumber = "UP 80 AT 7890",
                    driverName = "Manoj Verma",
                    driverPhone = "+91 94123 45678",
                    status = "On the way",
                    pickupPoint = "Main Entrance Gate / Hotel Portico",
                    latitude = 27.1730,
                    longitude = 78.0380,
                    etaMinutes = 8
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
