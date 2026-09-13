package com.travellikepro.opsleader.ui.triprequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.api.TripDto
import com.travellikepro.opsleader.data.repository.OperationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TripRequestsUiState {
    object Loading : TripRequestsUiState()
    data class Success(val trips: List<TripDto>) : TripRequestsUiState()
    object Empty : TripRequestsUiState()
    data class Error(val message: String) : TripRequestsUiState()
}

@HiltViewModel
class TripRequestsViewModel @Inject constructor(
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TripRequestsUiState>(TripRequestsUiState.Loading)
    val uiState: StateFlow<TripRequestsUiState> = _uiState.asStateFlow()

    private var allTrips: List<TripDto> = emptyList()
    private var currentFilter: String = "ALL"
    private var currentSearch: String = ""

    init {
        loadTrips()
    }

    fun loadTrips() {
        _uiState.value = TripRequestsUiState.Loading
        viewModelScope.launch {
            val result = operationsRepository.getTrips()
            result.fold(
                onSuccess = { trips ->
                    allTrips = trips
                    applyFilters()
                },
                onFailure = { error ->
                    _uiState.value = TripRequestsUiState.Error(
                        error.localizedMessage ?: "Failed to fetch trip requests"
                    )
                }
            )
        }
    }

    fun setStatusFilter(status: String) {
        currentFilter = status
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        currentSearch = query
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = allTrips.filter { trip ->
            val statusStr = trip.status ?: "pending"
            val matchesFilter = when (currentFilter.uppercase()) {
                "ALL" -> true
                "NEW", "PENDING" -> statusStr.equals("pending", ignoreCase = true) || statusStr.equals("new", ignoreCase = true)
                "ACCEPTED", "ASSIGNED" -> statusStr.equals("accepted", ignoreCase = true) || statusStr.equals("assigned", ignoreCase = true)
                "IN_PROGRESS" -> statusStr.equals("in_progress", ignoreCase = true) || statusStr.equals("active", ignoreCase = true)
                "COMPLETED" -> statusStr.equals("completed", ignoreCase = true)
                "CANCELLED", "REJECTED" -> statusStr.equals("cancelled", ignoreCase = true) || statusStr.equals("rejected", ignoreCase = true)
                else -> statusStr.equals(currentFilter, ignoreCase = true)
            }

            val matchesSearch = currentSearch.isBlank() ||
                (trip.destination?.contains(currentSearch, ignoreCase = true) == true) ||
                (trip.destination_name?.contains(currentSearch, ignoreCase = true) == true) ||
                (trip.title?.contains(currentSearch, ignoreCase = true) == true) ||
                (trip.tourist_name?.contains(currentSearch, ignoreCase = true) == true) ||
                (trip.requester_name?.contains(currentSearch, ignoreCase = true) == true) ||
                trip.id.contains(currentSearch, ignoreCase = true)

            matchesFilter && matchesSearch
        }

        if (filtered.isEmpty()) {
            _uiState.value = TripRequestsUiState.Empty
        } else {
            _uiState.value = TripRequestsUiState.Success(filtered)
        }
    }
}
