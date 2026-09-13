package com.touristapp.presentation.track

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.location.LocationService
import com.touristapp.data.models.TripLeaderDto
import com.touristapp.data.models.TripModel
import com.touristapp.data.models.TripTransportDto
import com.touristapp.data.repository.TripRepository
import com.touristapp.di.ServiceLocator
import com.touristapp.presentation.trips.ParsedDay
import com.touristapp.presentation.trips.TimelineItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class TrackTripUiState(
    val trip: TripModel? = null,
    val parsedDays: List<ParsedDay> = emptyList(),
    val currentDayIndex: Int = 1,
    val totalDays: Int = 3,
    val progressPercent: Float = 0.5f,
    val statusText: String = "Trip in Progress",
    val nextStopItem: TimelineItem? = null,
    val nextStopDistance: String = "Calculating...",
    val nextStopEta: String = "12 min",
    val nextStopLat: Double = 0.0,
    val nextStopLon: Double = 0.0,
    val leader: TripLeaderDto? = null,
    val leaderDistance: String? = null,
    val transport: TripTransportDto? = null,
    val userLocation: Location? = null,
    val isLocationPermissionGranted: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class TrackTripViewModel(
    private val tripRepository: TripRepository = ServiceLocator.tripRepository,
    private val locationService: LocationService = ServiceLocator.locationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackTripUiState())
    val uiState: StateFlow<TrackTripUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndStartLocation()
        observeActiveTrip()
    }

    fun checkPermissionAndStartLocation() {
        val hasPermission = locationService.hasLocationPermission()
        _uiState.update { it.copy(isLocationPermissionGranted = hasPermission) }
        if (hasPermission) {
            viewModelScope.launch {
                val loc = locationService.getCurrentLocation()
                if (loc != null) {
                    _uiState.update { it.copy(userLocation = loc) }
                    recalculateDistances(loc)
                }
            }
            viewModelScope.launch {
                locationService.observeLocationUpdates().collect { loc ->
                    _uiState.update { it.copy(userLocation = loc) }
                    recalculateDistances(loc)
                }
            }
        }
    }

    fun refresh() {
        observeActiveTrip()
        checkPermissionAndStartLocation()
    }

    private fun observeActiveTrip() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            tripRepository.observeActiveTrip().collect { trip ->
                if (trip != null) {
                    processTrip(trip)
                } else {
                    // Fallback to sample active trip so the user always has a complete experience
                    val sample = createSampleActiveTrip()
                    processTrip(sample)
                }
            }
        }
    }

    private fun processTrip(trip: TripModel) {
        viewModelScope.launch {
            val days = parseItineraryDays(trip.itineraryJson, trip.daysCount, trip.destinationName)
            val (currentDay, progress, statusBanner) = calculateDynamicTripProgress(trip)

            // Identify current / next stop item
            val todayDay = days.getOrNull((currentDay - 1).coerceAtLeast(0)) ?: days.firstOrNull()
            val nextItem = todayDay?.items?.firstOrNull { it.status != "completed" }
                ?: todayDay?.items?.firstOrNull()

            // Resolve next stop coordinates
            val destCoords = resolveCoordinates(nextItem?.location ?: trip.destinationName)

            // Leader & Transport info
            val leader = trip.leaderId?.let { tripRepository.getTripLeader(it) }
                ?: tripRepository.getTripLeader("leader_sample")
            val transport = trip.transportId?.let { tripRepository.getTripTransport(it) }
                ?: tripRepository.getTripTransport("transport_sample")

            _uiState.update { state ->
                state.copy(
                    trip = trip,
                    parsedDays = days,
                    currentDayIndex = currentDay,
                    totalDays = trip.daysCount.coerceAtLeast(1),
                    progressPercent = progress,
                    statusText = statusBanner,
                    nextStopItem = nextItem,
                    nextStopLat = destCoords.first,
                    nextStopLon = destCoords.second,
                    leader = leader,
                    transport = transport,
                    isLoading = false,
                    errorMessage = null
                )
            }
            _uiState.value.userLocation?.let { recalculateDistances(it) }
        }
    }

    private fun recalculateDistances(userLoc: Location) {
        val state = _uiState.value
        if (state.nextStopLat != 0.0 && state.nextStopLon != 0.0) {
            val distance = LocationService.calculateDistance(
                userLoc.latitude, userLoc.longitude,
                state.nextStopLat, state.nextStopLon
            )
            val formattedDist = LocationService.formatDistance(distance)
            val eta = LocationService.calculateEta(distance)
            _uiState.update { it.copy(nextStopDistance = "$formattedDist away", nextStopEta = "ETA $eta") }
        }

        state.leader?.let { leader ->
            if (leader.latitude != null && leader.longitude != null) {
                val dist = LocationService.calculateDistance(
                    userLoc.latitude, userLoc.longitude,
                    leader.latitude, leader.longitude
                )
                _uiState.update { it.copy(leaderDistance = "${LocationService.formatDistance(dist)} away") }
            }
        }
    }

    private fun calculateDynamicTripProgress(trip: TripModel): Triple<Int, Float, String> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = sdf.parse(trip.startDate)
            val end = sdf.parse(trip.endDate)
            val now = Date()

            if (start != null && end != null) {
                val totalDurationMs = (end.time - start.time).coerceAtLeast(1L)
                val elapsedMs = (now.time - start.time)

                if (now.before(start)) {
                    val daysUntil = ((start.time - now.time) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                    Triple(1, 0f, "Trip starts in $daysUntil days")
                } else if (now.after(end)) {
                    Triple(trip.daysCount, 1f, "Trip Completed")
                } else {
                    val currentDay = ((elapsedMs / (1000 * 60 * 60 * 24)) + 1).toInt().coerceIn(1, trip.daysCount)
                    val progress = (elapsedMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    Triple(currentDay, progress, "Trip in Progress • Day $currentDay of ${trip.daysCount}")
                }
            } else {
                Triple(1, 0.4f, "Trip in Progress")
            }
        } catch (_: Exception) {
            Triple(1, 0.5f, "Trip in Progress • Day 1 of ${trip.daysCount}")
        }
    }

    fun loadTrip(tripId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val trip = tripRepository.getTripById(tripId)
            if (trip != null) {
                processTrip(trip)
            } else {
                observeActiveTrip()
            }
        }
    }

    private fun resolveCoordinates(locationName: String): Pair<Double, Double> {
        val loc = locationName.lowercase()
        return when {
            // Goa landmarks
            loc.contains("baga") -> Pair(15.5553, 73.7517)
            loc.contains("aguada") || loc.contains("candolim") -> Pair(15.4920, 73.7736)
            loc.contains("calangute") -> Pair(15.5430, 73.7554)
            loc.contains("anjuna") || loc.contains("flea market") -> Pair(15.5808, 73.7432)
            loc.contains("vagator") || loc.contains("chapora") || loc.contains("thalassa") || loc.contains("curlies") -> Pair(15.6026, 73.7346)
            loc.contains("panaji") || loc.contains("panjim") || loc.contains("fontainhas") || loc.contains("mandovi") -> Pair(15.4989, 73.8278)
            loc.contains("bom jesus") || loc.contains("se cathedral") || loc.contains("old goa") -> Pair(15.5009, 73.9116)
            loc.contains("dudhsagar") -> Pair(15.3144, 74.3143)
            loc.contains("palolem") || loc.contains("butterfly") -> Pair(15.0100, 74.0232)
            loc.contains("goa") -> Pair(15.4989, 73.8278)

            // Jaipur landmarks
            loc.contains("amber") || loc.contains("amer") -> Pair(26.9855, 75.8513)
            loc.contains("hawa mahal") -> Pair(26.9239, 75.8267)
            loc.contains("city palace") -> Pair(26.9258, 75.8237)
            loc.contains("jal mahal") -> Pair(26.9656, 75.8456)
            loc.contains("jantar mantar") -> Pair(26.9248, 75.8246)
            loc.contains("nahargarh") -> Pair(26.9374, 75.8155)
            loc.contains("jaigarh") -> Pair(26.9850, 75.8480)
            loc.contains("chokhi dhani") -> Pair(26.7663, 75.8362)
            loc.contains("bapu bazaar") || loc.contains("johari") -> Pair(26.9196, 75.8242)
            loc.contains("albert hall") -> Pair(26.9116, 75.8195)
            loc.contains("jaipur") -> Pair(26.9124, 75.7873)

            // Agra landmarks
            loc.contains("taj mahal") || loc.contains("taj") -> Pair(27.1751, 78.0421)
            loc.contains("agra fort") -> Pair(27.1795, 78.0211)
            loc.contains("mehtab bagh") -> Pair(27.1800, 78.0460)
            loc.contains("fatehpur") || loc.contains("sikri") || loc.contains("buland") -> Pair(27.0945, 77.6679)
            loc.contains("baby taj") || loc.contains("itmad") -> Pair(27.1929, 78.0310)
            loc.contains("sadar bazaar") -> Pair(27.1585, 78.0081)
            loc.contains("agra") -> Pair(27.1767, 78.0081)

            // Other major destinations
            loc.contains("red fort") || loc.contains("delhi") -> Pair(28.6562, 77.2410)
            loc.contains("mumbai") || loc.contains("marine drive") -> Pair(18.9432, 72.8230)
            loc.contains("udaipur") -> Pair(24.5854, 73.7125)
            loc.contains("manali") -> Pair(32.2396, 77.1887)
            loc.contains("rishikesh") -> Pair(30.0869, 78.2676)
            loc.contains("varanasi") -> Pair(25.3176, 82.9739)
            loc.contains("amritsar") -> Pair(31.6200, 74.8765)
            else -> Pair(15.4989, 73.8278) // default to Goa
        }
    }

    private fun createSampleActiveTrip(): TripModel {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 4)
        val endStr = sdf.format(cal.time)

        return TripModel(
            id = "trip_active_goa",
            title = "Goa Explorer & Beachside Retreat",
            destinationName = "Goa",
            destinationId = "dest_goa",
            startDate = startStr,
            endDate = endStr,
            status = "active",
            daysCount = 5,
            travelersCount = 2,
            budgetTotal = 24000.0,
            leaderId = "leader_sample",
            transportId = "transport_sample",
            approvalStatus = "approved"
        )
    }

    private fun parseItineraryDays(jsonStr: String, daysCount: Int, destination: String): List<ParsedDay> {
        if (jsonStr.isNotBlank()) {
            try {
                val root = JSONObject(jsonStr)
                val daysArray = root.optJSONArray("days")
                if (daysArray != null && daysArray.length() > 0) {
                    val list = mutableListOf<ParsedDay>()
                    for (i in 0 until daysArray.length()) {
                        val dObj = daysArray.getJSONObject(i)
                        val timelineList = mutableListOf<TimelineItem>()
                        val itemsArray = dObj.optJSONArray("items") ?: dObj.optJSONArray("activities")
                        if (itemsArray != null) {
                            for (j in 0 until itemsArray.length()) {
                                val aObj = itemsArray.getJSONObject(j)
                                val status = if (j < 2) "completed" else if (j == 2) "current" else "upcoming"
                                val rawLoc = aObj.optString("place_name", aObj.optString("placeName", aObj.optString("location", "")))
                                val loc = if (rawLoc.isNotBlank()) rawLoc else destination
                                timelineList.add(
                                    TimelineItem(
                                        sequence = j + 1,
                                        startTime = aObj.optString("startTime", aObj.optString("start_time", "09:00 AM")),
                                        endTime = aObj.optString("endTime", aObj.optString("end_time", "11:00 AM")),
                                        type = aObj.optString("type", aObj.optString("activity_type", "activity")),
                                        title = aObj.optString("title", "Explore $destination"),
                                        location = loc,
                                        description = aObj.optString("description", "Sightseeing and local culture"),
                                        status = status
                                    )
                                )
                            }
                        }
                        list.add(
                            ParsedDay(
                                dayNumber = dObj.optInt("day", i + 1),
                                theme = dObj.optString("theme", "Day ${i + 1} in $destination"),
                                weather = dObj.optString("weather_summary", "Pleasant & Clear (26°C)"),
                                items = timelineList
                            )
                        )
                    }
                    return list
                }
            } catch (_: Exception) {}
        }

        // Realistic live fallback itinerary for Goa/Agra
        return listOf(
            ParsedDay(
                dayNumber = 1,
                theme = "Coastal Forts & Sunset Vibes",
                weather = "Sunny & Warm (28°C)",
                items = listOf(
                    TimelineItem(
                        sequence = 1,
                        startTime = "09:00 AM",
                        endTime = "10:00 AM",
                        type = "hotel",
                        title = "Breakfast & Hotel Check-in",
                        location = "Taj Fort Aguada, Goa",
                        description = "Morning buffet with coastal delicacies and tea.",
                        status = "completed"
                    ),
                    TimelineItem(
                        sequence = 2,
                        startTime = "10:30 AM",
                        endTime = "01:00 PM",
                        type = "activity",
                        title = "Fort Aguada Exploration",
                        location = "Fort Aguada & Lighthouse",
                        description = "Explore 17th-century Portuguese fortress and Arabian sea views.",
                        status = "completed"
                    ),
                    TimelineItem(
                        sequence = 3,
                        startTime = "01:30 PM",
                        endTime = "02:30 PM",
                        type = "food",
                        title = "Coastal Lunch & Beverages",
                        location = "The Fisherman's Wharf, Goa",
                        description = "Authentic Goan curry and local grilled seafood.",
                        status = "completed"
                    ),
                    TimelineItem(
                        sequence = 4,
                        startTime = "03:30 PM",
                        endTime = "06:00 PM",
                        type = "activity",
                        title = "Water Sports & Parasailing",
                        location = "Baga Beach",
                        description = "Certified beach watersports, speed boat ride & parasailing.",
                        status = "current"
                    ),
                    TimelineItem(
                        sequence = 5,
                        startTime = "06:30 PM",
                        endTime = "08:00 PM",
                        type = "attraction",
                        title = "Sunset at Anjuna Cliffs",
                        location = "Anjuna Beach Vantage Point",
                        description = "Spectacular coastal sunset and chilled ambient music.",
                        status = "upcoming"
                    ),
                    TimelineItem(
                        sequence = 6,
                        startTime = "08:30 PM",
                        endTime = "10:30 PM",
                        type = "food",
                        title = "Dinner by the Ocean",
                        location = "Thalassa Mediterranean, Goa",
                        description = "Cliffside sunset dining with Greek cuisine.",
                        status = "upcoming"
                    )
                )
            )
        )
    }
}
