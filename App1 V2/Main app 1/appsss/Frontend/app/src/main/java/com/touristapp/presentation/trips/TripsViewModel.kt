package com.touristapp.presentation.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.models.TripModel
import com.touristapp.data.remote.ApiResult
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray

class TripsViewModel : ViewModel() {

    private val tripRepository = ServiceLocator.tripRepository

    private val _aiPlan = MutableStateFlow<ApiResult<JsonObject>>(ApiResult.Success(JsonObject(emptyMap())))
    val aiPlan: StateFlow<ApiResult<JsonObject>> = _aiPlan

    private val _aiPlanPreview = MutableStateFlow<com.touristapp.data.remote.model.GeneratedItineraryPlanDto?>(null)
    val aiPlanPreview: StateFlow<com.touristapp.data.remote.model.GeneratedItineraryPlanDto?> = _aiPlanPreview

    private val _generationStage = MutableStateFlow<String>("")
    val generationStage: StateFlow<String> = _generationStage

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _trips = MutableStateFlow<ApiResult<JsonArray>>(ApiResult.Loading)
    val trips: StateFlow<ApiResult<JsonArray>> = _trips

    init {
        loadTrips()
    }

    fun loadTrips() {
        viewModelScope.launch {
            _trips.value = ApiResult.Loading
            try {
                val tripList = tripRepository.getUserTrips()
                val jsonArray = buildJsonArray {
                    tripList.forEach { trip ->
                        add(trip.toJsonObject())
                    }
                }
                _trips.value = ApiResult.Success(jsonArray)
            } catch (e: Exception) {
                _trips.value = ApiResult.Exception(e)
            }
        }
    }

    fun planWithAi(
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
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            _aiPlanPreview.value = null

            val stages = listOf(
                "Analyzing your travel preferences & destination...",
                "Finding suitable places in $destination...",
                "Planning your daily schedule & routes...",
                "Calculating travel times ($transportPreference)...",
                "Organizing meals ($foodPreference) & hotel...",
                "Checking transport options...",
                "Finalizing your structured itinerary..."
            )

            try {
                val previewJob = async {
                    tripRepository.previewAiPlan(
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
                        walkingTolerance = walkingTolerance,
                        startDate = startDate,
                        endDate = endDate
                    )
                }

                for (stage in stages) {
                    _generationStage.value = stage
                    delay(300)
                }

                val result = previewJob.await()
                _isGenerating.value = false

                result.onSuccess { planDto ->
                    _aiPlanPreview.value = planDto
                }.onFailure { e ->
                    _aiPlan.value = ApiResult.Exception(e)
                }
            } catch (e: Exception) {
                _isGenerating.value = false
                _aiPlan.value = ApiResult.Exception(e)
            }
        }
    }

    fun finalizeTripFromPreview(
        style: String = "Couple",
        interests: List<String> = emptyList(),
        budget: Double = 0.0,
        startDate: String? = null,
        endDate: String? = null,
        onSuccess: (String) -> Unit
    ) {
        val preview = _aiPlanPreview.value ?: return
        viewModelScope.launch {
            _isGenerating.value = true
            _generationStage.value = "Saving your personalized trip to Firestore..."
            try {
                val result = tripRepository.finalizeAndSaveTrip(
                    previewPlan = preview,
                    style = style,
                    interests = interests,
                    budget = budget,
                    startDate = startDate,
                    endDate = endDate
                )
                _isGenerating.value = false
                result.onSuccess { savedTrip ->
                    _aiPlan.value = ApiResult.Success(savedTrip.toJsonObject())
                    loadTrips()
                    onSuccess(savedTrip.id)
                }.onFailure { e ->
                    _aiPlan.value = ApiResult.Exception(e)
                }
            } catch (e: Exception) {
                _isGenerating.value = false
                _aiPlan.value = ApiResult.Exception(e)
            }
        }
    }

    fun clearPreview() {
        _aiPlanPreview.value = null
    }

    fun generatePlan(
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
    ) {
        planWithAi(
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
    }
}
