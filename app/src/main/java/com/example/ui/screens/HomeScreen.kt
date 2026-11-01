package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ExtractedFormat
import com.example.data.model.ExtractedMediaInfo
import com.example.ui.components.AdaptiveGlassCard
import com.example.ui.components.AmoledHeroGraphic
import com.example.ui.components.PulseFetchBrandLogo
import com.example.ui.theme.LocalPulseFetchTheme
import com.example.ui.viewmodel.ExtractionUiState
import com.example.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    urlInput: String,
    clipboardCandidateUrl: String?,
    extractionState: ExtractionUiState,
    ytDlpVersion: String,
    onUrlChange: (String) -> Unit,
    onClearUrl: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onApplyClipboardUrl: () -> Unit,
    onDismissClipboardUrl: () -> Unit,
    onSelectFormat: (ExtractedFormat) -> Unit,
    onStartDownload: () -> Unit,
    onUpdateExtractorClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val themeConfig = LocalPulseFetchTheme.current
    var showAudioTab by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Minimal Hero Brand Card (Display ONLY PulseFetch without technical badges above title)
        item {
            AdaptiveGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 26.dp,
                accentBorderTint = MaterialTheme.colorScheme.primary,
                elevated = true
            ) {
                AmoledHeroGraphic(
                    modifier = Modifier.matchParentSize()
                )
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PulseFetchBrandLogo(size = 54.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "PulseFetch",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your downloads. Your control.",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Paste a link or share from any app. Select your preferred video resolution or audio format and save directly to your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Auto-Detected Clipboard Banner
        item {
            AnimatedVisibility(
                visible = !clipboardCandidateUrl.isNullOrBlank(),
                enter = fadeIn(tween(220)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing))
            ) {
                val candidate = clipboardCandidateUrl.orEmpty()
                AdaptiveGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clipboard_detected_banner"),
                    cornerRadius = 20.dp,
                    accentBorderTint = MaterialTheme.colorScheme.secondary
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = "Clipboard link detected",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Link Detected in Clipboard",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = candidate,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onApplyClipboardUrl,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("paste_clipboard_banner_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Paste & Extract", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onDismissClipboardUrl,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("dismiss_clipboard_banner_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }

        // 3. URL Input & Extraction Console Card
        item {
            AdaptiveGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input_field"),
                        label = { Text("Paste Video or Audio Link") },
                        placeholder = { Text("https://youtu.be/... or https://archive.org/...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = "URL icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearUrl,
                                    modifier = Modifier.testTag("clear_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear URL"
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val pasted = clipboardManager.getText()?.text
                                        if (!pasted.isNullOrBlank()) {
                                            onUrlChange(pasted.trim())
                                        }
                                    },
                                    modifier = Modifier.testTag("paste_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentPaste,
                                        contentDescription = "Paste from clipboard",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                onAnalyzeClick()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                                alpha = if (themeConfig.isLiquidGlass) 0.55f else 1f
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(
                                alpha = if (themeConfig.isLiquidGlass) 0.42f else 1f
                            )
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onAnalyzeClick()
                            },
                            enabled = extractionState !is ExtractionUiState.Analyzing,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("analyze_link_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (extractionState is ExtractionUiState.Analyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Inspecting Stream…", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetch Formats", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    onUrlChange(clip.trim())
                                    onAnalyzeClick()
                                }
                            },
                            label = { Text("Paste & Analyze") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("quick_paste_chip")
                        )

                        AssistChip(
                            onClick = onUpdateExtractorClick,
                            label = { Text("Update Extractor") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.SystemUpdateAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                leadingIconContentColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("quick_update_extractor_chip")
                        )
                    }
                }
            }
        }

        // 4. Dynamic Extraction Result States
        when (val state = extractionState) {
            is ExtractionUiState.Idle -> {
                item {
                    IdleWorkflowGuideCard()
                }
            }

            is ExtractionUiState.Analyzing -> {
                item {
                    AdaptiveGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyzing_progress_card"),
                        cornerRadius = 22.dp,
                        accentBorderTint = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(34.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Fetching Available Formats…",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Resolving adaptive 1080p/4K video & audio streams for ${state.url}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            is ExtractionUiState.ProtectedBlocked -> {
                item {
                    ProtectedMediaNoticeCard(message = state.message)
                }
            }

            is ExtractionUiState.Error -> {
                item {
                    ExtractionErrorCard(
                        message = state.message,
                        onRetry = onAnalyzeClick,
                        onUpdateExtractor = onUpdateExtractorClick
                    )
                }
            }

            is ExtractionUiState.Ready -> {
                item {
                    ExtractedMediaHeaderCard(
                        mediaInfo = state.mediaInfo,
                        showAudioTab = showAudioTab,
                        onToggleTab = { isAudio ->
                            showAudioTab = isAudio
                            val defaultInTab = if (isAudio) {
                                state.mediaInfo.audioFormats.firstOrNull()
                            } else {
                                state.mediaInfo.videoFormats.firstOrNull()
                            }
                            if (defaultInTab != null) {
                                onSelectFormat(defaultInTab)
                            }
                        }
                    )
                }

                val activeList = if (showAudioTab) {
                    state.mediaInfo.audioFormats
                } else {
                    state.mediaInfo.videoFormats
                }

                items(activeList, key = { "${it.formatId}_${it.ext}_${it.isAudioOnly}" }) { format ->
                    FormatSelectionRowCard(
                        format = format,
                        isSelected = state.selectedFormat.formatId == format.formatId &&
                            state.selectedFormat.ext == format.ext &&
                            state.selectedFormat.isAudioOnly == format.isAudioOnly,
                        onSelect = { onSelectFormat(format) }
                    )
                }

                item {
                    Button(
                        onClick = onStartDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("start_download_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Download ${state.selectedFormat.label} (${state.selectedFormat.ext.uppercase()})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Subtle Developer Branding at Bottom of Home Screen: "BUILT BY RICK"
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp)
                    .testTag("home_built_by_rick_credit"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "BUILT BY RICK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.0.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
                )
            }
        }
    }
}

@Composable
private fun ExtractedMediaHeaderCard(
    mediaInfo: ExtractedMediaInfo,
    showAudioTab: Boolean,
    onToggleTab: (Boolean) -> Unit
) {
    AdaptiveGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("extracted_media_header_card"),
        cornerRadius = 24.dp,
        accentBorderTint = MaterialTheme.colorScheme.primary,
        elevated = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                if (!mediaInfo.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = mediaInfo.thumbnailUrl,
                        contentDescription = mediaInfo.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                if (mediaInfo.durationSeconds > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatDuration(mediaInfo.durationSeconds),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = mediaInfo.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mediaInfo.uploader,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = !showAudioTab,
                    onClick = { onToggleTab(false) },
                    label = { Text("Video (${mediaInfo.videoFormats.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("video_formats_tab")
                )

                FilterChip(
                    selected = showAudioTab,
                    onClick = { onToggleTab(true) },
                    label = { Text("Audio MP3/M4A (${mediaInfo.audioFormats.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.secondary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_formats_tab")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FormatSelectionRowCard(
    format: ExtractedFormat,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(180),
        label = "format_border_color"
    )

    AdaptiveGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("format_item_${format.formatId}_${format.ext}"),
        cornerRadius = 18.dp,
        accentBorderTint = if (isSelected) borderColor else null,
        elevated = isSelected
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected format" else "Select format",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = format.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = format.ext.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val codecDetails = if (format.isAudioOnly) {
                    "Audio Stream • ${if (format.bitrateKbps > 0) "${format.bitrateKbps} kbps" else "High Bitrate"}"
                } else {
                    buildString {
                        append(format.resolution)
                        if (format.fps > 30) append(" • ${format.fps}fps")
                        if (format.vcodec.isNotBlank() && format.vcodec != "none") {
                            append(" • ${format.vcodec.substringBefore(".")}")
                        }
                    }
                }

                Text(
                    text = codecDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = FormatUtils.formatBytes(format.fileSizeBytes),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProtectedMediaNoticeCard(message: String) {
    AdaptiveGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("protected_media_notice_card"),
        cornerRadius = 20.dp,
        accentBorderTint = MaterialTheme.colorScheme.error
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Protected content",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Protected or Private Stream",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExtractionErrorCard(
    message: String,
    onRetry: () -> Unit,
    onUpdateExtractor: () -> Unit
) {
    AdaptiveGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("extraction_error_card"),
        cornerRadius = 20.dp,
        accentBorderTint = MaterialTheme.colorScheme.error
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = "Extraction error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Unable to Extract Link",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry")
                }
                OutlinedButton(
                    onClick = onUpdateExtractor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Update Extractor")
                }
            }
        }
    }
}

@Composable
private fun IdleWorkflowGuideCard() {
    AdaptiveGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Start Guide",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            WorkflowStepRow(
                badge = "01",
                title = "Share or Paste Any Public Link",
                subtitle = "Use Android's Share sheet from YouTube, Instagram, or Chrome and pick PulseFetch, or paste directly above."
            )
            WorkflowStepRow(
                badge = "02",
                title = "Pick Video Resolution or Audio Format",
                subtitle = "Choose 4K/1080p/720p MP4 video or extract high-bitrate MP3/M4A audio."
            )
            WorkflowStepRow(
                badge = "03",
                title = "Saved Cleanly via MediaStore",
                subtitle = "Files save directly to Movies/PulseFetch or Music/PulseFetch with zero invasive storage permissions."
            )
        }
    }
}

@Composable
private fun WorkflowStepRow(
    badge: String,
    title: String,
    subtitle: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
