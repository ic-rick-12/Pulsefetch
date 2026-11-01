package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.AccentPreset
import com.example.data.model.AppearanceMode
import com.example.data.model.BackgroundStyle
import com.example.data.model.DefaultQuality
import com.example.data.model.GlassIntensity
import com.example.data.model.ThemePreference
import com.example.data.preferences.UserPreferences
import com.example.ui.components.AdaptiveGlassCard
import com.example.ui.components.AppearanceCustomizationSection
import com.example.ui.components.PulseFetchBrandLogo
import com.example.ui.viewmodel.ExtractorUpdateUiState

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    extractorUpdateState: ExtractorUpdateUiState,
    onSelectDefaultQuality: (DefaultQuality) -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit,
    onSaveSubfolder: (String) -> Unit,
    onUpdateExtractor: (useNightly: Boolean) -> Unit,
    onSelectAppearanceMode: (AppearanceMode) -> Unit = {},
    onSelectGlassIntensity: (GlassIntensity) -> Unit = {},
    onSelectAccentPreset: (AccentPreset) -> Unit = {},
    onApplyCustomAccentHex: (String) -> Unit = {},
    onSelectBackgroundStyle: (BackgroundStyle) -> Unit = {},
    onApplyCustomBackgroundHex: (String) -> Unit = {},
    onSelectThemePreference: (ThemePreference) -> Unit = {},
    onResetAppearance: () -> Unit = {},
    onResetAllSettings: () -> Unit = {},
    onRestartApp: () -> Unit = {},
    onBackToExtractor: () -> Unit
) {
    var showAboutPage by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (showAboutPage) {
            showAboutPage = false
        } else {
            onBackToExtractor()
        }
    }

    AnimatedContent(
        targetState = showAboutPage,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
        },
        label = "settings_about_transition"
    ) { isAboutOpen ->
        if (isAboutOpen) {
            AboutPulseFetchScreen(
                ytDlpVersion = preferences.ytDlpVersion,
                onBack = { showAboutPage = false }
            )
        } else {
            var folderInput by rememberSaveable(preferences.customSubfolder) {
                mutableStateOf(preferences.customSubfolder)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("settings_screen"),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Text(
                        text = "Settings & Personalization",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize AMOLED & Liquid Glass appearance, accent colors, yt-dlp engine updates, and MediaStore folders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 1. Dedicated Appearance & Personalization Studio
                item {
                    AppearanceCustomizationSection(
                        preferences = preferences,
                        onSelectAppearanceMode = onSelectAppearanceMode,
                        onSelectGlassIntensity = onSelectGlassIntensity,
                        onSelectAccentPreset = onSelectAccentPreset,
                        onApplyCustomAccentHex = onApplyCustomAccentHex,
                        onSelectBackgroundStyle = onSelectBackgroundStyle,
                        onApplyCustomBackgroundHex = onApplyCustomBackgroundHex,
                        onSelectThemePreference = onSelectThemePreference,
                        onResetAppearance = onResetAppearance
                    )
                }

                // 2. Update Extractor Card (youtubedl-android self-updater)
                item {
                    AdaptiveGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp,
                        accentBorderTint = MaterialTheme.colorScheme.primary
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SystemUpdateAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "yt-dlp Extractor Engine",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Active Version: ${preferences.ytDlpVersion}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text(
                                text = "Sites update their video players frequently. Pull the latest yt-dlp extractor binary directly onto your phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (extractorUpdateState is ExtractorUpdateUiState.Completed) {
                                Surface(
                                    color = if (extractorUpdateState.isError) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = extractorUpdateState.message,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (extractorUpdateState.isError) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onUpdateExtractor(false) },
                                    enabled = extractorUpdateState !is ExtractorUpdateUiState.Updating,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("update_extractor_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    if (extractorUpdateState is ExtractorUpdateUiState.Updating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Updating…", fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Update Extractor", fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onUpdateExtractor(true) },
                                    enabled = extractorUpdateState !is ExtractorUpdateUiState.Updating,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("update_extractor_nightly_button")
                                ) {
                                    Text("Nightly")
                                }
                            }
                        }
                    }
                }

                // 3. Network & Storage Folder Card
                item {
                    AdaptiveGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Wifi,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Column {
                                        Text(
                                            text = "Wi-Fi-Only Downloads",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Pause WorkManager queue on metered mobile networks",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = preferences.wifiOnly,
                                    onCheckedChange = onToggleWifiOnly,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("wifi_only_switch")
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "MediaStore Download Folder",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "Videos -> Movies/$folderInput • Audio -> Music/$folderInput",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = folderInput,
                                        onValueChange = { folderInput = it },
                                        singleLine = true,
                                        label = { Text("Subfolder Name") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("subfolder_input_field"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )

                                    Button(
                                        onClick = { onSaveSubfolder(folderInput) },
                                        modifier = Modifier
                                            .height(54.dp)
                                            .testTag("save_subfolder_button"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Default Quality Selection (including 1080p Full HD, Best/4K, 720p, 480p, Audio MP3/M4A)
                item {
                    AdaptiveGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.HighQuality,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Default Preferred Quality",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DefaultQuality.entries.forEach { quality ->
                                val isSelected = preferences.defaultQuality == quality
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            if (isSelected) 1.5.dp else 1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { onSelectDefaultQuality(quality) }
                                        .testTag("quality_option_${quality.name}"),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLowest
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) {
                                                Icons.Filled.CheckCircle
                                            } else {
                                                Icons.Filled.RadioButtonUnchecked
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = quality.label,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = quality.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. System & Reset Controls Card (Restart App & Reset All Settings)
                item {
                    AdaptiveGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 22.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RestartAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "System & Reset Controls",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Restart the PulseFetch session & extractor engine or restore defaults",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onRestartApp,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("restart_app_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RestartAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Restart App",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        folderInput = "PulseFetch"
                                        onResetAllSettings()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("reset_all_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Restore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Reset All",
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Dedicated Settings -> About PulseFetch Card (Official Developer Branding - Rick)
                item {
                    AdaptiveGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutPage = true }
                            .testTag("open_about_pulsefetch_card"),
                        cornerRadius = 22.dp,
                        accentBorderTint = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            PulseFetchBrandLogo(size = 50.dp)

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "About PulseFetch",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "BUILT BY RICK • v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "© 2026 Rick. All rights reserved.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Open About PulseFetch",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
