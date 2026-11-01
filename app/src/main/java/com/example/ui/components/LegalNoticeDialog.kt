package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

@Composable
fun LegalNoticeDialog(
    onAccept: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Non-dismissible until accepted */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("legal_notice_dialog"),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Gavel,
                            contentDescription = "Legal notice icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.legal_notice_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "First-Launch Compliance Agreement",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Only download content you own, have permission to download, or that is public domain / Creative Commons. You are responsible for following each site's terms and local law.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "• PulseFetch does not bypass DRM, paywalls, or private logins.\n• Zero ads, zero trackers, and zero analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("accept_legal_notice_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.legal_notice_accept),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AmoledHeroGraphic(
    modifier: Modifier = Modifier
) {
    val cyan = MaterialTheme.colorScheme.primary
    val teal = MaterialTheme.colorScheme.secondary
    val mint = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Deep radial cyan glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(cyan.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(w * 0.82f, h * 0.35f),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(w * 0.82f, h * 0.35f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(teal.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(w * 0.2f, h * 0.85f),
                radius = w * 0.45f
            ),
            radius = w * 0.45f,
            center = Offset(w * 0.2f, h * 0.85f)
        )

        // Flowing fiber-optic wave paths
        val wave1 = Path().apply {
            moveTo(0f, h * 0.75f)
            cubicTo(
                w * 0.3f, h * 0.45f,
                w * 0.65f, h * 0.95f,
                w, h * 0.3f
            )
        }
        drawPath(
            path = wave1,
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, cyan.copy(alpha = 0.45f), mint.copy(alpha = 0.7f))
            ),
            style = Stroke(width = 3.dp.toPx())
        )

        val wave2 = Path().apply {
            moveTo(0f, h * 0.9f)
            cubicTo(
                w * 0.35f, h * 0.65f,
                w * 0.7f, h * 0.2f,
                w, h * 0.6f
            )
        }
        drawPath(
            path = wave2,
            brush = Brush.horizontalGradient(
                listOf(teal.copy(alpha = 0.2f), teal.copy(alpha = 0.5f), Color.Transparent)
            ),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
