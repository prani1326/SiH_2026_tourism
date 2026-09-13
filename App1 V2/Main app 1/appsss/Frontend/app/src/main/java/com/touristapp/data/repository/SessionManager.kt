package com.touristapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.touristapp.data.firebase.FirestoreCollections
import com.touristapp.data.models.UserDto
import com.touristapp.ui.theme.AppThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages the global in-memory session state for the app, backed by SessionRepository & Firebase.
 */
class SessionManager(private val sessionRepository: SessionRepository) {

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    val currentUser: StateFlow<UserDto?> = _currentUser.asStateFlow()

    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            sessionRepository.themeModeFlow.collectLatest { mode ->
                _themeMode.value = mode
            }
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        sessionRepository.saveThemeMode(mode)
    }

    suspend fun loadUserFromStorage() {
        val userJson = sessionRepository.getUserJson()
        if (!userJson.isNullOrEmpty()) {
            try {
                val user = Json.decodeFromString<UserDto>(userJson)
                _currentUser.value = user
            } catch (e: Exception) {
                e.printStackTrace()
                _currentUser.value = null
            }
        }
        
        // Also refresh from Firestore if authenticated
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            fetchCurrentUser(uid)
        }
    }

    suspend fun fetchCurrentUser(uid: String? = null) {
        val targetUid = uid ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection(FirestoreCollections.USERS)
                .document(targetUid)
                .get()
                .await()
            if (doc.exists()) {
                val user = UserDto.fromFirestore(doc)
                saveUser(user)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveUser(user: UserDto) {
        _currentUser.value = user
        val userJson = Json.encodeToString(user)
        sessionRepository.saveUserJson(userJson)
        sessionRepository.saveKycStatus(user.kyc_status)
        sessionRepository.saveProfileCompletion(user.profile_completion)
        sessionRepository.saveOnboardingStep(user.onboarding_step)
        sessionRepository.saveProfileStatus(user.profile_status)
    }

    suspend fun clearSession() {
        _currentUser.value = null
        sessionRepository.clearSession()
    }
}
