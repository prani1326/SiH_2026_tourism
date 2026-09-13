package com.travellikepro.opsleader.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.api.UserResponse
import com.travellikepro.opsleader.data.local.datastore.SessionManager
import com.travellikepro.opsleader.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: UserResponse = UserResponse(id = "", name = "Ops Leader", email = "", role = "OPS_LEADER"),
    val isLoading: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val sessionUser = authRepository.getSessionUser()
        _uiState.value = _uiState.value.copy(user = sessionUser)

        viewModelScope.launch {
            val result = authRepository.getMe()
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(user = user)
                },
                onFailure = {
                    // Keep session user if remote fetch fails
                }
            )
        }
    }

    fun saveProfile(name: String, phone: String, region: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, saveSuccess = false)
        viewModelScope.launch {
            sessionManager.saveUser(
                id = _uiState.value.user.id,
                name = name,
                email = _uiState.value.user.email,
                role = _uiState.value.user.role,
                phone = phone,
                region = region
            )
            val updated = _uiState.value.user.copy(name = name, phone = phone, region = region)
            _uiState.value = _uiState.value.copy(user = updated, isLoading = false, saveSuccess = true)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
