package com.touristapp.data.models

import kotlinx.serialization.Serializable

@Serializable
data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val localeTag: String = code
)

val SupportedLanguages = listOf(
    LanguageItem(code = "auto", name = "Auto Detect", nativeName = "Auto Detect", flagEmoji = "🌐", localeTag = "en"),
    LanguageItem(code = "en", name = "English", nativeName = "English", flagEmoji = "🇺🇸", localeTag = "en"),
    LanguageItem(code = "hi", name = "Hindi", nativeName = "हिन्दी", flagEmoji = "🇮🇳", localeTag = "hi"),
    LanguageItem(code = "es", name = "Spanish", nativeName = "Español", flagEmoji = "🇪🇸", localeTag = "es"),
    LanguageItem(code = "fr", name = "French", nativeName = "Français", flagEmoji = "🇫🇷", localeTag = "fr"),
    LanguageItem(code = "de", name = "German", nativeName = "Deutsch", flagEmoji = "🇩🇪", localeTag = "de"),
    LanguageItem(code = "ja", name = "Japanese", nativeName = "日本語", flagEmoji = "🇯🇵", localeTag = "ja"),
    LanguageItem(code = "it", name = "Italian", nativeName = "Italiano", flagEmoji = "🇮🇹", localeTag = "it"),
    LanguageItem(code = "zh", name = "Chinese", nativeName = "中文", flagEmoji = "🇨🇳", localeTag = "zh"),
    LanguageItem(code = "ar", name = "Arabic", nativeName = "العربية", flagEmoji = "🇸🇦", localeTag = "ar"),
    LanguageItem(code = "ru", name = "Russian", nativeName = "Русский", flagEmoji = "🇷🇺", localeTag = "ru"),
    LanguageItem(code = "pt", name = "Portuguese", nativeName = "Português", flagEmoji = "🇵🇹", localeTag = "pt"),
    LanguageItem(code = "bn", name = "Bengali", nativeName = "বাংলা", flagEmoji = "🇮🇳", localeTag = "bn"),
    LanguageItem(code = "ta", name = "Tamil", nativeName = "தமிழ்", flagEmoji = "🇮🇳", localeTag = "ta"),
    LanguageItem(code = "te", name = "Telugu", nativeName = "తెలుగు", flagEmoji = "🇮🇳", localeTag = "te"),
    LanguageItem(code = "ko", name = "Korean", nativeName = "한국어", flagEmoji = "🇰🇷", localeTag = "ko")
)

@Serializable
data class TranslationRecord(
    val translationId: String = "",
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "en",
    val originalText: String = "",
    val translatedText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class SavedPlaceItem(
    val placeId: String = "",
    val placeName: String = "",
    val location: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val category: String = "Landmark",
    val rating: Double = 4.8
)

@Serializable
data class LandmarkInfo(
    val name: String,
    val city: String,
    val country: String,
    val builtDate: String,
    val whyFamous: String,
    val history: String,
    val facts: List<String>,
    val culturalSignificance: String,
    val bestTimeToVisit: String,
    val nearbyAttractions: List<String>,
    val visitorTips: List<String>,
    val confidence: Float,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val candidateMatches: List<String> = emptyList(),
    val isConfident: Boolean = confidence >= 0.65f
)

enum class VoiceSender {
    TRAVELER,
    LOCAL
}

@Serializable
data class VoiceChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: VoiceSender = VoiceSender.TRAVELER,
    val originalText: String = "",
    val translatedText: String = "",
    val sourceLang: String = "en",
    val targetLang: String = "hi",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TravelGuideDestination(
    val id: String,
    val name: String,
    val stateCountry: String,
    val tagline: String,
    val overview: String,
    val history: String,
    val culture: String,
    val localTraditions: List<String>,
    val famousAttractions: List<String>,
    val famousFood: List<String>,
    val thingsToDo: List<String>,
    val safetyTips: List<String>,
    val transportationTips: String,
    val bestVisitingTime: String,
    val nearbyPlaces: List<String>,
    val usefulPhrases: List<Pair<String, String>>,
    val touristTips: List<String>,
    val imageUrl: String? = null
)
