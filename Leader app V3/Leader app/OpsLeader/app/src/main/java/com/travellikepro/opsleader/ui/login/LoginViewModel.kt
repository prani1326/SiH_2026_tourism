package com.travellikepro.opsleader.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    object NewUserNeedsReferenceId : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun isUserLoggedIn(): Boolean = repository.isLoggedIn()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your email and password")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = repository.login(email.trim(), password)
            result.fold(
                onSuccess = {
                    _uiState.value = LoginUiState.Success
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(error.localizedMessage ?: "Invalid email or password")
                }
            )
        }
    }

    fun handleGoogleSignIn(
        idToken: String,
        referenceId: String? = null
    ) {
        if (idToken.isBlank()) {
            _uiState.value = LoginUiState.Error("Google sign-in was cancelled or returned an invalid token.")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = repository.googleAuth(idToken, referenceId)
            result.fold(
                onSuccess = { body ->
                    if (body.success) {
                        _uiState.value = LoginUiState.Success
                    } else if (body.errorCode == "NEW_USER") {
                        _uiState.value = LoginUiState.NewUserNeedsReferenceId
                    } else {
                        _uiState.value = LoginUiState.Error(body.message ?: "Google Authentication Failed")
                    }
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(error.localizedMessage ?: "Network error during Google sign-in")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}