package com.travellikepro.travelvendor.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.travelvendor.data.repository.AuthRepository
import com.travellikepro.travelvendor.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val kycStatus: String? = null,
    val error: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState(error = "Please enter email/mobile and password")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = LoginUiState(isLoading = true)

                val response = repository.login(identifier, password)

                if (response.success && response.data?.token != null) {
                    val token = response.data.token
                    val vendor = response.data.vendor
                    tokenManager.saveToken(token)

                    _uiState.value = LoginUiState(
                        isLoggedIn = true,
                        kycStatus = vendor?.kyc_status ?: "pending"
                    )
                } else {
                    _uiState.value = LoginUiState(
                        error = response.message ?: "Login failed. Please check credentials."
                    )
                }
            } catch (e: Exception) {
                val errorMsg = if (e is retrofit2.HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    var serverMessage = "Login failed (${e.code()}). Please check your input."
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
                } else {
                    e.localizedMessage ?: "Network error. Please check backend server connection."
                }
                
                _uiState.value = LoginUiState(
                    error = errorMsg
                )
            }
        }
    }
}