package com.example.airmind.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.airmind.core.controls.OrientationSensorManager
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanLight
import com.example.ui.theme.DeepNeonBlue
import com.example.ui.theme.ElectricBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Earbuds3DSceneView(
    sensorManager: OrientationSensorManager,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    showRings: Boolean = true
) {
    // Collect device orientation angles (pitch, roll, yaw)
    val orientation by sensorManager.orientationAngles.collectAsState()

    // Manage sensor lifecycle
    DisposableEffect(sensorManager) {
        sensorManager.startListening()
        onDispose {
            sensorManager.stopListening()
        }
    }

    // Manual user drag rotation offsets
    var dragAngleX by remember { mutableFloatStateOf(0f) }
    var dragAngleY by remember { mutableFloatStateOf(0f) }

    // Ambient floating idle animation
    val transition = rememberInfiniteTransition(label = "earbuds_idle_motion")
    val idleFloat by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_float"
    )

    // Glowing energy ring pulse
    val ringPulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )

    // Effective 3D rotation angles combined from device orientation sensor + touch drag
    val effectivePitch = (orientation.pitch * 0.75f + dragAngleY).coerceIn(-45f, 45f)
    val effectiveRoll = (orientation.roll * 0.85f + dragAngleX).coerceIn(-60f, 60f)

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    dragAngleX = (dragAngleX + dragAmount.x * 0.4f).coerceIn(-90f, 90f)
                    dragAngleY = (dragAngleY + dragAmount.y * 0.4f).coerceIn(-45f, 45f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = this.size.width / 2f
            val centerY = (this.size.height / 2f) + idleFloat

            // 1. Perspective Drop Shadow reacting to pitch & roll
            val shadowOffsetX = (effectiveRoll / 45f) * 16f
            val shadowOffsetY = (effectivePitch / 45f) * 8f
            val shadowW = this.size.width * 0.65f
            val shadowH = 24f
            val groundY = this.size.height - 18f

            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xB3000000),
                        Color(0x3300E5FF),
                        Color.Transparent
                    ),
                    center = Offset(centerX + shadowOffsetX, groundY + shadowOffsetY),
                    radius = shadowW / 2f
                ),
                topLeft = Offset(centerX - shadowW / 2f + shadowOffsetX, groundY - shadowH / 2f + shadowOffsetY),
                size = Size(shadowW, shadowH)
            )

            // 2. Holographic Energy Rings reacting to roll and pitch
            if (showRings) {
                val ringRadius = (this.size.width * 0.42f) * ringPulse
                val ringCenterY = centerY + 30f + shadowOffsetY

                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.8f),
                            ElectricBlue.copy(alpha = 0.25f),
                            CyberCyanLight.copy(alpha = 0.85f),
                            Color.Transparent,
                            CyberCyan.copy(alpha = 0.8f)
                        ),
                        center = Offset(centerX, ringCenterY)
                    ),
                    topLeft = Offset(centerX - ringRadius, ringCenterY - (ringRadius * 0.32f)),
                    size = Size(ringRadius * 2f, ringRadius * 0.64f),
                    style = Stroke(width = 3f)
                )

                // Dotted inner accent ring
                drawOval(
                    color = CyberCyan.copy(alpha = 0.4f),
                    topLeft = Offset(centerX - ringRadius * 0.75f, ringCenterY - (ringRadius * 0.24f)),
                    size = Size(ringRadius * 1.5f, ringRadius * 0.48f),
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round)
                )
            }

            // 3. Render 3D Connected AirBuds Pair responding to Device Physical Orientation
            val rollRad = Math.toRadians(effectiveRoll.toDouble())
            val pitchRad = Math.toRadians(effectivePitch.toDouble())

            val leftZ = (sin(rollRad) * 40.0).toFloat()
            val rightZ = (-sin(rollRad) * 40.0).toFloat()

            // Depth sorting based on orientation tilt
            if (leftZ < rightZ) {
                drawConnected3DEarbud(
                    centerX = centerX - 36f + (cos(rollRad) * 14f).toFloat(),
                    centerY = centerY - 8f + (sin(pitchRad) * 18f).toFloat(),
                    zDepth = leftZ,
                    tiltAngle = -16f + effectiveRoll * 0.3f,
                    pitchAngle = effectivePitch,
                    isLeft = true
                )
                drawConnected3DEarbud(
                    centerX = centerX + 36f - (cos(rollRad) * 14f).toFloat(),
                    centerY = centerY + 12f - (sin(pitchRad) * 18f).toFloat(),
                    zDepth = rightZ,
                    tiltAngle = 18f + effectiveRoll * 0.3f,
                    pitchAngle = effectivePitch,
                    isLeft = false
                )
            } else {
                drawConnected3DEarbud(
                    centerX = centerX + 36f - (cos(rollRad) * 14f).toFloat(),
                    centerY = centerY + 12f - (sin(pitchRad) * 18f).toFloat(),
                    zDepth = rightZ,
                    tiltAngle = 18f + effectiveRoll * 0.3f,
                    pitchAngle = effectivePitch,
                    isLeft = false
                )
                drawConnected3DEarbud(
                    centerX = centerX - 36f + (cos(rollRad) * 14f).toFloat(),
                    centerY = centerY - 8f + (sin(pitchRad) * 18f).toFloat(),
                    zDepth = leftZ,
                    tiltAngle = -16f + effectiveRoll * 0.3f,
                    pitchAngle = effectivePitch,
                    isLeft = true
                )
            }
        }
    }
}

