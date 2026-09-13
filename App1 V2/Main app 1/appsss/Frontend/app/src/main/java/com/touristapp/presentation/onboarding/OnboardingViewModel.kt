package com.touristapp.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.repository.SessionRepository
import com.touristapp.data.repository.UserRepository
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository = ServiceLocator.userRepository
) : ViewModel() {

    fun completeOnboarding(
        destinations: List<String>,
        styles: List<String>,
        interests: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val uid = userRepository.getCurrentUid()
                if (uid != null) {
                    userRepository.updatePreferences(
                        uid = uid,
                        destinations = destinations,
                        styles = styles,
                        interests = interests
                    )
                }
                sessionRepository.saveOnboardingStatus(true)
                onSuccess()
            } catch (e: Exception) {
                // If anything fails, still mark local onboarding complete so user is not blocked
                sessionRepository.saveOnboardingStatus(true)
                onSuccess()
            }
        }
    }

    fun skipOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val uid = userRepository.getCurrentUid()
                if (uid != null) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection(com.touristapp.data.firebase.FirestoreCollections.USERS)
                        .document(uid)
                        .set(mapOf("has_completed_onboarding" to true), com.google.firebase.firestore.SetOptions.merge())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            sessionRepository.saveOnboardingStatus(true)
            onSuccess()
        }
    }
}
