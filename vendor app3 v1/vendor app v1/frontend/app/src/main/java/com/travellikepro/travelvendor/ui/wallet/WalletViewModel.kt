package com.travellikepro.travelvendor.ui.wallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.travelvendor.data.model.WalletSummary
import com.travellikepro.travelvendor.data.repository.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WalletUiState {
    object Loading : WalletUiState()
    data class Success(val summary: WalletSummary) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WalletRepository(application)

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState

    init {
        loadWallet()
    }

    fun loadWallet() {
        viewModelScope.launch {
            _uiState.value = WalletUiState.Loading
            try {
                val response = repository.getWallet()
                if (response.success && response.data != null) {
                    _uiState.value = WalletUiState.Success(response.data)
                } else {
                    _uiState.value = WalletUiState.Error(response.message ?: "Failed to load wallet")
                }
            } catch (e: Exception) {
                _uiState.value = WalletUiState.Error(e.localizedMessage ?: "Network error loading wallet")
            }
        }
    }
}
