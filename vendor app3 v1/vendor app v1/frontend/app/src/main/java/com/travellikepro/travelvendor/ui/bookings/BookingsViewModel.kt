package com.travellikepro.travelvendor.ui.bookings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.travelvendor.data.model.Booking
import com.travellikepro.travelvendor.data.model.BookingCounts
import com.travellikepro.travelvendor.data.repository.BookingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BookingsUiState {
    object Loading : BookingsUiState()
    data class Success(val bookings: List<Booking>, val counts: BookingCounts) : BookingsUiState()
    object Empty : BookingsUiState()
    data class Error(val message: String) : BookingsUiState()
}

class BookingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookingsRepository(application)

    private val _uiState = MutableStateFlow<BookingsUiState>(BookingsUiState.Loading)
    val uiState: StateFlow<BookingsUiState> = _uiState

    private val _detailState = MutableStateFlow<Booking?>(null)
    val detailState: StateFlow<Booking?> = _detailState

    private val _isOperating = MutableStateFlow(false)
    val isOperating: StateFlow<Boolean> = _isOperating

    var currentStatusFilter: String? = null

    init {
        loadBookings()
    }

    fun loadBookings(status: String? = currentStatusFilter) {
        currentStatusFilter = status
        viewModelScope.launch {
            _uiState.value = BookingsUiState.Loading
            try {
                val response = repository.getBookings(status = status)
                if (response.success && response.data != null) {
                    val list = response.data.bookings
                    if (list.isEmpty()) {
                        _uiState.value = BookingsUiState.Empty
                    } else {
                        _uiState.value = BookingsUiState.Success(list, response.data.counts)
                    }
                } else {
                    _uiState.value = BookingsUiState.Error(response.message ?: "Failed to fetch bookings")
                }
            } catch (e: Exception) {
                _uiState.value = BookingsUiState.Error(e.localizedMessage ?: "Network error loading bookings")
            }
        }
    }

    fun loadBookingDetail(id: Int) {
        viewModelScope.launch {
            try {
                val response = repository.getBookingById(id)
                if (response.success) {
                    _detailState.value = response.data
                }
            } catch (_: Exception) {}
        }
    }

    fun acceptBooking(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isOperating.value = true
            try {
                val response = repository.acceptBooking(id)
                if (response.success) {
                    loadBookings()
                    onSuccess()
                }
            } catch (_: Exception) {
            } finally {
                _isOperating.value = false
            }
        }
    }

    fun rejectBooking(id: Int, reason: String? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isOperating.value = true
            try {
                val response = repository.rejectBooking(id, reason)
                if (response.success) {
                    loadBookings()
                    onSuccess()
                }
            } catch (_: Exception) {
            } finally {
                _isOperating.value = false
            }
        }
    }
}
