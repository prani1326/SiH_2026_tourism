package com.touristapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.models.DestinationDto
import com.touristapp.data.remote.ApiResult
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

data class WeatherInfo(
    val city: String = "Delhi / Agra",
    val temperature: String = "28°C",
    val condition: String = "Sunny & Pleasant",
    val rainProbability: String = "10%"
)

class HomeViewModel : ViewModel() {

    private val destinationRepository = ServiceLocator.destinationRepository
    private val tripRepository = ServiceLocator.tripRepository

    private val _destinations = MutableStateFlow<ApiResult<List<DestinationDto>>>(ApiResult.Loading)
    val destinations: StateFlow<ApiResult<List<DestinationDto>>> = _destinations

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _weather = MutableStateFlow(WeatherInfo())
    val weather: StateFlow<WeatherInfo> = _weather

    val currentUser = ServiceLocator.sessionManager.currentUser

    private val _activeTrip = MutableStateFlow<JsonObject?>(null)
    val activeTrip: StateFlow<JsonObject?> = _activeTrip

    val isGuest = ServiceLocator.sessionRepository.isGuestFlow

    init {
        loadDestinations()
        fetchUserIfNeeded()
        loadActiveTrip()
    }

    private fun fetchUserIfNeeded() {
        viewModelScope.launch {
            if (currentUser.value == null) {
                ServiceLocator.sessionManager.fetchCurrentUser()
            }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        loadDestinations(category)
    }

    fun loadDestinations(category: String = _selectedCategory.value) {
        viewModelScope.launch {
            val cached = if (category == "All" || category.isBlank()) {
                destinationRepository.getCachedPopularDestinations()
            } else {
                destinationRepository.getCachedDestinations(tag = category)
            }
            if (cached.isNotEmpty()) {
                _destinations.value = ApiResult.Success(cached)
            } else {
                _destinations.value = ApiResult.Loading
            }

            try {
                val list = if (category == "All" || category.isBlank()) {
                    destinationRepository.getPopularDestinations()
                } else {
                    destinationRepository.getDestinations(tag = category)
                }
                if (list.isNotEmpty()) {
                    _destinations.value = ApiResult.Success(list)
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    _destinations.value = ApiResult.Exception(e)
                }
            }
        }
    }

    fun loadActiveTrip() {
        viewModelScope.launch {
            try {
                val trips = tripRepository.getUserTrips()
                val active = trips.firstOrNull { it.status == "active" || it.status == "planning" }
                _activeTrip.value = active?.toJsonObject()
            } catch (e: Exception) {
                // Ignore if not logged in or network transient
            }
        }
    }
}
