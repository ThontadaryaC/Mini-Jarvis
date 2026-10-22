package com.minijarvis.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.minijarvis.core.JarvisStatus
import com.minijarvis.ui.theme.SiriBlue
import com.minijarvis.ui.theme.SiriCyan
import com.minijarvis.ui.theme.SiriIndigo
import com.minijarvis.ui.theme.SiriMagenta
import com.minijarvis.ui.theme.SiriOrange
import com.minijarvis.ui.theme.SiriPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisOrb(
    status: JarvisStatus,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    soundLevel: Float = 0f,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SiriOrbTransition")

    // Dynamic rotation angle
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    JarvisStatus.THINKING -> 1500
                    JarvisStatus.LISTENING -> 2200
                    JarvisStatus.SPEAKING -> 2000
                    else -> 6500
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "SiriRotation"
    )

    // Fluid breathing scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    JarvisStatus.LISTENING -> 800
                    JarvisStatus.SPEAKING -> 750
                    JarvisStatus.THINKING -> 600
                    else -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SiriPulse"
    )

    // Waveform phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SiriWavePhase"
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = (this.size.minDimension / 2f) * 0.85f * pulseScale

            // 1. Ambient Siri Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SiriMagenta.copy(alpha = 0.28f),
                        SiriPurple.copy(alpha = 0.22f),
                        SiriBlue.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.4f
                ),
                radius = radius * 1.4f,
                center = center
            )

            // 2. Swirling Iridescent Siri Blobs (Chromadynamic layers)
            val angleRad1 = Math.toRadians(rotation.toDouble()).toFloat()
            val angleRad2 = Math.toRadians((rotation + 120.0)).toFloat()
            val angleRad3 = Math.toRadians((rotation + 240.0)).toFloat()
            val offsetDist = radius * 0.35f

            val blob1Center = Offset(center.x + offsetDist * cos(angleRad1), center.y + offsetDist * sin(angleRad1))
            val blob2Center = Offset(center.x + offsetDist * cos(angleRad2), center.y + offsetDist * sin(angleRad2))
            val blob3Center = Offset(center.x + offsetDist * cos(angleRad3), center.y + offsetDist * sin(angleRad3))

            // Cyan/Blue lobe
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SiriCyan.copy(alpha = 0.85f),
                        SiriBlue.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    center = blob1Center,
                    radius = radius * 0.75f
                ),
                radius = radius * 0.75f,
                center = blob1Center,
                blendMode = BlendMode.Screen
            )

            // Magenta/Orange lobe
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SiriMagenta.copy(alpha = 0.85f),
                        SiriOrange.copy(alpha = 0.50f),
                        Color.Transparent
                    ),
                    center = blob2Center,
                    radius = radius * 0.75f
                ),
                radius = radius * 0.75f,
                center = blob2Center,
                blendMode = BlendMode.Screen
            )

            // Purple/Indigo lobe
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SiriPurple.copy(alpha = 0.90f),
                        SiriIndigo.copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = blob3Center,
                    radius = radius * 0.80f
                ),
                radius = radius * 0.80f,
                center = blob3Center,
                blendMode = BlendMode.Screen
            )

            // 3. Central Core Highlight (Pure Apple Glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        SiriCyan.copy(alpha = 0.75f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.38f
                ),
                radius = radius * 0.38f,
                center = center
            )

            // 4. Siri Animated Waveform (active when Listening or Speaking)
            if (status == JarvisStatus.LISTENING || status == JarvisStatus.SPEAKING) {
                drawSiriWaveforms(
                    center = center,
                    radius = radius,
                    wavePhase = wavePhase,
                    soundLevel = if (status == JarvisStatus.LISTENING) soundLevel.coerceAtLeast(0.35f) else 0.5f
                )
            }
        }
    }
}

private fun DrawScope.drawSiriWaveforms(
    center: Offset,
    radius: Float,
    wavePhase: Float,
    soundLevel: Float
) {
    val waveColors = listOf(
        SiriCyan.copy(alpha = 0.85f),
        SiriMagenta.copy(alpha = 0.80f),
        SiriPurple.copy(alpha = 0.75f),
        Color.White.copy(alpha = 0.90f)
    )

    val waveWidth = radius * 1.6f
    val startX = center.x - waveWidth / 2f
    val endX = center.x + waveWidth / 2f
    val steps = 40
    val stepX = waveWidth / steps

    waveColors.forEachIndexed { index, color ->
        val path = Path()
        val phaseOffset = wavePhase + (index * 0.8f)
        val amplitude = radius * 0.22f * soundLevel * (1f - (index * 0.15f))

        path.moveTo(startX, center.y)

        for (i in 0..steps) {
            val x = startX + (i * stepX)
            val normalizedX = (i.toFloat() / steps) // 0 to 1
            // Bell curve envelope so the wave smoothly tapers to 0 at edges
            val envelope = sin(normalizedX * PI).toFloat()
            val y = center.y + (sin((normalizedX * 4 * PI) + phaseOffset) * amplitude * envelope).toFloat()
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.5f - (index * 0.5f))
        )
    }
}
