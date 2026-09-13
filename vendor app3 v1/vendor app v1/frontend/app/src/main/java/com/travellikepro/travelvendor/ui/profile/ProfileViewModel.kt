package com.travellikepro.travelvendor.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.travelvendor.data.model.Vendor
import com.travellikepro.travelvendor.data.repository.ProfileRepository
import com.travellikepro.travelvendor.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: Vendor) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val response = repository.getProfile()
                if (response.success && response.data != null) {
                    _uiState.value = ProfileUiState.Success(response.data)
                } else {
                    _uiState.value = ProfileUiState.Error(response.message ?: "Failed to load profile")
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.localizedMessage ?: "Network error loading profile")
            }
        }
    }

    fun updateProfile(name: String, email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                val response = repository.updateProfile(name, email)
                if (response.success) {
                    loadProfile()
                    onSuccess()
                }
            } catch (_: Exception) {
            } finally {
                _isUpdating.value = false
            }
        }
    }

    fun logout() {
        tokenManager.clearToken()
    }
}
