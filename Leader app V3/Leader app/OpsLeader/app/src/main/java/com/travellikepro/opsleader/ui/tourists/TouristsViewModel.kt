package com.travellikepro.opsleader.ui.tourists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.api.TouristDto
import com.travellikepro.opsleader.data.repository.TouristRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TouristsUiState {
    object Loading : TouristsUiState()
    data class Success(val tourists: List<TouristDto>) : TouristsUiState()
    object Empty : TouristsUiState()
    data class Error(val message: String) : TouristsUiState()
}

@HiltViewModel
class TouristsViewModel @Inject constructor(
    private val touristRepository: TouristRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TouristsUiState>(TouristsUiState.Loading)
    val uiState: StateFlow<TouristsUiState> = _uiState.asStateFlow()

    private val _selectedTourist = MutableStateFlow<TouristDto?>(null)
    val selectedTourist: StateFlow<TouristDto?> = _selectedTourist.asStateFlow()

    init {
        loadTourists()
    }

    fun loadTourists() {
        _uiState.value = TouristsUiState.Loading
        viewModelScope.launch {
            val result = touristRepository.getTourists()
            result.fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _uiState.value = TouristsUiState.Empty
                    } else {
                        _uiState.value = TouristsUiState.Success(list)
                    }
                },
                onFailure = { error ->
                    _uiState.value = TouristsUiState.Error(error.localizedMessage ?: "Failed to fetch tourists")
                }
            )
        }
    }

    fun loadTouristDetail(touristId: String) {
        viewModelScope.launch {
            val result = touristRepository.getTouristById(touristId)
            result.fold(
                onSuccess = { tourist -> _selectedTourist.value = tourist },
                onFailure = { _selectedTourist.value = null }
            )
        }
    }
}
