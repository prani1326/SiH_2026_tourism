package com.travellikepro.opsleader.ui.main.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.api.DashboardKpiDto
import com.travellikepro.opsleader.data.api.SosAlertDto
import com.travellikepro.opsleader.data.api.TripDto
import com.travellikepro.opsleader.data.repository.AuthRepository
import com.travellikepro.opsleader.data.repository.OperationsRepository
import com.travellikepro.opsleader.data.repository.SafetyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiData(
    val kpi: DashboardKpiDto = DashboardKpiDto(),
    val recentTrips: List<TripDto> = emptyList(),
    val activeSosAlert: SosAlertDto? = null,
    val userName: String = "Ops Leader",
    val userRegion: String = "Rajasthan",
    val isRefreshing: Boolean = false
)

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val data: DashboardUiData) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val operationsRepository: OperationsRepository,
    private val safetyRepository: SafetyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData(isRefresh: Boolean = false) {
        if (!isRefresh && _uiState.value !is DashboardUiState.Success) {
            _uiState.value = DashboardUiState.Loading
        }

        viewModelScope.launch {
            try {
                val user = authRepository.getSessionUser()
                val kpisResult = operationsRepository.getDashboardKpis()
                val tripsResult = operationsRepository.getTrips()
                val sosResult = safetyRepository.getSosAlerts("active")

                val kpiData = kpisResult.getOrDefault(DashboardKpiDto())
                val trips = tripsResult.getOrDefault(emptyList()).take(5)
                val activeSos = sosResult.getOrDefault(emptyList()).firstOrNull()

                _uiState.value = DashboardUiState.Success(
                    DashboardUiData(
                        kpi = kpiData,
                        recentTrips = trips,
                        activeSosAlert = activeSos,
                        userName = user.displayName,
                        userRegion = user.region?.ifBlank { "North India - Rajasthan & Delhi" } ?: "North India - Rajasthan & Delhi",
                        isRefreshing = false
                    )
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(
                    e.localizedMessage ?: "Failed to load operational dashboard data."
                )
            }
        }
    }
}
