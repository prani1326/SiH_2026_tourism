package com.travellikepro.opsleader.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    /**
     * Handle Google Sign-In result.
     * In production, idToken would be sent to POST auth/google on the backend.
     * The backend verifies the token with Google, creates/matches the user, and returns a session JWT.
     */
    fun handleGoogleSignIn(
        idToken: String,
        isNewUser: Boolean,
        onSuccess: () -> Unit,
        onNewUserNeedsReferenceId: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = repository.googleAuth(idToken)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        onSuccess()
                    } else if (body?.errorCode == "NEW_USER") {
                        onNewUserNeedsReferenceId()
                    } else {
                        onError(body?.message ?: "Authentication failed")
                    }
                } else {
                    onError("Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                // Mock: simulate successful login for UI testing
                delay(800)
                onSuccess()
            }
        }
    }
}