package com.travellikepro.opsleader.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.api.BookingDto
import com.travellikepro.opsleader.data.repository.OperationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BookingsUiState {
    object Loading : BookingsUiState()
    data class Success(val bookings: List<BookingDto>) : BookingsUiState()
    object Empty : BookingsUiState()
    data class Error(val message: String) : BookingsUiState()
}

@HiltViewModel
class BookingsViewModel @Inject constructor(
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookingsUiState>(BookingsUiState.Loading)
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    init {
        loadBookings()
    }

    fun loadBookings() {
        _uiState.value = BookingsUiState.Loading
        viewModelScope.launch {
            val result = operationsRepository.getBookings()
            result.fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _uiState.value = BookingsUiState.Empty
                    } else {
                        _uiState.value = BookingsUiState.Success(list)
                    }
                },
                onFailure = { error ->
                    _uiState.value = BookingsUiState.Error(error.localizedMessage ?: "Failed to load bookings")
                }
            )
        }
    }
}
