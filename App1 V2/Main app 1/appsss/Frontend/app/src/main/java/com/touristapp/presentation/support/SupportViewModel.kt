package com.touristapp.presentation.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.remote.ApiResult
import com.touristapp.data.remote.model.SupportTicketOutDto
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SupportViewModel : ViewModel() {

    private val supportRepository = ServiceLocator.supportRepository

    private val _tickets = MutableStateFlow<ApiResult<List<SupportTicketOutDto>>>(ApiResult.Loading)
    val tickets: StateFlow<ApiResult<List<SupportTicketOutDto>>> = _tickets

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    init {
        loadTickets()
    }

    fun loadTickets() {
        viewModelScope.launch {
            _tickets.value = ApiResult.Loading
            val res = supportRepository.getUserTickets()
            res.onSuccess { _tickets.value = ApiResult.Success(it) }
                .onFailure { _tickets.value = ApiResult.Exception(it) }
        }
    }

    fun createTicket(subject: String, category: String, message: String) {
        viewModelScope.launch {
            val res = supportRepository.createTicket(subject, category, message)
            res.onSuccess {
                _statusMessage.value = "Ticket #${it.id.take(8)} opened successfully."
                loadTickets()
            }.onFailure {
                _statusMessage.value = "Failed to create support ticket: ${it.message}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
