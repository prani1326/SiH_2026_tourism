package com.touristapp.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileEditViewModel : ViewModel() {
    private val userRepository = ServiceLocator.userRepository
    val currentUser = ServiceLocator.sessionManager.currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun saveProfile(fullName: String, language: String, currency: String) {
        val uid = userRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            userRepository.updateProfile(
                uid = uid,
                fullName = fullName,
                language = language,
                currency = currency
            ).onSuccess {
                _saveSuccess.value = true
            }.onFailure { e ->
                _errorMessage.value = "Failed to update profile: ${e.localizedMessage}"
            }
            _isLoading.value = false
        }
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        val uid = userRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            userRepository.uploadAvatar(context, uid, uri).onSuccess {
                _saveSuccess.value = true
            }.onFailure { e ->
                _errorMessage.value = "Failed to upload avatar: ${e.localizedMessage}"
            }
            _isLoading.value = false
        }
    }
}
