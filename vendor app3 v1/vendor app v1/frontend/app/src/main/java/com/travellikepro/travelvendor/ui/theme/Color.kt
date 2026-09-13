/**
 * TravelVendor Color Palette & Visual Identity Design System
 *
 * Rationale:
 * A travel and tourism vendor application requires an identity that feels warm, adventurous,
 * trustworthy, and energetic. We avoid cold corporate blue/grey in favor of:
 * - Terracotta Rust (#D9531E) as Primary: Evokes earth, mountain sunsets, warmth, and trail journeys.
 * - Emerald Pine (#0D7A5F) as Secondary/Success: Symbolizes nature, forests, approval, and prosperity.
 * - Sunburst Amber (#E69500) as Tertiary/Warning: Represents warmth, hospitality, and pending status.
 * - Deep Charcoal Slate (#121D24) for Dark Surfaces: Delivers a sleek modern dark mode.
 * - Soft Warm Cream (#FBF9F5) for Light Backgrounds: Soft on eyes compared to harsh pure white.
 */

package com.travellikepro.travelvendor.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Core Colors
val TerracottaPrimary = Color(0xFFD9531E)
val TerracottaOnPrimary = Color(0xFFFFFFFF)
val TerracottaContainer = Color(0xFFFFDBCF)
val OnTerracottaContainer = Color(0xFF3B0900)

val EmeraldSecondary = Color(0xFF0D7A5F)
val EmeraldOnSecondary = Color(0xFFFFFFFF)
val EmeraldContainer = Color(0xFFA6F2D6)
val OnEmeraldContainer = Color(0xFF002117)

val AmberTertiary = Color(0xFFD97706)
val AmberOnTertiary = Color(0xFFFFFFFF)
val AmberContainer = Color(0xFFFFDDB8)
val OnAmberContainer = Color(0xFF2A1700)

// Neutral Colors - Light Mode
val LightBackground = Color(0xFFFBF9F5)
val LightOnBackground = Color(0xFF1F1B18)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1F1B18)
val LightSurfaceVariant = Color(0xFFF2ECE6)
val LightOnSurfaceVariant = Color(0xFF52443D)
val LightOutline = Color(0xFF85736B)

// Neutral Colors - Dark Mode
val DarkBackground = Color(0xFF121D24)
val DarkOnBackground = Color(0xFFE6E2DE)
val DarkSurface = Color(0xFF1A2630)
val DarkOnSurface = Color(0xFFE6E2DE)
val DarkSurfaceVariant = Color(0xFF293642)
val DarkOnSurfaceVariant = Color(0xFFD4C3BB)
val DarkOutline = Color(0xFF9C8D85)

// Status Colors
val StatusPendingBg = Color(0xFFFFF7ED)
val StatusPendingText = Color(0xFFC2410C)

val StatusApprovedBg = Color(0xFFECFDF5)
val StatusApprovedText = Color(0xFF047857)

val StatusRejectedBg = Color(0xFFFEF2F2)
val StatusRejectedText = Color(0xFFDC2626)

val StatusBlueBg = Color(0xFFEFF6FF)
val StatusBlueText = Color(0xFF1D4ED8)
