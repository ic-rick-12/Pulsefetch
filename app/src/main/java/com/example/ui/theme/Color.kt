package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Core AMOLED & Neutral Dark-Grey Surfaces
val AmoledBlack = Color(0xFF000000)
val DarkCarbonBackground = Color(0xFF0A0D12)
val CarbonSurface = Color(0xFF0B0E13)
val CarbonElevated = Color(0xFF12161E)
val CarbonCard = Color(0xFF181D27)
val CarbonBorder = Color(0xFF242B38)

// Daylight / Light Mode Surfaces
val DaylightBackground = Color(0xFFF5F7FA)
val DaylightSurface = Color(0xFFFFFFFF)
val DaylightElevated = Color(0xFFEEF2F6)
val DaylightCard = Color(0xFFE2E8F0)
val DaylightBorder = Color(0xFFCBD5E1)

// Default White / Silver Tokens
val WhiteSilverPrimary = Color(0xFFF8FAFC)
val SilverSecondary = Color(0xFFCBD5E1)

// Preset Cyan & Teal Tokens
val ElectricCyan = Color(0xFF00E5FF)
val ElectricCyanDim = Color(0xFF00B8D4)
val OnElectricCyan = Color(0xFF002227)
val ElectricCyanContainer = Color(0xFF00363F)
val OnElectricCyanContainer = Color(0xFFA8F5FF)

val DeepTeal = Color(0xFF00D4B2)
val OnDeepTeal = Color(0xFF002820)
val DeepTealContainer = Color(0xFF003B31)
val OnDeepTealContainer = Color(0xFF7FFCE2)

val NeonMint = Color(0xFF64FFDA)
val AmberWarning = Color(0xFFFFB300)
val CoralError = Color(0xFFFF5252)
val OnCoralError = Color(0xFF3B0004)
val CoralErrorContainer = Color(0xFF440D13)

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

fun contrastingContentColor(background: Color): Color {
    return if (background.luminance() > 0.42f) {
        Color(0xFF070A0F)
    } else {
        Color(0xFFF8FAFC)
    }
}

fun blendColors(fg: Color, bg: Color, fgRatio: Float): Color {
    val r = fgRatio.coerceIn(0f, 1f)
    val inv = 1f - r
    return Color(
        red = fg.red * r + bg.red * inv,
        green = fg.green * r + bg.green * inv,
        blue = fg.blue * r + bg.blue * inv,
        alpha = 1f
    )
}
