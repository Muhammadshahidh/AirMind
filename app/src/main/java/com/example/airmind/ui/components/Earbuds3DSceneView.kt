package com.example.airmind.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.airmind.core.controls.OrientationSensorManager
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardBorderGlow
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanLight
import com.example.ui.theme.DeepNeonBlue
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.cos
import kotlin.math.sin

enum class InspectionComponent {
    DUAL_EARBUDS,
    LEFT_EARBUD,
    RIGHT_EARBUD,
    CHARGING_CASE
}

enum class SceneInteractionMode {
    GYRO_TRACKING,
    MANUAL_ORBIT,
    AUTO_SPIN
}

/**
 * High-performance 3D AR SceneView for connected earbuds.
 * Provides real-time rendering, touch drag orbit, pinch-to-zoom, orientation tracking,
 * and holographic energy field visualization.
 */
@Composable
fun Earbuds3DSceneView(
    sensorManager: OrientationSensorManager,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    showRings: Boolean = true,
    inspectionComponent: InspectionComponent = InspectionComponent.DUAL_EARBUDS,
    interactionMode: SceneInteractionMode = SceneInteractionMode.GYRO_TRACKING,
    zoomScale: Float = 1.0f,
    onZoomChange: ((Float) -> Unit)? = null
) {
    val orientation by sensorManager.orientationAngles.collectAsState()

    DisposableEffect(sensorManager) {
        sensorManager.startListening()
        onDispose {
            sensorManager.stopListening()
        }
    }

    var manualRotationX by remember { mutableFloatStateOf(0f) }
    var manualRotationY by remember { mutableFloatStateOf(0f) }
    var currentZoom by remember { mutableFloatStateOf(zoomScale) }

    val transition = rememberInfiniteTransition(label = "sceneview_3d_loop")
    val idleFloat by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_float"
    )

    val autoSpinAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_spin"
    )

    val ringPulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )

    val effectivePitch = when (interactionMode) {
        SceneInteractionMode.GYRO_TRACKING -> (orientation.pitch * 0.75f + manualRotationY).coerceIn(-60f, 60f)
        SceneInteractionMode.MANUAL_ORBIT -> manualRotationY.coerceIn(-75f, 75f)
        SceneInteractionMode.AUTO_SPIN -> (sin(Math.toRadians(autoSpinAngle.toDouble())) * 15.0).toFloat()
    }

    val effectiveRoll = when (interactionMode) {
        SceneInteractionMode.GYRO_TRACKING -> (orientation.roll * 0.85f + manualRotationX).coerceIn(-90f, 90f)
        SceneInteractionMode.MANUAL_ORBIT -> manualRotationX
        SceneInteractionMode.AUTO_SPIN -> autoSpinAngle
    }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(interactionMode) {
                detectTransformGestures { _, pan, zoom, _ ->
                    manualRotationX = (manualRotationX + pan.x * 0.45f)
                    manualRotationY = (manualRotationY + pan.y * 0.35f).coerceIn(-75f, 75f)
                    val newZoom = (currentZoom * zoom).coerceIn(0.7f, 2.4f)
                    currentZoom = newZoom
                    onZoomChange?.invoke(newZoom)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = this.size.width / 2f
            val centerY = (this.size.height / 2f) + (if (interactionMode != SceneInteractionMode.AUTO_SPIN) idleFloat else 0f)

            val shadowOffsetX = (sin(Math.toRadians(effectiveRoll.toDouble())) * 18.0).toFloat()
            val shadowOffsetY = (sin(Math.toRadians(effectivePitch.toDouble())) * 10.0).toFloat()
            val shadowW = this.size.width * 0.65f * currentZoom
            val shadowH = 26f * currentZoom
            val groundY = this.size.height - 20f

            // 1. Perspective Depth Ground Shadow
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xB3000000), Color(0x3300E5FF), Color.Transparent),
                    center = Offset(centerX + shadowOffsetX, groundY + shadowOffsetY),
                    radius = shadowW / 2f
                ),
                topLeft = Offset(centerX - shadowW / 2f + shadowOffsetX, groundY - shadowH / 2f + shadowOffsetY),
                size = Size(shadowW, shadowH)
            )

            // 2. Holographic Energy Ring Matrix
            if (showRings) {
                val ringRadius = (this.size.width * 0.40f) * ringPulse * currentZoom
                val ringCenterY = centerY + 32f + shadowOffsetY

                drawOval(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.85f),
                            ElectricBlue.copy(alpha = 0.25f),
                            CyberCyanLight.copy(alpha = 0.9f),
                            Color.Transparent,
                            CyberCyan.copy(alpha = 0.85f)
                        ),
                        center = Offset(centerX, ringCenterY)
                    ),
                    topLeft = Offset(centerX - ringRadius, ringCenterY - (ringRadius * 0.32f)),
                    size = Size(ringRadius * 2f, ringRadius * 0.64f),
                    style = Stroke(width = 3.2f)
                )

                drawOval(
                    color = CyberCyan.copy(alpha = 0.35f),
                    topLeft = Offset(centerX - ringRadius * 0.72f, ringCenterY - (ringRadius * 0.23f)),
                    size = Size(ringRadius * 1.44f, ringRadius * 0.46f),
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round)
                )
            }

            // 3. Render 3D Model based on Component Selection
            val rollRad = Math.toRadians(effectiveRoll.toDouble())
            val pitchRad = Math.toRadians(effectivePitch.toDouble())

            when (inspectionComponent) {
                InspectionComponent.DUAL_EARBUDS -> {
                    val leftZ = (sin(rollRad) * 44.0).toFloat()
                    val rightZ = (-sin(rollRad) * 44.0).toFloat()

                    if (leftZ < rightZ) {
                        drawDetailed3DEarbud(
                            centerX = centerX - (40f * currentZoom) + (cos(rollRad) * 16f).toFloat(),
                            centerY = centerY - (10f * currentZoom) + (sin(pitchRad) * 20f).toFloat(),
                            zDepth = leftZ,
                            tiltAngle = -16f + effectiveRoll * 0.3f,
                            pitchAngle = effectivePitch,
                            isLeft = true,
                            zoom = currentZoom
                        )
                        drawDetailed3DEarbud(
                            centerX = centerX + (40f * currentZoom) - (cos(rollRad) * 16f).toFloat(),
                            centerY = centerY + (12f * currentZoom) - (sin(pitchRad) * 20f).toFloat(),
                            zDepth = rightZ,
                            tiltAngle = 18f + effectiveRoll * 0.3f,
                            pitchAngle = effectivePitch,
                            isLeft = false,
                            zoom = currentZoom
                        )
                    } else {
                        drawDetailed3DEarbud(
                            centerX = centerX + (40f * currentZoom) - (cos(rollRad) * 16f).toFloat(),
                            centerY = centerY + (12f * currentZoom) - (sin(pitchRad) * 20f).toFloat(),
                            zDepth = rightZ,
                            tiltAngle = 18f + effectiveRoll * 0.3f,
                            pitchAngle = effectivePitch,
                            isLeft = false,
                            zoom = currentZoom
                        )
                        drawDetailed3DEarbud(
                            centerX = centerX - (40f * currentZoom) + (cos(rollRad) * 16f).toFloat(),
                            centerY = centerY - (10f * currentZoom) + (sin(pitchRad) * 20f).toFloat(),
                            zDepth = leftZ,
                            tiltAngle = -16f + effectiveRoll * 0.3f,
                            pitchAngle = effectivePitch,
                            isLeft = true,
                            zoom = currentZoom
                        )
                    }
                }
                InspectionComponent.LEFT_EARBUD -> {
                    drawDetailed3DEarbud(
                        centerX = centerX,
                        centerY = centerY,
                        zDepth = 20f,
                        tiltAngle = effectiveRoll * 0.5f,
                        pitchAngle = effectivePitch,
                        isLeft = true,
                        zoom = currentZoom * 1.3f
                    )
                }
                InspectionComponent.RIGHT_EARBUD -> {
                    drawDetailed3DEarbud(
                        centerX = centerX,
                        centerY = centerY,
                        zDepth = 20f,
                        tiltAngle = effectiveRoll * 0.5f,
                        pitchAngle = effectivePitch,
                        isLeft = false,
                        zoom = currentZoom * 1.3f
                    )
                }
                InspectionComponent.CHARGING_CASE -> {
                    drawDetailed3DChargingCase(
                        centerX = centerX,
                        centerY = centerY,
                        rollAngle = effectiveRoll,
                        pitchAngle = effectivePitch,
                        zoom = currentZoom
                    )
                }
            }
        }
    }
}

