package com.touristapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.touristapp.data.models.EmergencyAlert
import com.touristapp.data.models.EmergencyContact
import com.touristapp.data.models.IncidentReport
import com.touristapp.data.models.LostPhoneSettings
import com.touristapp.data.models.SafetyGroupSettings
import com.touristapp.data.models.SafetySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.safetyDataStore: DataStore<Preferences> by preferencesDataStore(name = "tourist_safety_store")

class SafetyLocalStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private object Keys {
        val EMERGENCY_CONTACTS = stringPreferencesKey("emergency_contacts_json")
        val SAFETY_SETTINGS = stringPreferencesKey("safety_settings_json")
        val LOST_PHONE_SETTINGS = stringPreferencesKey("lost_phone_settings_json")
        val SAFETY_GROUP_SETTINGS = stringPreferencesKey("safety_group_settings_json")
        val PENDING_OFFLINE_ALERTS = stringPreferencesKey("pending_offline_alerts_json")
        val ACTIVE_ALERT = stringPreferencesKey("active_alert_json")
        val INCIDENT_REPORTS = stringPreferencesKey("incident_reports_json")
    }

    // 1. Emergency Contacts
    val emergencyContactsFlow: Flow<List<EmergencyContact>> = context.safetyDataStore.data.map { prefs ->
        val raw = prefs[Keys.EMERGENCY_CONTACTS]
        if (raw.isNullOrBlank()) {
            defaultEmergencyContacts()
        } else {
            try {
                json.decodeFromString<List<EmergencyContact>>(raw)
            } catch (e: Exception) {
                defaultEmergencyContacts()
            }
        }
    }

    suspend fun getEmergencyContacts(): List<EmergencyContact> {
        val prefs = context.safetyDataStore.data.firstOrNull()
        val raw = prefs?.get(Keys.EMERGENCY_CONTACTS)
        return if (raw.isNullOrBlank()) {
            defaultEmergencyContacts()
        } else {
            try {
                json.decodeFromString<List<EmergencyContact>>(raw)
            } catch (e: Exception) {
                defaultEmergencyContacts()
            }
        }
    }

    suspend fun saveEmergencyContacts(contacts: List<EmergencyContact>) {
        context.safetyDataStore.edit { prefs ->
            prefs[Keys.EMERGENCY_CONTACTS] = json.encodeToString(contacts)
        }
    }

    suspend fun saveOrUpdateContact(contact: EmergencyContact) {
        val current = getEmergencyContacts().toMutableList()
        val idx = current.indexOfFirst { it.id == contact.id }
        if (idx >= 0) {
            current[idx] = contact
        } else {
            current.add(contact)
        }
        saveEmergencyContacts(current)
    }

    suspend fun deleteContact(contactId: String) {
        val current = getEmergencyContacts().filterNot { it.id == contactId }
        saveEmergencyContacts(current)
    }

    // 2. Safety Settings
    val safetySettingsFlow: Flow<SafetySettings> = context.safetyDataStore.data.map { prefs ->
        val raw = prefs[Keys.SAFETY_SETTINGS]
        if (raw.isNullOrBlank()) SafetySettings() else {
            try {
                json.decodeFromString<SafetySettings>(raw)
            } catch (e: Exception) {
                SafetySettings()
            }
        }
    }

    suspend fun getSafetySettings(): SafetySettings {
        val prefs = context.safetyDataStore.data.firstOrNull()
        val raw = prefs?.get(Keys.SAFETY_SETTINGS)
        return if (raw.isNullOrBlank()) SafetySettings() else {
            try {
                json.decodeFromString<SafetySettings>(raw)
            } catch (e: Exception) {
                SafetySettings()
            }
        }
    }

    suspend fun saveSafetySettings(settings: SafetySettings) {
        context.safetyDataStore.edit { prefs ->
            prefs[Keys.SAFETY_SETTINGS] = json.encodeToString(settings)
        }
    }

    // 3. Lost Phone Settings
    val lostPhoneSettingsFlow: Flow<LostPhoneSettings> = context.safetyDataStore.data.map { prefs ->
        val raw = prefs[Keys.LOST_PHONE_SETTINGS]
        if (raw.isNullOrBlank()) LostPhoneSettings() else {
            try {
                json.decodeFromString<LostPhoneSettings>(raw)
            } catch (e: Exception) {
                LostPhoneSettings()
            }
        }
    }

    suspend fun getLostPhoneSettings(): LostPhoneSettings {
        val prefs = context.safetyDataStore.data.firstOrNull()
        val raw = prefs?.get(Keys.LOST_PHONE_SETTINGS)
        return if (raw.isNullOrBlank()) LostPhoneSettings() else {
            try {
                json.decodeFromString<LostPhoneSettings>(raw)
            } catch (e: Exception) {
                LostPhoneSettings()
            }
        }
    }

    suspend fun saveLostPhoneSettings(settings: LostPhoneSettings) {
        context.safetyDataStore.edit { prefs ->
            prefs[Keys.LOST_PHONE_SETTINGS] = json.encodeToString(settings)
        }
    }

    // 4. Safety Group Settings
    val safetyGroupSettingsFlow: Flow<SafetyGroupSettings> = context.safetyDataStore.data.map { prefs ->
        val raw = prefs[Keys.SAFETY_GROUP_SETTINGS]
        if (raw.isNullOrBlank()) SafetyGroupSettings() else {
            try {
                json.decodeFromString<SafetyGroupSettings>(raw)
            } catch (e: Exception) {
                SafetyGroupSettings()
            }
        }
    }

    suspend fun getSafetyGroupSettings(): SafetyGroupSettings {
        val prefs = context.safetyDataStore.data.firstOrNull()
        val raw = prefs?.get(Keys.SAFETY_GROUP_SETTINGS)
        return if (raw.isNullOrBlank()) SafetyGroupSettings() else {
            try {
                json.decodeFromString<SafetyGroupSettings>(raw)
            } catch (e: Exception) {
                SafetyGroupSettings()
            }
        }
    }

    suspend fun saveSafetyGroupSettings(settings: SafetyGroupSettings) {
        context.safetyDataStore.edit { prefs ->
            prefs[Keys.SAFETY_GROUP_SETTINGS] = json.encodeToString(settings)
        }
    }

    // 5. Active Alert Persistence
    val activeAlertFlow: Flow<EmergencyAlert?> = context.safetyDataStore.data.map { prefs ->
        val raw = prefs[Keys.ACTIVE_ALERT]
        if (raw.isNullOrBlank()) null else {
            try {
                json.decodeFromString<EmergencyAlert>(raw)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getActiveAlert(): EmergencyAlert? {
        val prefs = context.safetyDataStore.data.firstOrNull()
        val raw = prefs?.get(Keys.ACTIVE_ALERT)
        return if (raw.isNullOrBlank()) null else {
            try {
                json.decodeFromString<EmergencyAlert>(raw)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveActiveAlert(alert: EmergencyAlert?) {
        context.safetyDataStore.edit { prefs ->
            if (alert == null) {
                prefs.remove(Keys.ACTIVE_ALERT)
            } else {
                prefs[Keys.ACTIVE_ALERT] = json.encodeToString(alert)
            }
        }
    }

    // 5. Offline Queued Alerts
    suspend fun queueOfflineAlert(alert: EmergencyAlert) {
        val prefs = context.safetyDataStore.data.firstOrNull()
        val raw = prefs?.get(Keys.PENDING_OFFLINE_ALERTS)
        val list = if (raw.isNullOrBlank()) mutableListOf() else {
            try {
                json.decodeFromString<List<EmergencyAlert>>(raw).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
        }
        list.add(alert.copy(isSynced = false, networkStatus = "OFFLINE_PENDING"))
        context.safetyDataStore.edit { p ->
            p[Keys.PENDING_OFFLINE_ALERTS] = json.encodeToString(list)
        }
    }

    suspend fun getPendingOfflineAlerts(): List<EmergencyAlert> {
        val prefs = context.safetyDataStore.data.firstOrNull()
        val raw = prefs?.get(Keys.PENDING_OFFLINE_ALERTS)
        return if (raw.isNullOrBlank()) emptyList() else {
            try {
                json.decodeFromString(raw)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun clearPendingOfflineAlert(alertId: String) {
        val current = getPendingOfflineAlerts().filterNot { it.alertId == alertId }
        context.safetyDataStore.edit { p ->
            p[Keys.PENDING_OFFLINE_ALERTS] = json.encodeToString(current)
        }
    }

    // 6. Incident Reports
    suspend fun saveIncidentReport(report: IncidentReport) {
        val prefs = context.safetyDataStore.data.firstOrNull()
        val raw = prefs?.get(Keys.INCIDENT_REPORTS)
        val list = if (raw.isNullOrBlank()) mutableListOf() else {
            try {
                json.decodeFromString<List<IncidentReport>>(raw).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
        }
        list.add(0, report)
        context.safetyDataStore.edit { p ->
            p[Keys.INCIDENT_REPORTS] = json.encodeToString(list)
        }
    }

    suspend fun getIncidentReports(): List<IncidentReport> {
        val prefs = context.safetyDataStore.data.firstOrNull()
        val raw = prefs?.get(Keys.INCIDENT_REPORTS)
        return if (raw.isNullOrBlank()) emptyList() else {
            try {
                json.decodeFromString(raw)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun defaultEmergencyContacts(): List<EmergencyContact> {
        return listOf(
            EmergencyContact(
                name = "Primary Family Contact",
                relationship = "Family",
                primaryPhone = "+91 9876543210",
                email = "family.emergency@touristapp.com",
                isLocationSharingAllowed = true,
                isEnabled = true,
                isVerified = true
            ),
            EmergencyContact(
                name = "Tour Group Coordinator",
                relationship = "Trip Leader",
                primaryPhone = "+91 9123456789",
                email = "guide.support@touristapp.com",
                isLocationSharingAllowed = true,
                isEnabled = true,
                isVerified = true
            )
        )
    }
}
