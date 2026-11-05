package com.example.airmind.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanLight
import com.example.ui.theme.DeepNeonBlue
import com.example.ui.theme.ElectricBlue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Vector Math Point in space
 */
private data class Vec3D(val x: Float, val y: Float, val z: Float) {
    fun rotateY(angleRad: Double): Vec3D {
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        return Vec3D(
            x = x * cosA - z * sinA,
            y = y,
            z = x * sinA + z * cosA
        )
    }

    fun rotateX(angleRad: Double): Vec3D {
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        return Vec3D(
            x = x,
            y = y * cosA - z * sinA,
            z = y * sinA + z * cosA
        )
    }

    fun project(centerX: Float, centerY: Float, focalLength: Float = 500f, cameraDist: Float = 600f): Offset {
        val scale = focalLength / (cameraDist + z)
        return Offset(
            x = centerX + x * scale,
            y = centerY + y * scale
        )
    }
}

data class OrbitParticle(
    val initialAngle: Float,
    val speed: Float,
    val radiusX: Float,
    val radiusY: Float,
    val yOffset: Float,
    val baseSize: Float,
    val color: Color
)

@Composable
fun Earbuds3DCanvas(
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    showParticles: Boolean = true,
    showRings: Boolean = true,
    autoRotate: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "3d_earbuds_anim")

    // Full 360 degree rotation in 3D space
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Floating vertical oscillation
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // Energy ring glow pulsing
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )

    // Dynamic light reflection phase
    val lightSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "light_sweep"
    )

    // 3D Orbiting Energy Particles
    val particles = listOf(
        OrbitParticle(initialAngle = 0f, speed = 1.4f, radiusX = 130f, radiusY = 45f, yOffset = -30f, baseSize = 4.5f, color = CyberCyan),
        OrbitParticle(initialAngle = 70f, speed = 0.9f, radiusX = 150f, radiusY = 50f, yOffset = 15f, baseSize = 5.0f, color = CyberCyanLight),
        OrbitParticle(initialAngle = 140f, speed = 1.2f, radiusX = 120f, radiusY = 40f, yOffset = -10f, baseSize = 3.5f, color = ElectricBlue),
        OrbitParticle(initialAngle = 210f, speed = 1.1f, radiusX = 140f, radiusY = 48f, yOffset = 25f, baseSize = 4.0f, color = CyberCyan),
        OrbitParticle(initialAngle = 280f, speed = 0.8f, radiusX = 160f, radiusY = 55f, yOffset = -20f, baseSize = 4.8f, color = DeepNeonBlue),
        OrbitParticle(initialAngle = 330f, speed = 1.5f, radiusX = 115f, radiusY = 38f, yOffset = 10f, baseSize = 3.2f, color = CyberCyanLight)
    )

    Canvas(modifier = modifier.size(size)) {
        val centerX = this.size.width / 2f
        val centerY = (this.size.height / 2f) + floatOffset

        // 1. Realistic 3D Ground Drop Shadow
        val shadowScale = (1f - (floatOffset / 120f)).coerceIn(0.7f, 1.3f)
        val shadowW = (this.size.width * 0.65f) * shadowScale
        val shadowH = 26f * shadowScale
        val groundY = this.size.height - 20f

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xB3000000),
                    Color(0x4000E5FF),
                    Color(0x100080FF),
                    Color.Transparent
                ),
                center = Offset(centerX, groundY),
                radius = shadowW / 2f
            ),
            topLeft = Offset(centerX - shadowW / 2f, groundY - shadowH / 2f),
            size = Size(shadowW, shadowH)
        )

        // 2. Glowing 3D Cybernetic Energy Rings (Tilted in 3D space)
        if (showRings) {
            val ringRadius = (this.size.width * 0.44f) * ringPulse
            val ringTiltY = centerY + 36f

            // Outer Glowing Energy Ring
            drawOval(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        CyberCyan.copy(alpha = 0.85f),
                        ElectricBlue.copy(alpha = 0.3f),
                        CyberCyanLight.copy(alpha = 0.9f),
                        Color.Transparent,
                        CyberCyan.copy(alpha = 0.85f)
                    ),
                    center = Offset(centerX, ringTiltY)
                ),
                topLeft = Offset(centerX - ringRadius, ringTiltY - (ringRadius * 0.32f)),
                size = Size(ringRadius * 2, ringRadius * 0.64f),
                style = Stroke(width = 3.5f)
            )

            // Inner Secondary Concentric Ring
            val innerR = ringRadius * 0.76f
            drawOval(
                color = CyberCyan.copy(alpha = 0.45f),
                topLeft = Offset(centerX - innerR, ringTiltY - (innerR * 0.32f)),
                size = Size(innerR * 2, innerR * 0.64f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round)
            )

            // Dynamic Rotating Tick Marks on Energy Ring
            for (tick in 0 until 16) {
                val tickAngle = Math.toRadians((tick * 22.5 + rotationAngle * 0.5).toDouble())
                val tx = centerX + (cos(tickAngle) * (ringRadius * 0.88f)).toFloat()
                val ty = ringTiltY + (sin(tickAngle) * (ringRadius * 0.28f)).toFloat()
                drawCircle(
                    color = CyberCyanLight.copy(alpha = 0.6f),
                    radius = 1.8f,
                    center = Offset(tx, ty)
                )
            }
        }

        // 3. Render 3D Background Particles (z < 0: behind the earbuds)
        if (showParticles) {
            drawOrbitParticles(particles, centerX, centerY, rotationAngle, renderForeground = false)
        }

        // 4. Photorealistic 3D AirBuds Rendering with Perspective and Dynamic Specular Shading
        val radAngle = Math.toRadians((if (autoRotate) rotationAngle else 45f).toDouble())

        // Calculate Z depth of both earbuds to perform correct 3D depth sorting
        val leftEarbudZ = (sin(radAngle) * 35.0).toFloat()
        val rightEarbudZ = (-sin(radAngle) * 35.0).toFloat()

        if (leftEarbudZ < rightEarbudZ) {
            // Render Left Earbud first (further back), then Right Earbud
            drawRealistic3DEarbud(
                centerX = centerX - 38f + (cos(radAngle) * 16f).toFloat(),
                centerY = centerY - 6f,
                zDepth = leftEarbudZ,
                rotAngle = rotationAngle,
                isLeft = true,
                lightPhase = lightSweep
            )
            drawRealistic3DEarbud(
                centerX = centerX + 38f - (cos(radAngle) * 16f).toFloat(),
                centerY = centerY + 14f,
                zDepth = rightEarbudZ,
                rotAngle = rotationAngle + 180f,
                isLeft = false,
                lightPhase = (lightSweep + 0.5f) % 1f
            )
        } else {
            // Render Right Earbud first (further back), then Left Earbud
            drawRealistic3DEarbud(
                centerX = centerX + 38f - (cos(radAngle) * 16f).toFloat(),
                centerY = centerY + 14f,
                zDepth = rightEarbudZ,
                rotAngle = rotationAngle + 180f,
                isLeft = false,
                lightPhase = (lightSweep + 0.5f) % 1f
            )
            drawRealistic3DEarbud(
                centerX = centerX - 38f + (cos(radAngle) * 16f).toFloat(),
                centerY = centerY - 6f,
                zDepth = leftEarbudZ,
                rotAngle = rotationAngle,
                isLeft = true,
                lightPhase = lightSweep
            )
        }

        // 5. Render 3D Foreground Particles (z >= 0: in front of the earbuds)
        if (showParticles) {
            drawOrbitParticles(particles, centerX, centerY, rotationAngle, renderForeground = true)
        }
    }
}

