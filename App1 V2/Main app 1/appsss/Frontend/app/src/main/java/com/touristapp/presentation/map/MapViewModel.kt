package com.touristapp.presentation.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.location.LocationService
import com.touristapp.data.models.PlaceModel
import com.touristapp.data.repository.MapRepository
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val places: List<PlaceModel> = emptyList(),
    val filteredPlaces: List<PlaceModel> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val selectedPlace: PlaceModel? = null,
    val userLocation: Location? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MapViewModel(
    private val mapRepository: MapRepository = ServiceLocator.mapRepository,
    private val locationService: LocationService = ServiceLocator.locationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState(isLoading = true))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadPlaces()
        fetchLocation()
    }

    fun loadPlaces(destinationId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val list = mapRepository.getPlaces(destinationId = destinationId)
                _uiState.update { state ->
                    val filtered = applyFilter(list, state.selectedCategory, state.searchQuery)
                    state.copy(
                        places = list,
                        filteredPlaces = filtered,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load travel points: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setCategory(category: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.places, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredPlaces = filtered)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.places, state.selectedCategory, query)
            state.copy(searchQuery = query, filteredPlaces = filtered)
        }
    }

    fun selectPlace(place: PlaceModel?) {
        _uiState.update { it.copy(selectedPlace = place) }
    }

    fun fetchLocation() {
        viewModelScope.launch {
            val location = locationService.getCurrentLocation()
            if (location != null) {
                _uiState.update { it.copy(userLocation = location) }
            }
        }
    }

    private fun applyFilter(
        places: List<PlaceModel>,
        category: String,
        query: String
    ): List<PlaceModel> {
        var result = places

        if (category != "All" && category.isNotBlank()) {
            val normalized = when (category.lowercase()) {
                "hotels", "hotel" -> "hotel"
                "food", "restaurants" -> "food"
                "activities", "activity" -> "activity"
                "attractions", "attraction" -> "attraction"
                "transport", "transit" -> "transport"
                else -> category.lowercase()
            }
            result = result.filter { it.category.equals(normalized, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.destinationName.lowercase().contains(q) ||
                (it.address?.lowercase()?.contains(q) == true)
            }
        }

        return result
    }
}
