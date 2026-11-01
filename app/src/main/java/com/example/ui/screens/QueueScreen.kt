package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DownloadStatus
import com.example.ui.components.AdaptiveGlassCard
import com.example.util.FormatUtils

@Composable
fun QueueScreen(
    tasks: List<DownloadTaskEntity>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (DownloadTaskEntity) -> Unit,
    onClearFailedAndCancelled: () -> Unit,
    onBackToExtractor: () -> Unit
) {
    BackHandler(onBack = onBackToExtractor)

    val activeCount = tasks.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
    val pausedCount = tasks.count { it.status == DownloadStatus.PAUSED }
    val stoppedCount = tasks.count { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag("queue_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Download Queue",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (tasks.isEmpty()) {
                        "Foreground Service • WorkManager Resumable Queue"
                    } else {
                        "$activeCount active • $pausedCount paused • $stoppedCount stopped"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (stoppedCount > 0) {
                OutlinedButton(
                    onClick = onClearFailedAndCancelled,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("clear_failed_queue_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Stopped")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AdaptiveGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 26.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Text(
                            text = "No Active Downloads",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Paste or share a video or audio link on the Extractor tab to queue downloads.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
                modifier = Modifier.testTag("queue_list")
            ) {
                items(tasks, key = { it.id }) { task ->
                    QueueTaskCard(
                        task = task,
                        onPause = { onPause(task.id) },
                        onResume = { onResume(task.id) },
                        onCancel = { onCancel(task.id) },
                        onRetry = { onRetry(task.id) },
                        onRemove = { onRemove(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueTaskCard(
    task: DownloadTaskEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    val statusColor = when (task.status) {
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        DownloadStatus.QUEUED -> MaterialTheme.colorScheme.secondary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (task.progressPercent.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 320),
        label = "download_progress"
    )

    val sizeReadout = when {
        task.totalBytes > 0L && task.downloadedBytes > 0L -> {
            "${FormatUtils.formatBytes(task.downloadedBytes)} / ${FormatUtils.formatBytes(task.totalBytes)}"
        }
        task.totalBytes > 0L -> FormatUtils.formatBytes(task.totalBytes)
        task.downloadedBytes > 0L -> FormatUtils.formatBytes(task.downloadedBytes)
        else -> "Calculating size…"
    }

    AdaptiveGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("queue_item_${task.id}"),
        cornerRadius = 22.dp,
        accentBorderTint = if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.FAILED) {
            statusColor
        } else {
            null
        },
        elevated = task.status == DownloadStatus.DOWNLOADING
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    contentAlignment = Alignment.Center
                ) {
                    if (!task.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = task.thumbnailUrl,
                            contentDescription = task.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (task.isAudioOnly) Icons.Filled.AudioFile else Icons.Filled.Movie,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = statusColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = task.status.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "${task.formatLabel} • ${task.ext.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Progress Bar + Percentage + Speed + File Size + ETA
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${task.progressPercent.coerceIn(0, 100)}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• $sizeReadout",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (task.speedBytesPerSec > 0L && task.status == DownloadStatus.DOWNLOADING) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = FormatUtils.formatSpeed(task.speedBytesPerSec),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (task.etaSeconds >= 0L && task.status == DownloadStatus.DOWNLOADING) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "ETA ${FormatUtils.formatEta(task.etaSeconds)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            if (!task.errorMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = task.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Action Buttons (Pause / Resume / Retry / Cancel / Remove)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (task.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                        OutlinedButton(
                            onClick = onPause,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("pause_task_button_${task.id}")
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause")
                        }

                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("cancel_task_button_${task.id}")
                        ) {
                            Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel")
                        }
                    }

                    DownloadStatus.PAUSED -> {
                        Button(
                            onClick = onResume,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("resume_task_button_${task.id}")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("cancel_paused_task_button_${task.id}")
                        ) {
                            Text("Cancel")
                        }
                    }

                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("retry_task_button_${task.id}")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Download", fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("remove_task_button_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remove from queue",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    DownloadStatus.COMPLETED -> Unit
                }
            }
        }
    }
}