private fun DrawScope.drawOrbitParticles(
    particles: List<OrbitParticle>,
    centerX: Float,
    centerY: Float,
    rotationAngle: Float,
    renderForeground: Boolean
) {
    val rad = Math.toRadians(rotationAngle.toDouble())
    particles.forEach { p ->
        val angle = rad * p.speed + Math.toRadians(p.initialAngle.toDouble())
        val z = sin(angle).toFloat()

        // Distinguish foreground vs background
        if ((renderForeground && z >= 0) || (!renderForeground && z < 0)) {
            val px = centerX + (cos(angle) * p.radiusX).toFloat()
            val py = (centerY + p.yOffset) + (z * p.radiusY).toFloat()

            // Perspective scale & alpha based on depth Z
            val depthFactor = ((z + 1f) / 2f).coerceIn(0.2f, 1.0f)
            val particleRadius = p.baseSize * (0.6f + depthFactor * 0.7f)
            val particleAlpha = (0.3f + depthFactor * 0.7f).coerceIn(0.2f, 1f)

            // Particle core
            drawCircle(
                color = p.color.copy(alpha = particleAlpha),
                radius = particleRadius,
                center = Offset(px, py)
            )

            // Subtle particle glow
            drawCircle(
                color = p.color.copy(alpha = particleAlpha * 0.35f),
                radius = particleRadius * 2.2f,
                center = Offset(px, py)
            )
        }
    }
}

