package com.touristapp.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class EmergencyContact(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String = "",
    val name: String = "",
    @SerialName("primary_phone") val primaryPhone: String = "",
    @SerialName("alternate_phone") val alternatePhone: String = "",
    val email: String = "",
    val relationship: String = "Family", // Family, Friend, Spouse, Colleague, Other
    @SerialName("is_location_sharing_allowed") val isLocationSharingAllowed: Boolean = true,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class SafetySettings(
    @SerialName("user_id") val userId: String = "",
    @SerialName("lost_phone_mode_enabled") val lostPhoneModeEnabled: Boolean = false,
    @SerialName("safety_group_mode_enabled") val safetyGroupModeEnabled: Boolean = false,
    @SerialName("notifications_enabled") val notificationsEnabled: Boolean = true,
    @SerialName("last_known_location_enabled") val lastKnownLocationEnabled: Boolean = true,
    @SerialName("response_timeout") val responseTimeout: Int = 60, // in minutes (60, 120, 180, 300)
    @SerialName("grace_period") val gracePeriod: Int = 30, // 30, 60, 120, 180, 300 minutes
    @SerialName("vendor_alert_enabled") val vendorAlertEnabled: Boolean = true,
    @SerialName("trusted_contact_alert_enabled") val trustedContactAlertEnabled: Boolean = true,
    @SerialName("emergency_escalation_enabled") val emergencyEscalationEnabled: Boolean = true,
    @SerialName("auto_sync_enabled") val autoSyncEnabled: Boolean = true,
    @SerialName("offline_sms_enabled") val offlineSmsEnabled: Boolean = true,
    @SerialName("emergency_pin") val emergencyPin: String = "1234",
    @SerialName("heartbeat_interval_minutes") val heartbeatIntervalMinutes: Int = 15,
    @SerialName("emergency_message_template") val emergencyMessageTemplate: String =
        "EMERGENCY ALERT: [Traveler Name] has activated SOS. Location: [Location]. Time: [Time]. Trip: [Trip Name]. Please contact immediately.",
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class DeviceLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val address: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("is_live") val isLive: Boolean = true
)

@Serializable
data class DeviceStatus(
    @SerialName("user_id") val userId: String = "",
    @SerialName("device_id") val deviceId: String = "",
    @SerialName("battery_level") val batteryLevel: Int = 100, // 0-100
    @SerialName("is_charging") val isCharging: Boolean = false,
    @SerialName("network_status") val networkStatus: String = "ONLINE", // ONLINE, WIFI, CELLULAR, OFFLINE
    @SerialName("last_seen_at") val lastSeenAt: Long = System.currentTimeMillis(),
    @SerialName("last_app_opened_at") val lastAppOpenedAt: Long = System.currentTimeMillis(),
    @SerialName("last_location_at") val lastLocationAt: Long = System.currentTimeMillis(),
    @SerialName("last_known_location") val lastKnownLocation: DeviceLocation? = null,
    @SerialName("device_state") val deviceState: String = "ACTIVE", // ACTIVE, OFFLINE, UNREACHABLE, POSSIBLE_LOST, ALERT_PENDING, ALERT_SENT, VERIFIED_SAFE, ESCALATED
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SafetyEvent(
    @SerialName("event_id") val eventId: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String = "",
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("device_id") val deviceId: String = "",
    val type: String = "SAFETY_CHECK", // DEVICE_OFFLINE, DEVICE_UNREACHABLE, POSSIBLE_LOST, SAFETY_CHECK, VENDOR_ALERT, CONTACT_ALERT, SOS, DEVICE_RECONNECTED, NEED_HELP
    val status: String = "OPEN", // OPEN, ACKNOWLEDGED, RESOLVED, AWAITING_VERIFICATION, ESCALATED
    @SerialName("trigger_reason") val triggerReason: String = "",
    @SerialName("last_known_location") val lastKnownLocation: DeviceLocation? = null,
    @SerialName("last_known_location_at") val lastKnownLocationAt: Long? = null,
    @SerialName("escalation_stage") val escalationStage: String = "STAGE_0", // STAGE_0 (None), STAGE_1 (Vendor), STAGE_2 (Escalation), STAGE_3 (Contacts)
    @SerialName("delivery_status") val deliveryStatus: String = "PENDING", // PENDING, SENT, DELIVERED, FAILED
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @SerialName("resolved_at") val resolvedAt: Long? = null
)

@Serializable
data class SafetyAuditLog(
    @SerialName("log_id") val logId: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String = "",
    @SerialName("event_type") val eventType: String = "",
    val action: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class LostPhoneSettings(
    @SerialName("user_id") val userId: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = false,
    @SerialName("primary_contact_email") val primaryContactEmail: String = "",
    @SerialName("primary_contact_phone") val primaryContactPhone: String = "",
    @SerialName("alternate_contact_email") val alternateContactEmail: String = "",
    @SerialName("alternate_contact_phone") val alternateContactPhone: String = "",
    @SerialName("trusted_contact_name") val trustedContactName: String = "",
    val relationship: String = "Family",
    @SerialName("emergency_message") val emergencyMessage: String = "This device has not shown activity for the safety period. Please contact the traveler.",
    @SerialName("safety_timeout_hours") val safetyTimeoutHours: Int = 3, // 2, 3, 5, or custom
    @SerialName("last_app_open") val lastAppOpen: Long = System.currentTimeMillis(),
    @SerialName("last_heartbeat") val lastHeartbeat: Long = System.currentTimeMillis(),
    @SerialName("last_known_latitude") val lastKnownLatitude: Double = 0.0,
    @SerialName("last_known_longitude") val lastKnownLongitude: Double = 0.0,
    @SerialName("last_known_address") val lastKnownAddress: String = "",
    @SerialName("last_network_status") val lastNetworkStatus: String = "ONLINE",
    @SerialName("is_locked") val isLocked: Boolean = false
)

@Serializable
data class SosTimelineEvent(
    val stage: String, // SOS_ACTIVATED, LOCATION_ACQUIRED, CONTACTS_NOTIFIED, LEADER_NOTIFIED, DASHBOARD_ALERTED, HELP_RESPONDING, RESOLVED
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS", // SUCCESS, PENDING, OFFLINE, FAILED, PERMISSION_DENIED
    val detail: String? = null
)

@Serializable
data class ResponderInfo(
    @SerialName("responder_id") val responderId: String = "",
    @SerialName("responder_name") val responderName: String = "",
    @SerialName("responder_role") val responderRole: String = "Safety Operations",
    @SerialName("responder_phone") val responderPhone: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class EmergencyAlert(
    @SerialName("alert_id") val alertId: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String = "",
    @SerialName("user_name") val userName: String = "Traveler",
    @SerialName("user_email") val userEmail: String = "",
    @SerialName("user_phone") val userPhone: String = "",
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("trip_name") val tripName: String? = null,
    @SerialName("assigned_leader_id") val assignedLeaderId: String? = null,
    @SerialName("assigned_leader_name") val assignedLeaderName: String? = null,
    @SerialName("assigned_leader_phone") val assignedLeaderPhone: String? = null,
    @SerialName("emergency_type") val emergencyType: String = "General Emergency",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val address: String? = null,
    @SerialName("device_battery_percent") val deviceBatteryPercent: Int = 100,
    @SerialName("network_status") val networkStatus: String = "ONLINE", // ONLINE, OFFLINE_PENDING, NO_NETWORK
    @SerialName("gps_status") val gpsStatus: String = "ACTIVE", // ACTIVE, LAST_KNOWN, UNAVAILABLE
    @SerialName("emergency_contacts") val emergencyContacts: List<EmergencyContact> = emptyList(),
    val status: String = "ACTIVE", // ACTIVE, RESPONDING, RESOLVED, CANCELLED
    val priority: String = "EMERGENCY",
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("last_location_update") val lastLocationUpdate: Long = System.currentTimeMillis(),
    @SerialName("resolved_at") val resolvedAt: Long? = null,
    @SerialName("responder_info") val responderInfo: ResponderInfo? = null,
    @SerialName("timeline_events") val timelineEvents: List<SosTimelineEvent> = emptyList(),
    @SerialName("is_synced") val isSynced: Boolean = true
)

enum class EmergencyServiceType(val displayName: String) {
    POLICE("Police Station"),
    HOSPITAL("Hospital / Medical"),
    FIRE("Fire Station"),
    TOURIST_POLICE("Tourist Police Helpline"),
    AMBULANCE("Emergency Ambulance")
}

@Serializable
data class NearbyEmergencyService(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: EmergencyServiceType,
    val distanceKm: Double,
    val address: String,
    val phoneNumber: String,
    val latitude: Double,
    val longitude: Double,
    val isOpen24Hours: Boolean = true,
    val isOfficialApiSupported: Boolean = false
)

@Serializable
data class IncidentReport(
    @SerialName("incident_id") val incidentId: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String = "",
    @SerialName("user_name") val userName: String = "",
    @SerialName("trip_id") val tripId: String? = null,
    val title: String,
    val description: String,
    @SerialName("incident_type") val incidentType: String = "Safety Concern", // Theft, Medical, Lost Item, Harassment, Route Hazard, Other
    val severity: String = "Medium", // Low, Medium, High, Critical
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "SUBMITTED", // SUBMITTED, UNDER_REVIEW, RESOLVED
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class SafetyGroupSettings(
    @SerialName("user_id") val userId: String = "",
    @SerialName("is_enabled") val isEnabled: Boolean = false,
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_email") val userEmail: String = "",
    @SerialName("user_phone") val userPhone: String = "",
    @SerialName("first_alert_hours") val firstAlertHours: Int = 1, // 1, 2, 3, 5 hours
    @SerialName("vendor_alert_minutes") val vendorAlertMinutes: Int = 60,
    @SerialName("emergency_escalation_minutes") val emergencyEscalationMinutes: Int = 30,
    @SerialName("contact_alert_minutes") val contactAlertMinutes: Int = 60,
    @SerialName("last_response_at") val lastResponseAt: Long = System.currentTimeMillis(),
    @SerialName("last_activity_at") val lastActivityAt: Long = System.currentTimeMillis(),
    @SerialName("last_known_latitude") val lastKnownLatitude: Double = 0.0,
    @SerialName("last_known_longitude") val lastKnownLongitude: Double = 0.0,
    @SerialName("last_known_address") val lastKnownAddress: String = "",
    @SerialName("status") val status: String = "MONITORING", // MONITORING, NO_RESPONSE, VENDOR_ALERT, EMERGENCY_ESCALATION, SAFETY_CONTACTS_ALERT, RESOLVED, TRAVELER_RESPONDED
    @SerialName("active_alert_id") val activeAlertId: String? = null,
    @SerialName("contacts") val contacts: List<EmergencyContact> = emptyList()
)

@Serializable
data class OfficialEmergencyNumber(
    val number: String,
    val name: String,
    val description: String,
    val country: String = "India",
    val type: String = "General",
    val displayOrder: Int = 0
) {
    companion object {
        val INDIA_LIST = listOf(
            OfficialEmergencyNumber(
                number = "112",
                name = "National Emergency Helpline",
                description = "Police, Fire & Medical Response",
                country = "India",
                type = "National Police & Emergency",
                displayOrder = 1
            ),
            OfficialEmergencyNumber(
                number = "1363",
                name = "Tourist Helpline (24/7 Multi-Lingual)",
                description = "Ministry of Tourism, Govt of India",
                country = "India",
                type = "Tourist Helpline",
                displayOrder = 2
            ),
            OfficialEmergencyNumber(
                number = "1091",
                name = "Women Safety Helpline",
                description = "24/7 Immediate Police Support",
                country = "India",
                type = "Women Safety",
                displayOrder = 3
            ),
            OfficialEmergencyNumber(
                number = "108",
                name = "Emergency Ambulance & Casualty",
                description = "State Health & Trauma Services",
                country = "India",
                type = "Medical Casualty",
                displayOrder = 4
            )
        )
    }
}

