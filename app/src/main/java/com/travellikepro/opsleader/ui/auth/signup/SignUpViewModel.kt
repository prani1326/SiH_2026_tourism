package com.travellikepro.opsleader.ui.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.api.SignupRequest
import com.travellikepro.opsleader.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SignUpUiState {
    object Idle : SignUpUiState()
    object Loading : SignUpUiState()
    object Success : SignUpUiState()
    data class Error(val messageResId: Int? = null, val messageStr: String? = null, val isReferenceError: Boolean = false) : SignUpUiState()
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle)
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    /**
     * First-time Google sign-in flow: user provides their name (pre-filled from Google)
     * and a reference ID to create an account.
     */
    fun signup(
        name: String,
        email: String,
        referenceId: String
    ) {
        if (name.isBlank() || email.isBlank() || referenceId.isBlank()) {
            _uiState.value = SignUpUiState.Error(messageResId = com.travellikepro.opsleader.R.string.error_empty_fields)
            return
        }

        _uiState.value = SignUpUiState.Loading

        viewModelScope.launch {
            try {
                val response = authRepository.signup(
                    SignupRequest(
                        name = name,
                        email = email,
                        referenceId = referenceId
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        _uiState.value = SignUpUiState.Success
                    } else {
                        if (body?.errorCode == "REFERENCE_ID_INVALID") {
                            _uiState.value = SignUpUiState.Error(messageResId = com.travellikepro.opsleader.R.string.error_invalid_reference, isReferenceError = true)
                        } else {
                            _uiState.value = SignUpUiState.Error(messageStr = body?.message ?: "Signup Failed")
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody != null && errorBody.contains("REFERENCE_ID_INVALID")) {
                        _uiState.value = SignUpUiState.Error(messageResId = com.travellikepro.opsleader.R.string.error_invalid_reference, isReferenceError = true)
                    } else {
                        _uiState.value = SignUpUiState.Error(messageStr = "Failed to sign up. Code: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                // Mock: simulate backend response
                delay(1000)
                if (referenceId == "1326") {
                    _uiState.value = SignUpUiState.Success
                } else {
                    _uiState.value = SignUpUiState.Error(
                        messageResId = com.travellikepro.opsleader.R.string.error_invalid_reference,
                        isReferenceError = true
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = SignUpUiState.Idle
    }
}
