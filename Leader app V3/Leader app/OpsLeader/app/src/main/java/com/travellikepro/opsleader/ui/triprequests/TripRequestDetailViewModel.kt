package com.travellikepro.opsleader.ui.triprequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.api.AssignTripRequest
import com.travellikepro.opsleader.data.api.TripDto
import com.travellikepro.opsleader.data.api.VendorDto
import com.travellikepro.opsleader.data.repository.OperationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TripDetailUiState {
    object Loading : TripDetailUiState()
    data class Success(val trip: TripDto, val availableVendors: List<VendorDto> = emptyList()) : TripDetailUiState()
    data class Error(val message: String) : TripDetailUiState()
}

@HiltViewModel
class TripRequestDetailViewModel @Inject constructor(
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TripDetailUiState>(TripDetailUiState.Loading)
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    private var currentTripId: String = ""

    fun loadTripDetail(tripId: String) {
        currentTripId = tripId
        _uiState.value = TripDetailUiState.Loading

        viewModelScope.launch {
            val tripResult = operationsRepository.getTripById(tripId)
            val vendorsResult = operationsRepository.getPartners()

            tripResult.fold(
                onSuccess = { trip ->
                    val vendors = vendorsResult.getOrDefault(emptyList())
                    _uiState.value = TripDetailUiState.Success(trip = trip, availableVendors = vendors)
                },
                onFailure = { error ->
                    _uiState.value = TripDetailUiState.Error(
                        error.localizedMessage ?: "Failed to fetch trip details."
                    )
                }
            )
        }
    }

    fun assignVendor(vendorId: String, vendorName: String, notes: String = "") {
        if (currentTripId.isBlank()) return

        viewModelScope.launch {
            val request = AssignTripRequest(
                vendor_id = vendorId,
                vendor_name = vendorName,
                notes = notes
            )
            val result = operationsRepository.assignTrip(currentTripId, request)
            result.fold(
                onSuccess = { updatedTrip ->
                    val currentVendors = (_uiState.value as? TripDetailUiState.Success)?.availableVendors ?: emptyList()
                    _uiState.value = TripDetailUiState.Success(trip = updatedTrip, availableVendors = currentVendors)
                },
                onFailure = { error ->
                    _uiState.value = TripDetailUiState.Error(error.localizedMessage ?: "Failed to assign vendor.")
                }
            )
        }
    }

    fun acceptTrip() {
        assignVendor(vendorId = "auto-assigned", vendorName = "Ops Leader Primary Vendor", notes = "Request Accepted by Ops Leader")
    }

    fun rejectTrip(reason: String) {
        if (currentTripId.isBlank()) return
        viewModelScope.launch {
            operationsRepository.addTripNote(currentTripId, "Trip Rejected. Reason: $reason")
            loadTripDetail(currentTripId)
        }
    }
}
