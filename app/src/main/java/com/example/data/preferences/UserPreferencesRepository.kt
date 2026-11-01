package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentPreset
import com.example.data.model.AppearanceMode
import com.example.data.model.BackgroundStyle
import com.example.data.model.ColorHexUtils
import com.example.data.model.DefaultQuality
import com.example.data.model.GlassIntensity
import com.example.data.model.SearchEngine
import com.example.data.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pulsefetch_prefs")

data class UserPreferences(
    val hasAcceptedLegalNotice: Boolean = false,
    val defaultQuality: DefaultQuality = DefaultQuality.P1080,
    val wifiOnly: Boolean = false,
    val customSubfolder: String = "PulseFetch",
    val ytDlpVersion: String = "Bundled v2024.11+",
    val lastUpdatedTimestamp: Long = 0L,
    // Appearance & Personalization Preferences (Default Accent: WHITE / SILVER)
    val appearanceMode: AppearanceMode = AppearanceMode.AMOLED,
    val accentPreset: AccentPreset = AccentPreset.WHITE_SILVER,
    val customAccentHex: String = "#F8FAFC",
    val glassIntensity: GlassIntensity = GlassIntensity.MEDIUM,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.PURE_BLACK,
    val customBackgroundHex: String = "#080B10",
    val themePreference: ThemePreference = ThemePreference.DARK,
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ACCEPTED_LEGAL = booleanPreferencesKey("has_accepted_legal_notice")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CUSTOM_SUBFOLDER = stringPreferencesKey("custom_subfolder")
        val YTDLP_VERSION = stringPreferencesKey("ytdlp_version")

        // Personalization keys (v2 accent key ensures White/Silver default is respected)
        val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset_v2")
        val CUSTOM_ACCENT_HEX = stringPreferencesKey("custom_accent_hex_v2")
        val GLASS_INTENSITY = stringPreferencesKey("glass_intensity")
        val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        val CUSTOM_BACKGROUND_HEX = stringPreferencesKey("custom_background_hex")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val SEARCH_ENGINE = stringPreferencesKey("browser_search_engine")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val qualityName = prefs[Keys.DEFAULT_QUALITY] ?: DefaultQuality.P1080.name
        val quality = runCatching { DefaultQuality.valueOf(qualityName) }.getOrDefault(DefaultQuality.P1080)
        val rawFolder = prefs[Keys.CUSTOM_SUBFOLDER]?.trim().orEmpty()

        val appearanceMode = runCatching {
            AppearanceMode.valueOf(prefs[Keys.APPEARANCE_MODE] ?: AppearanceMode.AMOLED.name)
        }.getOrDefault(AppearanceMode.AMOLED)

        val accentPreset = runCatching {
            AccentPreset.valueOf(prefs[Keys.ACCENT_PRESET] ?: AccentPreset.WHITE_SILVER.name)
        }.getOrDefault(AccentPreset.WHITE_SILVER)

        val customAccentHex = ColorHexUtils.normalizeHexOrNull(
            prefs[Keys.CUSTOM_ACCENT_HEX].orEmpty()
        ) ?: "#F8FAFC"

        val glassIntensity = runCatching {
            GlassIntensity.valueOf(prefs[Keys.GLASS_INTENSITY] ?: GlassIntensity.MEDIUM.name)
        }.getOrDefault(GlassIntensity.MEDIUM)

        val backgroundStyle = runCatching {
            BackgroundStyle.valueOf(prefs[Keys.BACKGROUND_STYLE] ?: BackgroundStyle.PURE_BLACK.name)
        }.getOrDefault(BackgroundStyle.PURE_BLACK)

        val customBackgroundHex = ColorHexUtils.normalizeHexOrNull(
            prefs[Keys.CUSTOM_BACKGROUND_HEX].orEmpty()
        ) ?: "#080B10"

        val themePreference = runCatching {
            ThemePreference.valueOf(prefs[Keys.THEME_PREFERENCE] ?: ThemePreference.DARK.name)
        }.getOrDefault(ThemePreference.DARK)

        val searchEngine = runCatching {
            SearchEngine.valueOf(prefs[Keys.SEARCH_ENGINE] ?: SearchEngine.DUCKDUCKGO.name)
        }.getOrDefault(SearchEngine.DUCKDUCKGO)

        UserPreferences(
            hasAcceptedLegalNotice = prefs[Keys.ACCEPTED_LEGAL] ?: false,
            defaultQuality = quality,
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: false,
            customSubfolder = rawFolder.ifEmpty { "PulseFetch" },
            ytDlpVersion = prefs[Keys.YTDLP_VERSION] ?: "Bundled (yt-dlp)",
            appearanceMode = appearanceMode,
            accentPreset = accentPreset,
            customAccentHex = customAccentHex,
            glassIntensity = glassIntensity,
            backgroundStyle = backgroundStyle,
            customBackgroundHex = customBackgroundHex,
            themePreference = themePreference,
            searchEngine = searchEngine
        )
    }

    suspend fun getSnapshot(): UserPreferences = preferencesFlow.first()

    suspend fun acceptLegalNotice() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCEPTED_LEGAL] = true
        }
    }

    suspend fun setDefaultQuality(quality: DefaultQuality) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_QUALITY] = quality.name
        }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WIFI_ONLY] = enabled
        }
    }

    suspend fun setCustomSubfolder(subfolder: String) {
        val cleaned = subfolder.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim().ifEmpty { "PulseFetch" }
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_SUBFOLDER] = cleaned
        }
    }

    suspend fun setYtDlpVersion(version: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.YTDLP_VERSION] = version
        }
    }

    suspend fun setAppearanceMode(mode: AppearanceMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APPEARANCE_MODE] = mode.name
        }
    }

    suspend fun setAccentPreset(preset: AccentPreset) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCENT_PRESET] = preset.name
        }
    }

    suspend fun setCustomAccentHex(hexInput: String) {
        val normalized = ColorHexUtils.normalizeHexOrNull(hexInput) ?: return
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_ACCENT_HEX] = normalized
            prefs[Keys.ACCENT_PRESET] = AccentPreset.CUSTOM.name
        }
    }

    suspend fun setGlassIntensity(intensity: GlassIntensity) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GLASS_INTENSITY] = intensity.name
        }
    }

    suspend fun setBackgroundStyle(style: BackgroundStyle) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_STYLE] = style.name
        }
    }

    suspend fun setCustomBackgroundHex(hexInput: String) {
        val normalized = ColorHexUtils.normalizeHexOrNull(hexInput) ?: return
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_BACKGROUND_HEX] = normalized
            prefs[Keys.BACKGROUND_STYLE] = BackgroundStyle.CUSTOM.name
        }
    }

    suspend fun setThemePreference(themePreference: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_PREFERENCE] = themePreference.name
        }
    }

    suspend fun setSearchEngine(searchEngine: SearchEngine) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SEARCH_ENGINE] = searchEngine.name
        }
    }

    suspend fun resetAppearanceSettings() {
        context.dataStore.edit { prefs ->
            prefs[Keys.APPEARANCE_MODE] = AppearanceMode.AMOLED.name
            prefs[Keys.ACCENT_PRESET] = AccentPreset.WHITE_SILVER.name
            prefs[Keys.CUSTOM_ACCENT_HEX] = "#F8FAFC"
            prefs[Keys.GLASS_INTENSITY] = GlassIntensity.MEDIUM.name
            prefs[Keys.BACKGROUND_STYLE] = BackgroundStyle.PURE_BLACK.name
            prefs[Keys.CUSTOM_BACKGROUND_HEX] = "#080B10"
            prefs[Keys.THEME_PREFERENCE] = ThemePreference.DARK.name
        }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_QUALITY] = DefaultQuality.P1080.name
            prefs[Keys.WIFI_ONLY] = false
            prefs[Keys.CUSTOM_SUBFOLDER] = "PulseFetch"
            prefs[Keys.APPEARANCE_MODE] = AppearanceMode.AMOLED.name
            prefs[Keys.ACCENT_PRESET] = AccentPreset.WHITE_SILVER.name
            prefs[Keys.CUSTOM_ACCENT_HEX] = "#F8FAFC"
            prefs[Keys.GLASS_INTENSITY] = GlassIntensity.MEDIUM.name
            prefs[Keys.BACKGROUND_STYLE] = BackgroundStyle.PURE_BLACK.name
            prefs[Keys.CUSTOM_BACKGROUND_HEX] = "#080B10"
            prefs[Keys.THEME_PREFERENCE] = ThemePreference.DARK.name
        }
    }
}

