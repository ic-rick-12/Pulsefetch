package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentPreset
import com.example.data.model.AppearanceMode
import com.example.data.model.BackgroundStyle
import com.example.data.model.ColorHexUtils
import com.example.data.model.GlassIntensity
import com.example.data.model.ThemePreference
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.contrastingContentColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceCustomizationSection(
    preferences: UserPreferences,
    onSelectAppearanceMode: (AppearanceMode) -> Unit,
    onSelectGlassIntensity: (GlassIntensity) -> Unit,
    onSelectAccentPreset: (AccentPreset) -> Unit,
    onApplyCustomAccentHex: (String) -> Unit,
    onSelectBackgroundStyle: (BackgroundStyle) -> Unit,
    onApplyCustomBackgroundHex: (String) -> Unit,
    onSelectThemePreference: (ThemePreference) -> Unit,
    onResetAppearance: () -> Unit
) {
    var showCustomColorStudio by rememberSaveable {
        mutableStateOf(preferences.accentPreset == AccentPreset.CUSTOM)
    }
    var customHexInput by rememberSaveable(preferences.customAccentHex) {
        mutableStateOf(preferences.customAccentHex)
    }
    var pickerHue by rememberSaveable { mutableFloatStateOf(190f) }
    var pickerSaturation by rememberSaveable { mutableFloatStateOf(0.95f) }
    var pickerValue by rememberSaveable { mutableFloatStateOf(1.0f) }

    var customBgHexInput by rememberSaveable(preferences.customBackgroundHex) {
        mutableStateOf(preferences.customBackgroundHex)
    }

    AdaptiveGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("appearance_customization_section"),
        cornerRadius = 24.dp,
        accentBorderTint = MaterialTheme.colorScheme.primary,
        elevated = true
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Appearance & Theme Studio",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${preferences.appearanceMode.label} • ${preferences.accentPreset.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        showCustomColorStudio = false
                        pickerHue = 190f
                        pickerSaturation = 0.95f
                        pickerValue = 1.0f
                        customHexInput = "#F8FAFC"
                        customBgHexInput = "#080B10"
                        onResetAppearance()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("reset_appearance_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = "Reset Appearance",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // 1. Two Appearance Modes: AMOLED Mode vs Liquid Glass Mode
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "APPEARANCE MODE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppearanceMode.entries.forEach { mode ->
                        val isSelected = preferences.appearanceMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectAppearanceMode(mode) }
                                .testTag("appearance_mode_${mode.name.lowercase()}"),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (mode == AppearanceMode.AMOLED) {
                                            Icons.Filled.DarkMode
                                        } else {
                                            Icons.Filled.BlurOn
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // 2. Adjustable Glass Intensity (Low / Medium / High)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIQUID GLASS INTENSITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = preferences.glassIntensity.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassIntensity.entries.forEach { intensity ->
                        val isSelected = preferences.glassIntensity == intensity
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectGlassIntensity(intensity) }
                                .testTag("glass_intensity_${intensity.name.lowercase()}"),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = intensity.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 3. Accent Colour Selector (12 Presets + Custom HEX / Color Picker)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ColorLens,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ACCENT COLOUR (12 PRESETS + CUSTOM)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (preferences.accentPreset == AccentPreset.CUSTOM) {
                            "Custom (${preferences.customAccentHex})"
                        } else {
                            preferences.accentPreset.label
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccentPreset.standardPresets.forEach { preset ->
                        val isSelected = preferences.accentPreset == preset
                        val swatchColor = Color(preset.primaryColorLong)

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) swatchColor else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    onSelectAccentPreset(preset)
                                }
                                .testTag("accent_preset_${preset.name.lowercase()}"),
                            color = if (isSelected) {
                                swatchColor.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = contrastingContentColor(swatchColor),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) swatchColor else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Custom Color Studio Toggle Chip
                    val isCustomSelected = preferences.accentPreset == AccentPreset.CUSTOM
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(
                                width = if (isCustomSelected || showCustomColorStudio) 1.5.dp else 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                showCustomColorStudio = !showCustomColorStudio
                            }
                            .testTag("toggle_custom_accent_studio"),
                        color = if (isCustomSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Custom HEX / Picker",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Expandable Custom Color Picker + HEX Input + Live Preview
                AnimatedVisibility(
                    visible = showCustomColorStudio,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val candidateHex = ColorHexUtils.normalizeHexOrNull(customHexInput)
                        ?: preferences.customAccentHex
                    val candidateColor = Color(ColorHexUtils.parseHexToArgbLong(candidateHex, 0xFF00E5FF))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, candidateColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                            .testTag("custom_color_studio_panel"),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Interactive Colour Picker & Live Preview",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Hue Rainbow Gradient Indicator + Slider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFFFF1744),
                                                Color(0xFFFF9100),
                                                Color(0xFFC6FF00),
                                                Color(0xFF00E676),
                                                Color(0xFF00E5FF),
                                                Color(0xFF2979FF),
                                                Color(0xFFAA00FF),
                                                Color(0xFFF50057)
                                            )
                                        )
                                    )
                            )

                            Slider(
                                value = pickerHue,
                                onValueChange = { newHue ->
                                    pickerHue = newHue
                                    customHexInput = ColorHexUtils.hsvToHex(pickerHue, pickerSaturation, pickerValue)
                                },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = candidateColor,
                                    activeTrackColor = candidateColor
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Saturation",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Slider(
                                        value = pickerSaturation,
                                        onValueChange = { s ->
                                            pickerSaturation = s
                                            customHexInput = ColorHexUtils.hsvToHex(pickerHue, pickerSaturation, pickerValue)
                                        },
                                        valueRange = 0.15f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = candidateColor,
                                            activeTrackColor = candidateColor
                                        )
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Brightness",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Slider(
                                        value = pickerValue,
                                        onValueChange = { v ->
                                            pickerValue = v
                                            customHexInput = ColorHexUtils.hsvToHex(pickerHue, pickerSaturation, pickerValue)
                                        },
                                        valueRange = 0.35f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = candidateColor,
                                            activeTrackColor = candidateColor
                                        )
                                    )
                                }
                            }

                            // Live Preview Bar
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, candidateColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                color = candidateColor.copy(alpha = 0.12f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(candidateColor)
                                            )
                                            Text(
                                                text = "Live Preview ($candidateHex)",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Surface(
                                            color = candidateColor,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "1080P • 60FPS",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = contrastingContentColor(candidateColor),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { 0.72f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = candidateColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                }
                            }

                            // HEX Input + Apply & Reset Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customHexInput,
                                    onValueChange = { customHexInput = it },
                                    singleLine = true,
                                    label = { Text("Custom HEX") },
                                    placeholder = { Text("#00E5FF") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("custom_accent_hex_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = candidateColor,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                Button(
                                    onClick = { onApplyCustomAccentHex(customHexInput) },
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("apply_custom_accent_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = candidateColor,
                                        contentColor = contrastingContentColor(candidateColor)
                                    )
                                ) {
                                    Text(
                                        text = "Apply",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        pickerHue = 190f
                                        pickerSaturation = 0.95f
                                        pickerValue = 1.0f
                                        customHexInput = "#F8FAFC"
                                        onSelectAccentPreset(AccentPreset.WHITE_SILVER)
                                    },
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("reset_custom_accent_button"),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Reset",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 4. Background Selector: Pure Black / Dark / Custom
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "BACKGROUND TONE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BackgroundStyle.entries.forEach { style ->
                        val isSelected = preferences.backgroundStyle == style
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectBackgroundStyle(style) }
                                .testTag("background_style_${style.name.lowercase()}"),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = style.defaultHex,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = preferences.backgroundStyle == BackgroundStyle.CUSTOM) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customBgHexInput,
                            onValueChange = { customBgHexInput = it },
                            singleLine = true,
                            label = { Text("Custom Background HEX") },
                            placeholder = { Text("#061018") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_background_hex_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = { onApplyCustomBackgroundHex(customBgHexInput) },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("apply_custom_background_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. Theme Preference: Dark / Light / System
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Contrast,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "THEME PREFERENCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemePreference.entries.forEach { pref ->
                        val isSelected = preferences.themePreference == pref
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectThemePreference(pref) }
                                .testTag("theme_preference_${pref.name.lowercase()}"),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pref.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