/**
 * Draws a photorealistic 3D AirBud with realistic shading, metallic stems,
 * silicone in-ear tips, capacitive touch sensor, and micro-mesh ports.
 */
private fun DrawScope.drawRealistic3DEarbud(
    centerX: Float,
    centerY: Float,
    zDepth: Float,
    rotAngle: Float,
    isLeft: Boolean,
    lightPhase: Float
) {
    // 3D perspective scale factor based on distance
    val perspectiveScale = (1.0f + (zDepth / 280f)).coerceIn(0.82f, 1.22f)
    val tiltRad = Math.toRadians((if (isLeft) -16.0 else 18.0) + (sin(Math.toRadians(rotAngle.toDouble())) * 6.0)).toFloat()
    val cosT = cos(tiltRad)
    val sinT = sin(tiltRad)

    val earbudWidth = 36f * perspectiveScale
    val stemLength = 72f * perspectiveScale
    val bulbRadius = 28f * perspectiveScale

    val stemStart = Offset(centerX, centerY)
    val stemEnd = Offset(
        centerX - (sinT * stemLength),
        centerY + (cosT * stemLength)
    )

    // Photorealistic Sleek Metallic Ceramic Body Gradient
    val ceramicBodyBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF131B2E),
            Color(0xFF1E2D4A),
            Color(0xFF0F172A),
            Color(0xFF090E1A)
        ),
        start = Offset(stemStart.x - 20f, stemStart.y - 20f),
        end = Offset(stemEnd.x + 20f, stemEnd.y + 20f)
    )

    // 1. Draw Stem
    val stemPath = Path().apply {
        moveTo(stemStart.x - earbudWidth * 0.28f, stemStart.y)
        lineTo(stemEnd.x - earbudWidth * 0.20f, stemEnd.y)
        quadraticTo(stemEnd.x, stemEnd.y + 7f * perspectiveScale, stemEnd.x + earbudWidth * 0.20f, stemEnd.y)
        lineTo(stemStart.x + earbudWidth * 0.28f, stemStart.y)
        close()
    }
    drawPath(stemPath, brush = ceramicBodyBrush)

    // Moving Specular Light Glint along stem
    val glintY = stemStart.y + (stemEnd.y - stemStart.y) * lightPhase
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(CyberCyanLight.copy(alpha = 0.7f), Color.Transparent),
            center = Offset(stemStart.x, glintY),
            radius = 16f * perspectiveScale
        ),
        radius = 16f * perspectiveScale,
        center = Offset(stemStart.x, glintY)
    )

    // Subtle edge rim light
    drawPath(
        stemPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                CyberCyan.copy(alpha = 0.85f),
                ElectricBlue.copy(alpha = 0.3f),
                CyberCyan.copy(alpha = 0.65f)
            )
        ),
        style = Stroke(width = 1.6f * perspectiveScale)
    )

    // Capacitive Touch Sensor Groove on outer stem
    val touchGrooveY = stemStart.y + (stemEnd.y - stemStart.y) * 0.42f
    val touchGrooveX = stemStart.x + (stemEnd.x - stemStart.x) * 0.42f
    drawRoundRect(
        color = CyberCyan.copy(alpha = 0.8f),
        topLeft = Offset(touchGrooveX - 2.5f * perspectiveScale, touchGrooveY - 10f * perspectiveScale),
        size = Size(5f * perspectiveScale, 20f * perspectiveScale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * perspectiveScale)
    )

    // Metallic Chrome Bottom Cap & Dual Gold Charging Contacts
    val capH = 8f * perspectiveScale
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFF64748B)),
            start = Offset(stemEnd.x - 10f, stemEnd.y),
            end = Offset(stemEnd.x + 10f, stemEnd.y + capH)
        ),
        topLeft = Offset(stemEnd.x - earbudWidth * 0.18f, stemEnd.y),
        size = Size(earbudWidth * 0.36f, capH)
    )

    // Bottom Gold Contact Pins
    drawCircle(
        color = Color(0xFFFFD700),
        radius = 2.2f * perspectiveScale,
        center = Offset(stemEnd.x - 4.5f * perspectiveScale, stemEnd.y + capH * 0.6f)
    )
    drawCircle(
        color = Color(0xFFFFD700),
        radius = 2.2f * perspectiveScale,
        center = Offset(stemEnd.x + 4.5f * perspectiveScale, stemEnd.y + capH * 0.6f)
    )

    // 2. Earbud Head Bulb (Acoustic Chamber)
    val bulbCenter = Offset(
        centerX + (if (isLeft) -18f else 18f) * perspectiveScale,
        centerY - 20f * perspectiveScale
    )

    // 3D Sphere Shading for Bulb
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF3B4D6E),
                Color(0xFF1E293B),
                Color(0xFF0A0F1D)
            ),
            center = Offset(bulbCenter.x - 8f * perspectiveScale, bulbCenter.y - 8f * perspectiveScale),
            radius = bulbRadius * 1.15f
        ),
        radius = bulbRadius,
        center = bulbCenter
    )

    // Specular Highlight Spot on Bulb
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                CyberCyanLight.copy(alpha = 0.3f),
                Color.Transparent
            ),
            center = Offset(bulbCenter.x - 7f * perspectiveScale, bulbCenter.y - 7f * perspectiveScale),
            radius = bulbRadius * 0.5f
        ),
        radius = bulbRadius * 0.5f,
        center = Offset(bulbCenter.x - 7f * perspectiveScale, bulbCenter.y - 7f * perspectiveScale)
    )

    // Neon Rim Edge around Earpiece
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(CyberCyan, ElectricBlue, Color.Transparent, CyberCyan),
            center = bulbCenter
        ),
        radius = bulbRadius,
        center = bulbCenter,
        style = Stroke(width = 2.4f * perspectiveScale)
    )

    // In-ear Silicone Tip with Ergonomic Contour
    val tipCenter = Offset(
        bulbCenter.x + (if (isLeft) -14f else 14f) * perspectiveScale,
        bulbCenter.y + 6f * perspectiveScale
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E293B),
                Color(0xFF0F172A),
                Color(0xFF030712)
            ),
            center = tipCenter,
            radius = 16f * perspectiveScale
        ),
        topLeft = Offset(tipCenter.x - 10f * perspectiveScale, tipCenter.y - 12f * perspectiveScale),
        size = Size(20f * perspectiveScale, 24f * perspectiveScale)
    )

    // Acoustic Sound Port Mesh Grille
    drawOval(
        color = Color(0xFF020408),
        topLeft = Offset(tipCenter.x - 5f * perspectiveScale, tipCenter.y - 8f * perspectiveScale),
        size = Size(10f * perspectiveScale, 16f * perspectiveScale)
    )

    // Secondary Ambient Noise-Cancelling Mic Grille on Top
    val topMicCenter = Offset(bulbCenter.x, bulbCenter.y - bulbRadius + 4f * perspectiveScale)
    drawOval(
        color = Color(0xFF0B1120),
        topLeft = Offset(topMicCenter.x - 4f * perspectiveScale, topMicCenter.y - 2f * perspectiveScale),
        size = Size(8f * perspectiveScale, 4f * perspectiveScale)
    )
}
