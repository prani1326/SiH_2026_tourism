package com.touristapp.presentation.destination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.models.DestinationDto
import com.touristapp.data.remote.ApiResult
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DestinationDetailViewModel : ViewModel() {

    private val destinationRepository = ServiceLocator.destinationRepository

    private val _destination = MutableStateFlow<ApiResult<DestinationDto>>(ApiResult.Loading)
    val destination: StateFlow<ApiResult<DestinationDto>> = _destination

    fun loadDestination(destinationId: String) {
        viewModelScope.launch {
            _destination.value = ApiResult.Loading
            try {
                val response = destinationRepository.getDestinationById(destinationId)
                if (response != null) {
                    _destination.value = ApiResult.Success(response)
                } else {
                    _destination.value = ApiResult.Error(404, "Destination not found")
                }
            } catch (e: Exception) {
                _destination.value = ApiResult.Exception(e)
            }
        }
    }
}
