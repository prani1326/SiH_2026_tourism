package com.touristapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.touristapp.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tourist_session")

class SessionRepository(private val context: Context) {

    private object PreferencesKeys {
        val HAS_SEEN_WALKTHROUGH = booleanPreferencesKey("has_seen_walkthrough")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val ONBOARDING_STEP = intPreferencesKey("onboarding_step")
        val KYC_STATUS = stringPreferencesKey("kyc_status")
        val PROFILE_STATUS = stringPreferencesKey("profile_status")
        val PROFILE_COMPLETION = floatPreferencesKey("profile_completion")
        val USER_JSON = stringPreferencesKey("user_json")
        val IS_GUEST = booleanPreferencesKey("is_guest")
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
    }

    val hasSeenWalkthroughFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_SEEN_WALKTHROUGH] ?: false
    }

    val hasCompletedOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
    }

    val onboardingStepFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_STEP] ?: 1
    }

    val kycStatusFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.KYC_STATUS] ?: "NOT_STARTED"
    }

    val profileCompletionFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PROFILE_COMPLETION] ?: 0f
    }

    val profileStatusFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PROFILE_STATUS] ?: "PROFILE_INCOMPLETE"
    }

    val isGuestFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_GUEST] ?: false
    }

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
        try {
            AppThemeMode.valueOf(raw)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    suspend fun saveThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun saveWalkthroughSeen(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_WALKTHROUGH] = seen
        }
    }

    suspend fun saveOnboardingStatus(hasCompleted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = hasCompleted
        }
    }

    suspend fun saveOnboardingStep(step: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_STEP] = step
        }
    }

    suspend fun saveKycStatus(status: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KYC_STATUS] = status
        }
    }

    suspend fun saveProfileStatus(status: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROFILE_STATUS] = status
        }
    }

    suspend fun saveProfileCompletion(completion: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROFILE_COMPLETION] = completion
        }
    }

    suspend fun setGuestMode(isGuest: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GUEST] = isGuest
        }
    }

    suspend fun saveUserJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_JSON] = json
        }
    }

    suspend fun getUserJson(): String? {
        val preferences = context.dataStore.data.firstOrNull()
        return preferences?.get(PreferencesKeys.USER_JSON)
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_JSON)
            preferences.remove(PreferencesKeys.IS_GUEST)
            preferences.remove(PreferencesKeys.ONBOARDING_STEP)
            preferences.remove(PreferencesKeys.KYC_STATUS)
            preferences.remove(PreferencesKeys.PROFILE_COMPLETION)
            preferences.remove(PreferencesKeys.HAS_SEEN_WALKTHROUGH)
            preferences.remove(PreferencesKeys.HAS_COMPLETED_ONBOARDING)
        }
    }
}
