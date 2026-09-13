package com.travellikepro.travelvendor.ui.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.travelvendor.data.model.RegisterRequest
import com.travellikepro.travelvendor.data.repository.AuthRepository
import com.travellikepro.travelvendor.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class SignUpUiState(
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
    val error: String? = null
)

class SignUpViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    fun register(name: String, email: String, mobile: String, password: String) {
        if (name.isBlank() || email.isBlank() || mobile.isBlank() || password.isBlank()) {
            _uiState.value = SignUpUiState(error = "All fields are required")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = SignUpUiState(isLoading = true)

                val response = repository.register(RegisterRequest(name, email, mobile, password))

                if (response.success && response.data?.token != null) {
                    // Backend automatically logs them in on successful signup.
                    val token = response.data.token
                    tokenManager.saveToken(token)

                    _uiState.value = SignUpUiState(
                        isRegistered = true
                    )
                } else {
                    _uiState.value = SignUpUiState(
                        error = response.message ?: "Registration failed."
                    )
                }
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is SocketTimeoutException, is ConnectException, is UnknownHostException ->
                        "Cannot reach server. Please check your connection."
                    is HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        var serverMessage = "Registration failed (${e.code()}). Please check your input."
                        try {
                            if (errorBody != null) {
                                val json = org.json.JSONObject(errorBody)
                                if (json.has("message")) {
                                    serverMessage = json.getString("message")
                                }
                            }
                        } catch (ex: Exception) {
                            // fallback to default
                        }
                        serverMessage
                    }
                    else -> e.localizedMessage ?: "Network error. Please check backend server connection."
                }

                _uiState.value = SignUpUiState(
                    error = errorMsg
                )
            }
        }
    }
}
