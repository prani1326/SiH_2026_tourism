package com.travellikepro.travelvendor.ui.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.travelvendor.data.repository.KycRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class KycCheckState {
    object Loading : KycCheckState()
    data class Success(val kycStatus: String) : KycCheckState()
    data class Error(val message: String) : KycCheckState()
}

class KycCheckViewModel(application: Application) : AndroidViewModel(application) {
    private val kycRepository = KycRepository(application)
    
    private val _uiState = MutableStateFlow<KycCheckState>(KycCheckState.Loading)
    val uiState: StateFlow<KycCheckState> = _uiState

    fun checkKycStatus() {
        viewModelScope.launch {
            _uiState.value = KycCheckState.Loading
            try {
                val response = kycRepository.getKycStatus()
                if (response.success && response.data != null) {
                    _uiState.value = KycCheckState.Success(response.data.kyc_status)
                } else {
                    _uiState.value = KycCheckState.Error(response.message ?: "Failed to fetch KYC status")
                }
            } catch (e: Exception) {
                _uiState.value = KycCheckState.Error(e.localizedMessage ?: "Network error")
            }
        }
    }
}
