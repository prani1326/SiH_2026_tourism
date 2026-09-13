package com.touristapp.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.models.TripEligibilityResponse
import com.touristapp.data.models.UserDto
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isEditMode: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val eligibility: TripEligibilityResponse? = null
)

class ProfileViewModel : ViewModel() {
    private val userRepository = ServiceLocator.userRepository
    private val authRepository = ServiceLocator.authRepository
    private val apiClient = ServiceLocator.backendApiClient
    private val sessionRepo = ServiceLocator.sessionRepository

    val currentUser: StateFlow<UserDto?> = ServiceLocator.sessionManager.currentUser
    val themeMode: StateFlow<com.touristapp.ui.theme.AppThemeMode> = ServiceLocator.sessionManager.themeMode

    fun setThemeMode(mode: com.touristapp.ui.theme.AppThemeMode) {
        viewModelScope.launch {
            ServiceLocator.sessionManager.setThemeMode(mode)
        }
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Editable field states
    val editFullName = MutableStateFlow("")
    val editBio = MutableStateFlow("")
    val editDob = MutableStateFlow("1998-05-15")
    val editGender = MutableStateFlow("Male")
    val editNationality = MutableStateFlow("Indian")
    val editLanguage = MutableStateFlow("English")
    val editPhone = MutableStateFlow("")
    val editAddressLine1 = MutableStateFlow("")
    val editAddressLine2 = MutableStateFlow("")
    val editCity = MutableStateFlow("")
    val editState = MutableStateFlow("")
    val editCountry = MutableStateFlow("India")
    val editPostalCode = MutableStateFlow("")

    init {
        refreshProfile()
        checkEligibility()
    }

    fun checkEligibility() {
        viewModelScope.launch {
            val res = apiClient.checkTripEligibility()
            if (res.isSuccess) {
                val data = res.getOrThrow()
                _uiState.value = _uiState.value.copy(eligibility = data)
                sessionRepo.saveKycStatus(data.kyc_status)
                sessionRepo.saveProfileCompletion(data.profile_completion)
            }
        }
    }

    fun refreshProfile() {
        val uid = userRepository.getCurrentUid() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            userRepository.getUserProfile(uid).onSuccess { user ->
                editFullName.value = user.full_name
                editBio.value = user.profile?.bio ?: ""
                editDob.value = user.profile?.date_of_birth ?: "1998-05-15"
                editGender.value = user.profile?.gender ?: "Male"
                editNationality.value = user.profile?.nationality ?: "Indian"
                editLanguage.value = user.profile?.language ?: "English"
                editPhone.value = user.phone ?: ""
                editAddressLine1.value = user.address?.address_line1 ?: ""
                editAddressLine2.value = user.address?.address_line2 ?: ""
                editCity.value = user.address?.city ?: ""
                editState.value = user.address?.state ?: ""
                editCountry.value = user.address?.country ?: "India"
                editPostalCode.value = user.address?.postal_code ?: ""
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { e ->
                currentUser.value?.let { u ->
                    editFullName.value = u.full_name
                    editBio.value = u.profile?.bio ?: ""
                    editDob.value = u.profile?.date_of_birth ?: "1998-05-15"
                    editGender.value = u.profile?.gender ?: "Male"
                    editNationality.value = u.profile?.nationality ?: "Indian"
                    editLanguage.value = u.profile?.language ?: "English"
                    editPhone.value = u.phone ?: ""
                    editAddressLine1.value = u.address?.address_line1 ?: ""
                    editAddressLine2.value = u.address?.address_line2 ?: ""
                    editCity.value = u.address?.city ?: ""
                    editState.value = u.address?.state ?: ""
                    editCountry.value = u.address?.country ?: "India"
                    editPostalCode.value = u.address?.postal_code ?: ""
                }
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun toggleEditMode() {
        val user = currentUser.value
        if (!_uiState.value.isEditMode && user != null) {
            editFullName.value = user.full_name
            editBio.value = user.profile?.bio ?: ""
            editDob.value = user.profile?.date_of_birth ?: "1998-05-15"
            editGender.value = user.profile?.gender ?: "Male"
            editNationality.value = user.profile?.nationality ?: "Indian"
            editLanguage.value = user.profile?.language ?: "English"
            editPhone.value = user.phone ?: ""
            editAddressLine1.value = user.address?.address_line1 ?: ""
            editAddressLine2.value = user.address?.address_line2 ?: ""
            editCity.value = user.address?.city ?: ""
            editState.value = user.address?.state ?: ""
            editCountry.value = user.address?.country ?: "India"
            editPostalCode.value = user.address?.postal_code ?: ""
        }
        _uiState.value = _uiState.value.copy(isEditMode = !_uiState.value.isEditMode)
    }

    fun saveProfile() {
        val uid = userRepository.getCurrentUid()
        if (uid == null) {
            _uiState.value = _uiState.value.copy(error = "User not authenticated")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            userRepository.updateProfile(
                uid = uid,
                fullName = editFullName.value,
                bio = editBio.value,
                nationality = editNationality.value,
                language = editLanguage.value,
                phone = editPhone.value
            ).onSuccess {
                // Also update backend address and check eligibility
                val addrPayload = mapOf(
                    "address_line1" to editAddressLine1.value,
                    "address_line2" to editAddressLine2.value,
                    "city" to editCity.value,
                    "state" to editState.value,
                    "country" to editCountry.value,
                    "postal_code" to editPostalCode.value,
                    "phone" to editPhone.value,
                    "email" to (currentUser.value?.email ?: "")
                )
                apiClient.saveProfileOnboardingStep(2, addrPayload)
                checkEligibility()

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isEditMode = false,
                    saveSuccess = true,
                    successMessage = "Profile updated successfully!"
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save: ${e.localizedMessage}"
                )
            }
        }
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        val uid = userRepository.getCurrentUid()
        if (uid == null) {
            _uiState.value = _uiState.value.copy(error = "Please log in to upload avatar")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            userRepository.uploadAvatar(context, uid, uri).onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, successMessage = "Avatar updated!")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Avatar upload failed: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null, saveSuccess = false)
    }
}