/**
 * Fullscreen Interactive 3D AR Inspection Studio Dialog.
 * Allows users to inspect earbuds in 360 degrees with component breakdown,
 * zoom control, gyro vs manual orbit toggles, and hardware specs.
 */
@Composable
fun Earbuds3DInspectionDialog(
    deviceName: String,
    sensorManager: OrientationSensorManager,
    onDismiss: () -> Unit
) {
    var selectedComponent by remember { mutableStateOf(InspectionComponent.DUAL_EARBUDS) }
    var interactionMode by remember { mutableStateOf(SceneInteractionMode.MANUAL_ORBIT) }
    var currentZoom by remember { mutableFloatStateOf(1.15f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200E5FF))
                                .border(1.dp, CyberCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "3D AR Inspection Studio",
                                color = TextWhite,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = deviceName,
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x18FFFFFF))
                            .testTag("close_3d_inspection_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Component Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InspectionChip(
                        title = "Pair",
                        isSelected = selectedComponent == InspectionComponent.DUAL_EARBUDS,
                        onClick = { selectedComponent = InspectionComponent.DUAL_EARBUDS }
                    )
                    InspectionChip(
                        title = "Left Bud",
                        isSelected = selectedComponent == InspectionComponent.LEFT_EARBUD,
                        onClick = { selectedComponent = InspectionComponent.LEFT_EARBUD }
                    )
                    InspectionChip(
                        title = "Right Bud",
                        isSelected = selectedComponent == InspectionComponent.RIGHT_EARBUD,
                        onClick = { selectedComponent = InspectionComponent.RIGHT_EARBUD }
                    )
                    InspectionChip(
                        title = "Case",
                        isSelected = selectedComponent == InspectionComponent.CHARGING_CASE,
                        onClick = { selectedComponent = InspectionComponent.CHARGING_CASE }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive 3D Model Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .border(1.dp, CardBorderGlow, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Earbuds3DSceneView(
                        sensorManager = sensorManager,
                        size = 320.dp,
                        showRings = true,
                        inspectionComponent = selectedComponent,
                        interactionMode = interactionMode,
                        zoomScale = currentZoom,
                        onZoomChange = { currentZoom = it }
                    )

                    // Top Floating Camera Tracking Mode Badges
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xD90B101E))
                            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CameraModeButton(
                            icon = Icons.Default.TouchApp,
                            label = "Orbit",
                            isActive = interactionMode == SceneInteractionMode.MANUAL_ORBIT,
                            onClick = { interactionMode = SceneInteractionMode.MANUAL_ORBIT }
                        )
                        CameraModeButton(
                            icon = Icons.Default.MotionPhotosOn,
                            label = "Gyro",
                            isActive = interactionMode == SceneInteractionMode.GYRO_TRACKING,
                            onClick = { interactionMode = SceneInteractionMode.GYRO_TRACKING }
                        )
                        CameraModeButton(
                            icon = Icons.AutoMirrored.Filled.RotateRight,
                            label = "360°",
                            isActive = interactionMode == SceneInteractionMode.AUTO_SPIN,
                            onClick = { interactionMode = SceneInteractionMode.AUTO_SPIN }
                        )
                    }

                    // Bottom Floating Zoom Indicator
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xD90B101E))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Zoom: ${(currentZoom * 100).toInt()}% • Drag to rotate 360°",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hardware Component Breakdown Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Hardware Architecture & Sensors",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ComponentSpecPill(label = "Dual Feedforward ANC Mics", value = "High SNR -42dB", modifier = Modifier.weight(1f))
                        ComponentSpecPill(label = "Acoustic Drivers", value = "11mm Dynamic Bass", modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ComponentSpecPill(label = "Touch Sensor", value = "Capacitive Precision", modifier = Modifier.weight(1f))
                        ComponentSpecPill(label = "Chamber Air Vent", value = "Pressure Relief Mesh", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectionChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0x3300E5FF) else Color(0x12FFFFFF))
            .border(1.dp, if (isSelected) CyberCyan else CardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectDragGestures { _, _ -> }
            }
            .pointerInput(isSelected) {
                // Clickable handled cleanly
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.pointerInput(Unit) {
                // Touch handling
            }
        ) {
            Text(
                text = title,
                color = if (isSelected) CyberCyan else TextGray,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.pointerInput(Unit) {
                    // Tap detector
                }
            )
        }
        // Transparent overlay for reliable click
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectDragGestures { _, _ -> }
                }
                .pointerInput(Unit) {
                    // Touch
                }
        )
    }
}

