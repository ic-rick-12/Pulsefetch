package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalPulseFetchTheme
import kotlinx.coroutines.delay

/**
 * Ambient background layer that powers iOS-inspired Liquid Glass frosted refraction while keeping
 * AMOLED mode pure (#000000) and zero-overhead. Includes performance-friendly fallback on API < 31.
 */
@Composable
fun PulseFetchRootBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val themeConfig = LocalPulseFetchTheme.current
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (themeConfig.isLiquidGlass) {
            val primary = themeConfig.accentColor
            val secondary = themeConfig.secondaryAccentColor
            val glowAlpha = themeConfig.glassIntensity.ambientGlowAlpha
            val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(themeConfig.glassIntensity.blurRadiusDp.dp)
            } else {
                Modifier
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
            ) {
                val w = size.width
                val h = size.height

                // Top-right primary liquid light pool
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = glowAlpha),
                            primary.copy(alpha = glowAlpha * 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.14f),
                        radius = w * 0.72f
                    ),
                    radius = w * 0.72f,
                    center = Offset(w * 0.85f, h * 0.14f)
                )

                // Center-left secondary liquid refraction
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondary.copy(alpha = glowAlpha * 0.82f),
                            secondary.copy(alpha = glowAlpha * 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.12f, h * 0.52f),
                        radius = w * 0.68f
                    ),
                    radius = w * 0.68f,
                    center = Offset(w * 0.12f, h * 0.52f)
                )

                // Bottom accent reflection pool
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = glowAlpha * 0.60f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.65f, h * 0.90f),
                        radius = w * 0.60f
                    ),
                    radius = w * 0.60f,
                    center = Offset(w * 0.65f, h * 0.90f)
                )
            }
        }

        content()
    }
}

/**
 * Adaptive card surface that renders a minimal carbon-bordered card in AMOLED mode
 * and a frosted translucent panel with soft glass borders & reflections in Liquid Glass mode.
 */
@Composable
fun AdaptiveGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    accentBorderTint: Color? = null,
    elevated: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val themeConfig = LocalPulseFetchTheme.current
    val shape = RoundedCornerShape(cornerRadius)
    val primary = accentBorderTint ?: MaterialTheme.colorScheme.primary

    if (themeConfig.isLiquidGlass) {
        val intensity = themeConfig.glassIntensity
        val baseSurface = if (elevated) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }

        val glassBrush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (themeConfig.isLightTheme) 0.48f else 0.10f),
                baseSurface,
                baseSurface.copy(alpha = (intensity.surfaceAlpha + 0.05f).coerceAtMost(0.94f))
            )
        )

        val borderBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (intensity.borderHighlightAlpha + 0.16f).coerceAtMost(0.80f)),
                primary.copy(alpha = intensity.borderHighlightAlpha),
                primary.copy(alpha = intensity.borderHighlightAlpha * 0.28f)
            )
        )

        Box(
            modifier = modifier
                .clip(shape)
                .background(brush = glassBrush, shape = shape)
                .border(width = 1.dp, brush = borderBrush, shape = shape),
            content = content
        )
    } else {
        val containerColor = if (elevated) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
        val borderColor = accentBorderTint?.copy(alpha = 0.28f)
            ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)

        Surface(
            modifier = modifier
                .clip(shape)
                .border(width = 1.dp, color = borderColor, shape = shape),
            shape = shape,
            color = containerColor
        ) {
            Box(content = content)
        }
    }
}

/**
 * Custom vector-drawn PulseFetch brand logo with dynamic White/Silver or selected accent colors.
 */
@Composable
fun PulseFetchBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.20f),
                        surface,
                        secondary.copy(alpha = 0.16f)
                    )
                )
            )
            .border(
                width = 1.25.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.50f),
                        primary.copy(alpha = 0.75f),
                        secondary.copy(alpha = 0.40f)
                    )
                ),
                shape = RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.18f)) {
            val w = this.size.width
            val h = this.size.height

            // Subtle inner frame
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(primary.copy(alpha = 0.25f), secondary.copy(alpha = 0.12f))
                ),
                cornerRadius = CornerRadius(w * 0.22f, w * 0.22f),
                style = Stroke(width = 1.25.dp.toPx())
            )

            // Pulse wave across upper half
            val pulsePath = Path().apply {
                moveTo(w * 0.10f, h * 0.42f)
                lineTo(w * 0.28f, h * 0.42f)
                lineTo(w * 0.38f, h * 0.18f)
                lineTo(w * 0.50f, h * 0.58f)
                lineTo(w * 0.62f, h * 0.30f)
                lineTo(w * 0.72f, h * 0.42f)
                lineTo(w * 0.90f, h * 0.42f)
            }
            drawPath(
                path = pulsePath,
                brush = Brush.horizontalGradient(listOf(secondary, primary)),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Fetch downward arrow chevron in lower half
            val arrowPath = Path().apply {
                moveTo(w * 0.50f, h * 0.46f)
                lineTo(w * 0.50f, h * 0.84f)
                moveTo(w * 0.32f, h * 0.68f)
                lineTo(w * 0.50f, h * 0.86f)
                lineTo(w * 0.68f, h * 0.68f)
            }
            drawPath(
                path = arrowPath,
                color = primary,
                style = Stroke(
                    width = 3.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Official Developer Branding Splash Screen:
 * - PulseFetch Logo
 * - App name: PulseFetch
 * - Tagline: "Your downloads. Your control."
 * - Subtle developer credit: "BUILT BY RICK"
 * - Adapts to AMOLED or Liquid Glass mode
 */
@Composable
fun PulseFetchSplashScreen(
    onFinished: () -> Unit
) {
    var startEntrance by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startEntrance) 1f else 0.92f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startEntrance) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    LaunchedEffect(Unit) {
        startEntrance = true
        delay(1150)
        onFinished()
    }

    val interactionSource = remember { MutableInteractionSource() }

    PulseFetchRootBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onFinished
            )
            .testTag("pulsefetch_splash_screen")
    ) {
        // Center Brand Identity
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .scale(scale)
                .alpha(alpha)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = haloAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
                PulseFetchBrandLogo(size = 84.dp)
            }

            Text(
                text = "PulseFetch",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Your downloads. Your control.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Subtle Bottom Developer Credit: "BUILT BY RICK"
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .alpha(alpha * 0.78f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "BUILT BY RICK",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
