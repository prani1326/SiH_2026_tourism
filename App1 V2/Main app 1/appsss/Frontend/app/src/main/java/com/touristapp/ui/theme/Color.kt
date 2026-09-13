package com.touristapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * App theme mode options.
 */
enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

// Primary Brand Colors (Travel Blue Identity)
val TravelPrimary = Color(0xFF1A73E8)
val TravelDarkPrimary = Color(0xFF0B57D0)
val TravelLightPrimary = Color(0xFFE8F0FE)
val TravelNavy = Color(0xFF1E293B)

// Accent & Status Colors
val TravelSuccess = Color(0xFF10B981)
val TravelWarning = Color(0xFFF59E0B)
val TravelOrange = TravelWarning
val PrimaryOrange = Color(0xFFE65100)
val TravelEmergency = Color(0xFFEF4444)

// Neutral & Background Colors (Light Mode)
val BackgroundLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFF8FAFC)
val CardBorderLight = Color(0xFFE2E8F0)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF94A3B8)

// Dark Mode Neutral Colors
val BackgroundDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val CardBorderDark = Color(0xFF334155)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFFCBD5E1)
val TextMutedDark = Color(0xFF64748B)

// Backwards compatibility aliases
val EmeraldPrimary = TravelPrimary
val EmeraldDark = TravelDarkPrimary
val OrangeAccent = TravelWarning
val OrangeAccentDark = Color(0xFFD97706)
val OrangeAccentLight = Color(0xFFFEF3C7)
val ErrorRed = TravelEmergency
val ErrorRedDark = Color(0xFFF87171)

/**
 * Semantic helper extensions to get theme-aware colors effortlessly across all screens.
 */
val ColorScheme.isDarkMode: Boolean
    get() = background == BackgroundDark

val ColorScheme.brandPillBackground: Color
    get() = if (isDarkMode) TravelPrimary.copy(alpha = 0.22f) else TravelLightPrimary

val ColorScheme.cardBackground: Color
    get() = surface

val ColorScheme.subtleBorder: Color
    get() = if (isDarkMode) CardBorderDark else CardBorderLight