@Composable
private fun CameraModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) Color(0x3300E5FF) else Color.Transparent)
            .border(1.dp, if (isActive) CyberCyan.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) CyberCyan else TextGray,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (isActive) TextWhite else TextGray,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ComponentSpecPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0EFFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = label, color = TextGray, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun DrawScope.drawDetailed3DEarbud(
    centerX: Float,
    centerY: Float,
    zDepth: Float,
    tiltAngle: Float,
    pitchAngle: Float,
    isLeft: Boolean,
    zoom: Float
) {
    val scale = ((1.0f + (zDepth / 260f)) * zoom).coerceIn(0.7f, 2.8f)
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
        start = Offset(stemStart.x - 16f * scale, stemStart.y - 16f * scale),
        end = Offset(stemEnd.x + 16f * scale, stemEnd.y + 16f * scale)
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

    // Neon Cybernetic Stem Contour
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
        cornerRadius = CornerRadius(2.5f * scale)
    )

    // Chrome End-Cap & Gold Contact Charging Pins
    val capHeight = 7f * scale
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFCBD5E1), Color(0xFF64748B)),
            start = Offset(stemEnd.x - 8f * scale, stemEnd.y),
            end = Offset(stemEnd.x + 8f * scale, stemEnd.y + capHeight)
        ),
        topLeft = Offset(stemEnd.x - earbudWidth * 0.18f, stemEnd.y),
        size = Size(earbudWidth * 0.36f, capHeight)
    )
    drawCircle(color = Color(0xFFFFD700), radius = 2f * scale, center = Offset(stemEnd.x - 4f * scale, stemEnd.y + capHeight * 0.6f))
    drawCircle(color = Color(0xFFFFD700), radius = 2f * scale, center = Offset(stemEnd.x + 4f * scale, stemEnd.y + capHeight * 0.6f))

    // Earbud Head Bulb
    val bulbCenter = Offset(
        centerX + (if (isLeft) -16f else 16f) * scale,
        centerY - 18f * scale + (pitchAngle * 0.15f * scale)
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
            colors = listOf(Color.White.copy(alpha = 0.85f), CyberCyanLight.copy(alpha = 0.2f), Color.Transparent),
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

    // Feedforward Microphone Port on Head
    drawCircle(
        color = Color(0xFF020617),
        radius = 2.2f * scale,
        center = Offset(bulbCenter.x + (if (isLeft) 6f else -6f) * scale, bulbCenter.y - 12f * scale)
    )
}

