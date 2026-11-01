package com.example.data.model

import kotlin.math.roundToInt

enum class AppearanceMode(val label: String, val subtitle: String) {
    AMOLED(
        label = "AMOLED Mode",
        subtitle = "Pure #000000 black background, dark-grey surfaces & minimal borders"
    ),
    LIQUID_GLASS(
        label = "Liquid Glass",
        subtitle = "iOS-inspired frosted translucent panels, blur & floating navigation"
    )
}

enum class GlassIntensity(
    val label: String,
    val description: String,
    val surfaceAlpha: Float,
    val elevatedAlpha: Float,
    val borderHighlightAlpha: Float,
    val ambientGlowAlpha: Float,
    val blurRadiusDp: Float
) {
    LOW(
        label = "Low",
        description = "Subtle frosted tint with high contrast",
        surfaceAlpha = 0.84f,
        elevatedAlpha = 0.90f,
        borderHighlightAlpha = 0.24f,
        ambientGlowAlpha = 0.14f,
        blurRadiusDp = 12f
    ),
    MEDIUM(
        label = "Medium",
        description = "Balanced translucent glass & specular reflections",
        surfaceAlpha = 0.66f,
        elevatedAlpha = 0.74f,
        borderHighlightAlpha = 0.40f,
        ambientGlowAlpha = 0.24f,
        blurRadiusDp = 22f
    ),
    HIGH(
        label = "High",
        description = "Deep frosted translucency & luminous depth",
        surfaceAlpha = 0.48f,
        elevatedAlpha = 0.58f,
        borderHighlightAlpha = 0.58f,
        ambientGlowAlpha = 0.36f,
        blurRadiusDp = 34f
    )
}

enum class AccentPreset(
    val label: String,
    val defaultHex: String,
    val primaryColorLong: Long,
    val secondaryColorLong: Long
) {
    WHITE_SILVER("White / Silver", "#F8FAFC", 0xFFF8FAFC, 0xFFCBD5E1),
    CYAN("Cyan", "#00E5FF", 0xFF00E5FF, 0xFF00D4B2),
    ELECTRIC_BLUE("Electric Blue", "#2979FF", 0xFF2979FF, 0xFF00B0FF),
    PURPLE("Purple", "#AA00FF", 0xFFAA00FF, 0xFFE040FB),
    VIOLET("Violet", "#7C4DFF", 0xFF7C4DFF, 0xFFB388FF),
    EMERALD_GREEN("Emerald Green", "#00E676", 0xFF00E676, 0xFF1DE9B6),
    LIME("Lime", "#C6FF00", 0xFFC6FF00, 0xFF76FF03),
    ORANGE("Orange", "#FF9100", 0xFFFF9100, 0xFFFFAB40),
    RED("Red", "#FF1744", 0xFFFF1744, 0xFFFF5252),
    ROSE_PINK("Rose Pink", "#F50057", 0xFFF50057, 0xFFFF4081),
    SUNSET_ORANGE("Sunset Orange", "#FF6D00", 0xFFFF6D00, 0xFFFF3D00),
    TEAL("Teal", "#00BFA5", 0xFF00BFA5, 0xFF64FFDA),
    CUSTOM("Custom", "#F8FAFC", 0xFFF8FAFC, 0xFFCBD5E1);

    companion object {
        val standardPresets: List<AccentPreset> = entries.filter { it != CUSTOM }
    }
}

enum class BackgroundStyle(
    val label: String,
    val subtitle: String,
    val defaultHex: String
) {
    PURE_BLACK("Pure Black", "True #000000 AMOLED zero-lit pixels", "#000000"),
    DARK("Dark", "Deep carbon-slate #0A0D12 atmosphere", "#0A0D12"),
    CUSTOM("Custom", "Personalized HEX background tone", "#080B10")
}

enum class ThemePreference(val label: String, val subtitle: String) {
    DARK("Dark", "Optimized for AMOLED & low-light viewing"),
    LIGHT("Light", "Crisp daylight surfaces with subtle silver contrast"),
    SYSTEM("System", "Follow Android device dark/light mode")
}

enum class SearchEngine(
    val label: String,
    val homeUrl: String,
    val queryTemplate: String
) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com", "https://duckduckgo.com/?q=%s"),
    GOOGLE("Google", "https://www.google.com", "https://www.google.com/search?q=%s"),
    BRAVE("Brave Search", "https://search.brave.com", "https://search.brave.com/search?q=%s"),
    BING("Bing", "https://www.bing.com", "https://www.bing.com/search?q=%s")
}

object ColorHexUtils {
    private val hexRegex = Regex("^#?([0-9A-Fa-f]{6})$")

    fun normalizeHexOrNull(rawInput: String): String? {
        val cleaned = rawInput.trim().removePrefix("#")
        if (!hexRegex.matches(cleaned)) return null
        return "#${cleaned.uppercase()}"
    }

    fun parseHexToArgbLong(rawHex: String, fallback: Long = 0xFFF8FAFC): Long {
        val normalized = normalizeHexOrNull(rawHex) ?: return fallback
        val rgb = normalized.removePrefix("#").toLongOrNull(16) ?: return fallback
        return 0xFF000000L or rgb
    }

    fun hsvToHex(hue: Float, saturation: Float, value: Float): String {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)

        val c = v * s
        val x = c * (1f - kotlin.math.abs(((h / 60f) % 2f) - 1f))
        val m = v - c

        val (rPrime, gPrime, bPrime) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((rPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((gPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((bPrime + m) * 255f).roundToInt().coerceIn(0, 255)

        return String.format("#%02X%02X%02X", r, g, b)
    }
}
