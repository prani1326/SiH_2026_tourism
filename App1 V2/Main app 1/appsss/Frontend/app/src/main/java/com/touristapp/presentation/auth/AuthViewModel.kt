package com.touristapp.presentation.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.touristapp.data.firebase.PhoneAuthCallback
import com.touristapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val resetSent: Boolean = false,
    val error: String? = null,
    val otpSent: Boolean = false,
    val verificationId: String? = null,
    val phoneNumber: String? = null,
    val resendToken: PhoneAuthProvider.ForceResendingToken? = null
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Please enter email and password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repository.login(email.trim(), password).onSuccess {
                _uiState.value = AuthUiState(isSuccess = true)
            }.onFailure { exception ->
                _uiState.value = AuthUiState(error = formatAuthError(exception))
            }
        }
    }

    fun signup(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Please fill all fields.")
            return
        }

        if (password.length < 6) {
            _uiState.value = AuthUiState(error = "Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repository.signup(name.trim(), email.trim(), password).onSuccess {
                _uiState.value = AuthUiState(isSuccess = true)
            }.onFailure { exception ->
                _uiState.value = AuthUiState(error = formatAuthError(exception))
            }
        }
    }

    fun sendPhoneOtp(activity: Activity, phoneNumber: String) {
        val trimmedPhone = phoneNumber.trim()
        if (trimmedPhone.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid phone number.")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null, phoneNumber = trimmedPhone)

        repository.sendPhoneOtp(
            activity = activity,
            phoneNumber = trimmedPhone,
            callback = object : PhoneAuthCallback {
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpSent = true,
                        verificationId = verificationId,
                        resendToken = token,
                        error = null
                    )
                }

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    loginWithCredential(credential)
                }

                override fun onVerificationFailed(exception: FirebaseException) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = formatAuthError(exception)
                    )
                }
            },
            resendToken = _uiState.value.resendToken
        )
    }

    fun verifyPhoneOtp(code: String) {
        val verificationId = _uiState.value.verificationId
        if (verificationId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = "No verification request found. Please request a new OTP.")
            return
        }
        if (code.length < 6) {
            _uiState.value = _uiState.value.copy(error = "Please enter complete 6-digit code.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.verifyPhoneOtp(verificationId, code.trim()).onSuccess {
                _uiState.value = AuthUiState(isSuccess = true)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = formatAuthError(exception)
                )
            }
        }
    }

    fun loginWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.loginWithCredential(credential).onSuccess {
                _uiState.value = AuthUiState(isSuccess = true)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = formatAuthError(exception)
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun formatAuthError(exception: Throwable): String {
        val msg = exception.localizedMessage ?: ""
        return when {
            msg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "Firebase Setup Pending: Please enable 'Email/Password' or 'Phone' in Firebase Console."
            msg.contains("badly formatted", ignoreCase = true) || msg.contains("invalid email", ignoreCase = true) ->
                "Invalid email format. Please enter a valid email address (e.g. user@example.com)."
            msg.contains("invalid-verification-code", ignoreCase = true) || msg.contains("invalid verification code", ignoreCase = true) ->
                "Invalid OTP code. Please check and enter the correct 6-digit code."
            msg.contains("session-expired", ignoreCase = true) || msg.contains("expired", ignoreCase = true) ->
                "OTP has expired. Please tap 'Resend' to receive a new code."
            msg.contains("app-not-authorized", ignoreCase = true) || msg.contains("SHA-1", ignoreCase = true) ->
                "Phone Auth Setup Pending: Please register SHA-1 fingerprint in Firebase Console."
            msg.contains("too-many-requests", ignoreCase = true) || msg.contains("quota exceeded", ignoreCase = true) ->
                "Too many attempts. Please try again later."
            msg.contains("user-not-found", ignoreCase = true) ->
                "No account found with this email. Please sign up first."
            msg.contains("wrong-password", ignoreCase = true) || msg.contains("invalid-credential", ignoreCase = true) ->
                "Incorrect email or password. Please try again."
            msg.contains("email-already-in-use", ignoreCase = true) ->
                "This email is already registered. Please log in instead."
            msg.contains("network", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
                "Network connection issue. Please check your internet connection."
            else -> msg.ifBlank { "Operation failed. Please try again." }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState(error = "Please enter your email.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repository.sendPasswordReset(email.trim()).onSuccess {
                _uiState.value = AuthUiState(resetSent = true)
            }.onFailure { exception ->
                _uiState.value = AuthUiState(error = exception.localizedMessage ?: "Failed to send reset email.")
            }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repository.continueAsGuest().onSuccess {
                _uiState.value = AuthUiState(isSuccess = true)
            }.onFailure { exception ->
                _uiState.value = AuthUiState(error = exception.localizedMessage ?: "Failed to continue as guest")
            }
        }
    }
}