private fun DrawScope.drawConnected3DEarbud(
    centerX: Float,
    centerY: Float,
    zDepth: Float,
    tiltAngle: Float,
    pitchAngle: Float,
    isLeft: Boolean
) {
    val scale = (1.0f + (zDepth / 260f)).coerceIn(0.85f, 1.25f)
    val tiltRad = Math.toRadians(tiltAngle.toDouble()).toFloat()
    val cosT = cos(tiltRad)
    val sinT = sin(tiltRad)

    val earbudWidth = 34f * scale
    val stemLength = 68f * scale
    val bulbRadius = 26f * scale

    val stemStart = Offset(centerX, centerY)
    val stemEnd = Offset(
        centerX - (sinT * stemLength),
        centerY + (cosT * stemLength)
    )

    // Body Gradient with Metallic Cyber Highlights
    val ceramicBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E293B),
            Color(0xFF334155),
            Color(0xFF0F172A),
            Color(0xFF070B18)
        ),
        start = Offset(stemStart.x - 16f, stemStart.y - 16f),
        end = Offset(stemEnd.x + 16f, stemEnd.y + 16f)
    )

    // Stem Geometry
    val stemPath = Path().apply {
        moveTo(stemStart.x - earbudWidth * 0.26f, stemStart.y)
        lineTo(stemEnd.x - earbudWidth * 0.20f, stemEnd.y)
        quadraticTo(stemEnd.x, stemEnd.y + 6f * scale, stemEnd.x + earbudWidth * 0.20f, stemEnd.y)
        lineTo(stemStart.x + earbudWidth * 0.26f, stemStart.y)
        close()
    }
    drawPath(stemPath, brush = ceramicBrush)

    // Neon Stem Trim Highlight
    drawPath(
        stemPath,
        brush = Brush.verticalGradient(
            colors = listOf(CyberCyan.copy(alpha = 0.85f), ElectricBlue.copy(alpha = 0.2f), CyberCyan.copy(alpha = 0.6f))
        ),
        style = Stroke(width = 1.6f * scale)
    )

    // Capacitive Touch Sensor Groove
    val touchY = stemStart.y + (stemEnd.y - stemStart.y) * 0.45f
    val touchX = stemStart.x + (stemEnd.x - stemStart.x) * 0.45f
    drawRoundRect(
        color = CyberCyan.copy(alpha = 0.85f),
        topLeft = Offset(touchX - 2.5f * scale, touchY - 8f * scale),
        size = Size(5f * scale, 16f * scale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * scale)
    )

    // Chrome Cap & Gold Charging Pins
    val capHeight = 7f * scale
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFCBD5E1), Color(0xFF64748B)),
            start = Offset(stemEnd.x - 8f, stemEnd.y),
            end = Offset(stemEnd.x + 8f, stemEnd.y + capHeight)
        ),
        topLeft = Offset(stemEnd.x - earbudWidth * 0.18f, stemEnd.y),
        size = Size(earbudWidth * 0.36f, capHeight)
    )
    drawCircle(color = Color(0xFFFFD700), radius = 2f * scale, center = Offset(stemEnd.x - 4f * scale, stemEnd.y + capHeight * 0.6f))
    drawCircle(color = Color(0xFFFFD700), radius = 2f * scale, center = Offset(stemEnd.x + 4f * scale, stemEnd.y + capHeight * 0.6f))

    // Earbud Head Bulb
    val bulbCenter = Offset(
        centerX + (if (isLeft) -16f else 16f) * scale,
        centerY - 18f * scale + (pitchAngle * 0.15f)
    )

    // 3D Sphere Shading
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0A0F1D)),
            center = Offset(bulbCenter.x - 6f * scale, bulbCenter.y - 6f * scale),
            radius = bulbRadius * 1.1f
        ),
        radius = bulbRadius,
        center = bulbCenter
    )

    // Specular Glint Spot
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.8f), CyberCyanLight.copy(alpha = 0.2f), Color.Transparent),
            center = Offset(bulbCenter.x - 6f * scale, bulbCenter.y - 6f * scale),
            radius = bulbRadius * 0.45f
        ),
        radius = bulbRadius * 0.45f,
        center = Offset(bulbCenter.x - 6f * scale, bulbCenter.y - 6f * scale)
    )

    // Neon Cyan Rim
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(CyberCyan, ElectricBlue, Color.Transparent, CyberCyan),
            center = bulbCenter
        ),
        radius = bulbRadius,
        center = bulbCenter,
        style = Stroke(width = 2.2f * scale)
    )

    // Silicone In-ear Tip
    val tipCenter = Offset(bulbCenter.x + (if (isLeft) -12f else 12f) * scale, bulbCenter.y + 4f * scale)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617)),
            center = tipCenter,
            radius = 14f * scale
        ),
        topLeft = Offset(tipCenter.x - 8f * scale, tipCenter.y - 10f * scale),
        size = Size(16f * scale, 20f * scale)
    )

    // Sound Port Mesh Grille
    drawOval(
        color = Color(0xFF020408),
        topLeft = Offset(tipCenter.x - 4f * scale, tipCenter.y - 6f * scale),
        size = Size(8f * scale, 12f * scale)
    )
}
