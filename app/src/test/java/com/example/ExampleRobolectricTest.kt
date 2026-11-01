package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AccentPreset
import com.example.data.model.AppearanceMode
import com.example.data.model.BackgroundStyle
import com.example.data.model.ColorHexUtils
import com.example.data.model.DefaultQuality
import com.example.data.model.GlassIntensity
import com.example.data.model.ThemePreference
import com.example.data.preferences.UserPreferencesRepository
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PulseFetch", appName)

        val legalNotice = context.getString(R.string.legal_notice_body)
        assertTrue(legalNotice.contains("Only download content you own"))
    }

    @Test
    fun `navigation destinations exclude browser and formatting utilities work`() {
        val destinations = MainDestination.entries.map { it.route }
        assertEquals(listOf("extractor", "queue", "history", "settings"), destinations)
        assertFalse(destinations.contains("browser"))

        assertEquals("02:05", FormatUtils.formatDuration(125L))
        assertTrue(FormatUtils.extractFirstUrl("Watch this https://example.com/video.mp4 now") == "https://example.com/video.mp4")
    }

    @Test
    fun `appearance settings default to White Silver and persist all 12 accent presets and custom hex`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = UserPreferencesRepository(context)

        // Verify 12 selectable presets plus CUSTOM, with WHITE_SILVER first
        val selectablePresets = AccentPreset.standardPresets
        assertEquals(12, selectablePresets.size)
        assertEquals(AccentPreset.WHITE_SILVER, selectablePresets.first())

        // Verify default accent preset is WHITE_SILVER
        val initialPrefs = repo.preferencesFlow.first()
        assertEquals(AccentPreset.WHITE_SILVER, initialPrefs.accentPreset)

        // Switch to Liquid Glass mode with High intensity and Violet accent
        repo.setAppearanceMode(AppearanceMode.LIQUID_GLASS)
        repo.setGlassIntensity(GlassIntensity.HIGH)
        repo.setAccentPreset(AccentPreset.VIOLET)
        repo.setBackgroundStyle(BackgroundStyle.DARK)
        repo.setThemePreference(ThemePreference.DARK)
        repo.setDefaultQuality(DefaultQuality.P1080)

        val updatedPrefs = repo.preferencesFlow.first()
        assertEquals(AppearanceMode.LIQUID_GLASS, updatedPrefs.appearanceMode)
        assertEquals(GlassIntensity.HIGH, updatedPrefs.glassIntensity)
        assertEquals(AccentPreset.VIOLET, updatedPrefs.accentPreset)
        assertEquals(BackgroundStyle.DARK, updatedPrefs.backgroundStyle)
        assertEquals(ThemePreference.DARK, updatedPrefs.themePreference)
        assertEquals(DefaultQuality.P1080, updatedPrefs.defaultQuality)

        // Apply custom HEX color and verify AccentPreset switches to CUSTOM
        repo.setCustomAccentHex("FF3366")
        val customPrefs = repo.preferencesFlow.first()
        assertEquals(AccentPreset.CUSTOM, customPrefs.accentPreset)
        assertEquals("#FF3366", customPrefs.customAccentHex)

        // Reset appearance settings back to AMOLED + White/Silver defaults
        repo.resetAppearanceSettings()
        val resetPrefs = repo.preferencesFlow.first()
        assertEquals(AppearanceMode.AMOLED, resetPrefs.appearanceMode)
        assertEquals(AccentPreset.WHITE_SILVER, resetPrefs.accentPreset)
        assertEquals(GlassIntensity.MEDIUM, resetPrefs.glassIntensity)
        assertEquals(BackgroundStyle.PURE_BLACK, resetPrefs.backgroundStyle)
    }

    @Test
    fun `color hex utilities validate hex input and convert hsv to hex`() {
        assertEquals("#00E5FF", ColorHexUtils.normalizeHexOrNull("#00e5ff"))
        assertEquals("#FFAA00", ColorHexUtils.normalizeHexOrNull("ffaa00"))
        assertNull(ColorHexUtils.normalizeHexOrNull("#GGGGGG"))
        assertNull(ColorHexUtils.normalizeHexOrNull("12345"))

        val converted = ColorHexUtils.hsvToHex(186f, 1f, 1f)
        assertNotNull(ColorHexUtils.normalizeHexOrNull(converted))
    }
}
