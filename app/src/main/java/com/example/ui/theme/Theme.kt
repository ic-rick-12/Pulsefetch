package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AccentPreset
import com.example.data.model.AppearanceMode
import com.example.data.model.BackgroundStyle
import com.example.data.model.ColorHexUtils
import com.example.data.model.GlassIntensity
import com.example.data.model.ThemePreference
import com.example.data.preferences.UserPreferences

@Immutable
data class PulseFetchThemeConfig(
    val appearanceMode: AppearanceMode = AppearanceMode.AMOLED,
    val accentPreset: AccentPreset = AccentPreset.WHITE_SILVER,
    val accentColor: Color = WhiteSilverPrimary,
    val secondaryAccentColor: Color = SilverSecondary,
    val glassIntensity: GlassIntensity = GlassIntensity.MEDIUM,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.PURE_BLACK,
    val themePreference: ThemePreference = ThemePreference.DARK,
    val isLightTheme: Boolean = false
) {
    val isLiquidGlass: Boolean
        get() = appearanceMode == AppearanceMode.LIQUID_GLASS
}

val LocalPulseFetchTheme = staticCompositionLocalOf { PulseFetchThemeConfig() }

@Composable
fun MyApplicationTheme(
    preferences: UserPreferences = UserPreferences(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useLightTheme = when (preferences.themePreference) {
        ThemePreference.DARK -> false
        ThemePreference.LIGHT -> true
        ThemePreference.SYSTEM -> !systemDark
    }

    val rawPrimary = when {
        preferences.accentPreset == AccentPreset.CUSTOM -> {
            Color(ColorHexUtils.parseHexToArgbLong(preferences.customAccentHex, 0xFFF8FAFC))
        }
        useLightTheme && preferences.accentPreset == AccentPreset.WHITE_SILVER -> {
            // In light mode, keep Silver/Slate legible against white daylight surfaces
            Color(0xFF1E293B)
        }
        else -> {
            Color(preferences.accentPreset.primaryColorLong)
        }
    }

    val rawSecondary = when {
        preferences.accentPreset == AccentPreset.CUSTOM -> {
            blendColors(rawPrimary, SilverSecondary, 0.65f)
        }
        useLightTheme && preferences.accentPreset == AccentPreset.WHITE_SILVER -> {
            Color(0xFF475569)
        }
        else -> {
            Color(preferences.accentPreset.secondaryColorLong)
        }
    }

    val colorTransitionSpec = tween<Color>(durationMillis = 260, easing = FastOutSlowInEasing)

    val animatedPrimary by animateColorAsState(
        targetValue = rawPrimary,
        animationSpec = colorTransitionSpec,
        label = "accent_primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = rawSecondary,
        animationSpec = colorTransitionSpec,
        label = "accent_secondary"
    )

    val resolvedBaseBackground = when {
        useLightTheme -> DaylightBackground
        preferences.backgroundStyle == BackgroundStyle.PURE_BLACK -> AmoledBlack
        preferences.backgroundStyle == BackgroundStyle.DARK -> DarkCarbonBackground
        else -> Color(ColorHexUtils.parseHexToArgbLong(preferences.customBackgroundHex, 0xFF080B10))
    }

    val animatedBackground by animateColorAsState(
        targetValue = resolvedBaseBackground,
        animationSpec = colorTransitionSpec,
        label = "theme_bg"
    )

    val isLiquidGlass = preferences.appearanceMode == AppearanceMode.LIQUID_GLASS
    val glassAlpha = preferences.glassIntensity.surfaceAlpha
    val elevatedGlassAlpha = preferences.glassIntensity.elevatedAlpha

    val colorScheme = if (useLightTheme) {
        val baseSurface = if (isLiquidGlass) {
            DaylightSurface.copy(alpha = elevatedGlassAlpha)
        } else {
            DaylightSurface
        }
        val lowContainer = if (isLiquidGlass) {
            blendColors(animatedPrimary, DaylightSurface, 0.05f).copy(alpha = glassAlpha)
        } else {
            DaylightElevated
        }
        val midContainer = if (isLiquidGlass) {
            blendColors(animatedPrimary, DaylightElevated, 0.08f).copy(alpha = elevatedGlassAlpha)
        } else {
            DaylightCard
        }

        lightColorScheme(
            primary = animatedPrimary,
            onPrimary = contrastingContentColor(animatedPrimary),
            primaryContainer = blendColors(animatedPrimary, DaylightSurface, 0.14f),
            onPrimaryContainer = TextPrimaryLight,
            secondary = animatedSecondary,
            onSecondary = contrastingContentColor(animatedSecondary),
            secondaryContainer = blendColors(animatedSecondary, DaylightSurface, 0.14f),
            onSecondaryContainer = TextPrimaryLight,
            tertiary = blendColors(animatedPrimary, animatedSecondary, 0.5f),
            onTertiary = contrastingContentColor(animatedPrimary),
            background = animatedBackground,
            onBackground = TextPrimaryLight,
            surface = baseSurface,
            onSurface = TextPrimaryLight,
            surfaceVariant = midContainer,
            onSurfaceVariant = TextSecondaryLight,
            surfaceContainerLowest = animatedBackground,
            surfaceContainerLow = lowContainer,
            surfaceContainer = midContainer,
            surfaceContainerHigh = blendColors(animatedPrimary, DaylightCard, 0.10f),
            surfaceContainerHighest = blendColors(animatedPrimary, DaylightCard, 0.16f),
            outline = if (isLiquidGlass) {
                animatedPrimary.copy(alpha = preferences.glassIntensity.borderHighlightAlpha)
            } else {
                DaylightBorder
            },
            outlineVariant = DaylightBorder.copy(alpha = 0.6f),
            error = CoralError,
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    } else {
        val darkCardBase = if (preferences.backgroundStyle == BackgroundStyle.PURE_BLACK) {
            CarbonSurface
        } else {
            blendColors(animatedBackground, CarbonCard, 0.45f)
        }
        val darkElevatedBase = if (preferences.backgroundStyle == BackgroundStyle.PURE_BLACK) {
            CarbonElevated
        } else {
            blendColors(animatedBackground, CarbonCard, 0.25f)
        }

        val lowContainer = if (isLiquidGlass) {
            blendColors(animatedPrimary, CarbonElevated, 0.08f).copy(alpha = glassAlpha)
        } else {
            darkCardBase
        }
        val midContainer = if (isLiquidGlass) {
            blendColors(animatedPrimary, CarbonCard, 0.11f).copy(alpha = elevatedGlassAlpha)
        } else {
            darkElevatedBase
        }
        val highContainer = if (isLiquidGlass) {
            blendColors(animatedPrimary, Color(0xFF1E2430), 0.14f).copy(
                alpha = (elevatedGlassAlpha + 0.08f).coerceAtMost(0.95f)
            )
        } else {
            CarbonCard
        }

        darkColorScheme(
            primary = animatedPrimary,
            onPrimary = contrastingContentColor(animatedPrimary),
            primaryContainer = blendColors(animatedPrimary, CarbonElevated, 0.18f),
            onPrimaryContainer = blendColors(animatedPrimary, Color.White, 0.35f),
            secondary = animatedSecondary,
            onSecondary = contrastingContentColor(animatedSecondary),
            secondaryContainer = blendColors(animatedSecondary, CarbonElevated, 0.18f),
            onSecondaryContainer = blendColors(animatedSecondary, Color.White, 0.35f),
            tertiary = blendColors(animatedPrimary, animatedSecondary, 0.45f),
            onTertiary = contrastingContentColor(animatedPrimary),
            background = animatedBackground,
            onBackground = TextPrimary,
            surface = animatedBackground,
            onSurface = TextPrimary,
            surfaceVariant = highContainer,
            onSurfaceVariant = TextSecondary,
            surfaceContainerLowest = animatedBackground,
            surfaceContainerLow = lowContainer,
            surfaceContainer = midContainer,
            surfaceContainerHigh = highContainer,
            surfaceContainerHighest = blendColors(animatedPrimary, Color(0xFF222834), 0.16f),
            outline = if (isLiquidGlass) {
                blendColors(animatedPrimary, Color.White, 0.35f).copy(
                    alpha = preferences.glassIntensity.borderHighlightAlpha
                )
            } else {
                CarbonBorder
            },
            outlineVariant = CarbonBorder.copy(alpha = 0.65f),
            error = CoralError,
            onError = OnCoralError,
            errorContainer = CoralErrorContainer,
            onErrorContainer = Color(0xFFFFDADE)
        )
    }

    val themeConfig = PulseFetchThemeConfig(
        appearanceMode = preferences.appearanceMode,
        accentPreset = preferences.accentPreset,
        accentColor = animatedPrimary,
        secondaryAccentColor = animatedSecondary,
        glassIntensity = preferences.glassIntensity,
        backgroundStyle = preferences.backgroundStyle,
        themePreference = preferences.themePreference,
        isLightTheme = useLightTheme
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = useLightTheme
                    isAppearanceLightNavigationBars = useLightTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalPulseFetchTheme provides themeConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
