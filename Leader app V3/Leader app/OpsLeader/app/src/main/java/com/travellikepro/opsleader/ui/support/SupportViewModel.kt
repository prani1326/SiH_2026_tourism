package com.travellikepro.opsleader.ui.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.api.BroadcastNotificationRequest
import com.travellikepro.opsleader.data.api.SupportTicketDto
import com.travellikepro.opsleader.data.repository.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupportUiData(
    val tickets: List<SupportTicketDto> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val supportRepository: SupportRepository
) : ViewModel() {

    private val _uiData = MutableStateFlow(SupportUiData())
    val uiData: StateFlow<SupportUiData> = _uiData.asStateFlow()

    init {
        loadTickets()
    }

    fun loadTickets() {
        _uiData.value = _uiData.value.copy(isLoading = true)
        viewModelScope.launch {
            val result = supportRepository.getSupportTickets()
            result.fold(
                onSuccess = { tickets ->
                    _uiData.value = _uiData.value.copy(tickets = tickets, isLoading = false)
                },
                onFailure = { error ->
                    _uiData.value = _uiData.value.copy(isLoading = false, message = error.localizedMessage)
                }
            )
        }
    }

    fun sendReply(ticketId: String, text: String) {
        viewModelScope.launch {
            val result = supportRepository.sendTicketMessage(ticketId, text)
            result.fold(
                onSuccess = { loadTickets() },
                onFailure = { error ->
                    _uiData.value = _uiData.value.copy(message = error.localizedMessage)
                }
            )
        }
    }

    fun broadcastAlert(title: String, body: String) {
        viewModelScope.launch {
            supportRepository.broadcastNotification(
                BroadcastNotificationRequest(title = title, body = body)
            )
        }
    }
}
