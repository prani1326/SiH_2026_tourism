package com.travellikepro.opsleader.ui.safety

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.api.CreateIncidentRequest
import com.travellikepro.opsleader.data.api.DispatchResponderRequest
import com.travellikepro.opsleader.data.api.IncidentDto
import com.travellikepro.opsleader.data.api.SosAlertDto
import com.travellikepro.opsleader.data.repository.SafetyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SafetyUiData(
    val sosAlerts: List<SosAlertDto> = emptyList(),
    val incidents: List<IncidentDto> = emptyList(),
    val isBusy: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SafetyViewModel @Inject constructor(
    private val safetyRepository: SafetyRepository
) : ViewModel() {

    private val _uiData = MutableStateFlow(SafetyUiData())
    val uiData: StateFlow<SafetyUiData> = _uiData.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val sos = safetyRepository.getSosAlerts().getOrDefault(emptyList())
            val inc = safetyRepository.getIncidents().getOrDefault(emptyList())
            _uiData.value = _uiData.value.copy(sosAlerts = sos, incidents = inc)
        }
    }

    fun dispatchResponder(alertId: String, responderName: String, phone: String, etaMinutes: Int) {
        viewModelScope.launch {
            val request = DispatchResponderRequest(
                responder_name = responderName,
                responder_phone = phone,
                responder = responderName,
                eta_minutes = etaMinutes
            )
            val result = safetyRepository.dispatchResponder(alertId, request)
            result.fold(
                onSuccess = {
                    loadData()
                },
                onFailure = { error ->
                    _uiData.value = _uiData.value.copy(message = error.localizedMessage)
                }
            )
        }
    }

    fun resolveSos(alertId: String) {
        viewModelScope.launch {
            val result = safetyRepository.resolveSosAlert(alertId)
            result.fold(
                onSuccess = { loadData() },
                onFailure = { error ->
                    _uiData.value = _uiData.value.copy(message = error.localizedMessage)
                }
            )
        }
    }

    fun reportIncident(touristName: String, title: String, description: String, location: String) {
        viewModelScope.launch {
            val request = CreateIncidentRequest(
                tourist_name = touristName,
                title = title,
                description = description,
                category = "SAFETY",
                severity = "MAJOR",
                location = location
            )
            val result = safetyRepository.createIncident(request)
            result.fold(
                onSuccess = { loadData() },
                onFailure = { error ->
                    _uiData.value = _uiData.value.copy(message = error.localizedMessage)
                }
            )
        }
    }

    fun updateIncidentStatus(incidentId: String, status: String, notes: String? = null) {
        viewModelScope.launch {
            val result = safetyRepository.updateIncidentStatus(incidentId, status, notes)
            result.fold(
                onSuccess = { loadData() },
                onFailure = { error ->
                    _uiData.value = _uiData.value.copy(message = error.localizedMessage)
                }
            )
        }
    }

    fun resolveIncident(incidentId: String, resolutionSummary: String) {
        viewModelScope.launch {
            val result = safetyRepository.resolveIncident(incidentId, resolutionSummary)
            result.fold(
                onSuccess = { loadData() },
                onFailure = { error ->
                    _uiData.value = _uiData.value.copy(message = error.localizedMessage)
                }
            )
        }
    }
}