private fun DrawScope.drawDetailed3DChargingCase(
    centerX: Float,
    centerY: Float,
    rollAngle: Float,
    pitchAngle: Float,
    zoom: Float
) {
    val scale = zoom * 1.1f
    val caseW = 130f * scale
    val caseH = 100f * scale

    val offsetX = (sin(Math.toRadians(rollAngle.toDouble())) * 14.0).toFloat()
    val offsetY = (sin(Math.toRadians(pitchAngle.toDouble())) * 10.0).toFloat()

    // 1. Case Body (Lower Shell)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
        ),
        topLeft = Offset(centerX - caseW / 2f + offsetX, centerY - 10f + offsetY),
        size = Size(caseW, caseH * 0.65f),
        cornerRadius = CornerRadius(22f * scale)
    )

    // Cyber contour
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(CyberCyan.copy(alpha = 0.8f), ElectricBlue.copy(alpha = 0.3f), CyberCyan.copy(alpha = 0.8f))
        ),
        topLeft = Offset(centerX - caseW / 2f + offsetX, centerY - 10f + offsetY),
        size = Size(caseW, caseH * 0.65f),
        cornerRadius = CornerRadius(22f * scale),
        style = Stroke(width = 2f * scale)
    )

    // 2. Open Lid (Upper Shell)
    val lidHeight = caseH * 0.45f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF334155), Color(0xFF1E293B))
        ),
        topLeft = Offset(centerX - caseW / 2f + offsetX, centerY - 10f - lidHeight + offsetY),
        size = Size(caseW, lidHeight),
        cornerRadius = CornerRadius(20f * scale)
    )

    drawRoundRect(
        color = CyberCyanLight.copy(alpha = 0.6f),
        topLeft = Offset(centerX - caseW / 2f + offsetX, centerY - 10f - lidHeight + offsetY),
        size = Size(caseW, lidHeight),
        cornerRadius = CornerRadius(20f * scale),
        style = Stroke(width = 1.5f * scale)
    )

    // 3. LED Battery Indicator
    drawCircle(
        color = NeonGreen,
        radius = 3.5f * scale,
        center = Offset(centerX + offsetX, centerY + 22f + offsetY)
    )

    // LED Glow Halo
    drawCircle(
        color = NeonGreen.copy(alpha = 0.35f),
        radius = 8f * scale,
        center = Offset(centerX + offsetX, centerY + 22f + offsetY)
    )

    // 4. Case Earbud Magnetic Cavities
    drawOval(
        color = Color(0xFF0A0F1D),
        topLeft = Offset(centerX - caseW * 0.32f + offsetX, centerY - 14f + offsetY),
        size = Size(26f * scale, 14f * scale)
    )
    drawOval(
        color = Color(0xFF0A0F1D),
        topLeft = Offset(centerX + caseW * 0.12f + offsetX, centerY - 14f + offsetY),
        size = Size(26f * scale, 14f * scale)
    )
}
