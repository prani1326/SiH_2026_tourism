package com.touristapp.data.models

import kotlinx.serialization.Serializable

@Serializable
data class SafetyIntelligenceDto(
    val destination: String = "Jaipur",
    val safety_score: Int = 91,
    val risk_label: String = "Very Safe",
    val risk_color: String = "#10B981",
    val is_night: Boolean = false,
    val day_safety: Int = 96,
    val night_safety: Int = 82,
    val crowd_risk: String = "Medium",
    val weather_risk: String = "Low",
    val scam_risk: String = "Low",
    val network_status: String = "Strong 5G / 4G",
    val nearby_police_stations: Int = 3,
    val nearby_hospitals: Int = 4,
    val explanation: String = "Favorable conditions with active tourist police patrols.",
    val updated_at: String = ""
)

@Serializable
data class SafeRouteDto(
    val id: String = "route_safest",
    val name: String = "Route B (Main Boulevard)",
    val category: String = "Recommended",
    val is_recommended: Boolean = true,
    val duration_min: Int = 27,
    val distance_km: Double = 6.8,
    val safety_score: Int = 93,
    val lighting_quality: String = "Well-lit & CCTV Monitored",
    val police_patrols: Boolean = true,
    val cost_inr: Int = 180,
    val highlights: String = "Passes through tourist police kiosk and main transit corridors.",
    val explanation: String = "Route B is 7 mins longer but has significantly better safety and lighting."
)

@Serializable
data class CrowdAlternativeDto(
    val name: String = "Mehtab Bagh",
    val crowd_level: String = "Low (24%)",
    val wait_time_min: Int = 5,
    val distance_away: String = "12 min away",
    val highlights: String = "Peaceful view with zero queue."
)

@Serializable
data class HourlyCrowdTrendDto(
    val hour: String = "8 AM",
    val density_pct: Int = 35
)

@Serializable
data class CrowdIntelligenceDto(
    val monument_name: String = "Amber Fort",
    val destination: String = "Jaipur",
    val crowd_badge: String = "HIGH",
    val badge_color: String = "#EF4444",
    val crowd_density_pct: Int = 78,
    val estimated_wait_min: Int = 30,
    val best_visiting_window: String = "8:00 AM – 10:00 AM",
    val traffic_level: String = "Moderate",
    val peak_hours: String = "11:30 AM – 3:30 PM",
    val hourly_trend: List<HourlyCrowdTrendDto> = emptyList(),
    val alternatives: List<CrowdAlternativeDto> = emptyList(),
    val data_source: String = "Government Tourism Board & Live Sensor Aggregates",
    val updated_at: String = ""
)

@Serializable
data class GuardianTrustedContactDto(
    val name: String = "Family Contact",
    val phone: String = "+91 98765 43210",
    val relationship: String = "Family"
)

@Serializable
data class GuardianSessionDto(
    val session_id: String = "",
    val user_id: String = "",
    val trip_name: String = "Jaipur Heritage Tour",
    val status: String = "ACTIVE_ON_SCHEDULE",
    val is_sharing_enabled: Boolean = true,
    val current_location_name: String = "Amber Fort, Jaipur",
    val latitude: Double = 26.9855,
    val longitude: Double = 75.8513,
    val battery_level: Int = 74,
    val network_status: String = "Strong 5G",
    val last_check_in: String = "10:32 AM",
    val next_check_in: String = "1:32 PM",
    val check_in_interval_hours: Int = 3,
    val trusted_contacts: List<GuardianTrustedContactDto> = emptyList(),
    val created_at: String = "",
    val expires_at: String = ""
)

@Serializable
data class HelplineItemDto(
    val name: String = "Tourist Police Helpline",
    val number: String = "1363"
)

@Serializable
data class EmergencyBundleDto(
    val destination: String = "Jaipur",
    val destination_helplines: List<HelplineItemDto> = emptyList(),
    val offline_instructions: List<String> = emptyList()
)

@Serializable
data class HeritageMonumentDto(
    val id: String = "amber_fort",
    val name: String = "Amber Fort (Amer Palace)",
    val location: String = "Amer, Jaipur, Rajasthan",
    val built_period: String = "1592 AD (16th Century)",
    val built_by: String = "Raja Man Singh I",
    val architectural_style: String = "Rajput & Mughal Fusion",
    val material: String = "Red Sandstone and Pale Yellow Marble",
    val unesco_status: String = "UNESCO Hill Forts of Rajasthan",
    val historical_context: String = "Perched high on the rugged Cheel ka Teela, Amber Fort was the principal seat of the Kachwaha Rajputs.",
    val did_you_know: List<String> = emptyList(),
    val narration_script_en: String = "",
    val narration_script_hi: String = ""
)

@Serializable
data class HeritageAnalysisDto(
    val success: Boolean = true,
    val match_confidence: Double = 0.96,
    val monument: HeritageMonumentDto = HeritageMonumentDto(),
    val audio_languages: List<String> = emptyList(),
    val analyzed_at: String = ""
)

@Serializable
data class ScamAlertDto(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val severity: String = "Medium",
    val description: String = "",
    val verified_remedy: String = "",
    val fair_price_benchmark: String = "",
    val reported_hotspot: String = "",
    val reports_count: Int = 0
)

@Serializable
data class SustainabilityPillarDto(
    val score: Int = 18,
    val max: Int = 20,
    val label: String = ""
)

@Serializable
data class SustainabilityScoreDto(
    val trip_id: String = "",
    val overall_score: Int = 82,
    val tier: String = "Eco-Champion (Gold)",
    val carbon_offset_kg: Double = 34.8,
    val recommendations: List<String> = emptyList()
)

@Serializable
data class LocalExperienceDto(
    val id: String = "",
    val destination: String = "Jaipur",
    val title: String = "",
    val category: String = "",
    val host_name: String = "",
    val verified_badge: Boolean = true,
    val rating: Double = 4.9,
    val reviews_count: Int = 120,
    val price_inr: Int = 750,
    val duration_hours: Double = 2.5,
    val location: String = "",
    val languages: List<String> = emptyList(),
    val accessibility: String = "",
    val eco_score: Int = 95,
    val description: String = "",
    val image_url: String = ""
)
