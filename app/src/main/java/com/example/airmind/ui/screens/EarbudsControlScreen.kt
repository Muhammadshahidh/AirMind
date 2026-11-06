package com.example.airmind.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airmind.core.assistant.AiAssistantEngine
import com.example.airmind.core.assistant.SpeechManager
import com.example.airmind.core.audio.NoiseCancellationController
import com.example.airmind.core.audio.NoiseControlMode
import com.example.airmind.core.bluetooth.BluetoothDeviceManager
import com.example.airmind.core.bluetooth.EarbudActionHandler
import com.example.airmind.core.datastore.UserPreferencesRepository
import com.example.airmind.ui.components.CyberTopAppBar
import com.example.airmind.ui.components.Earbuds3DCanvas
import com.example.airmind.ui.components.GlassmorphicCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepNeonBlue
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun EarbudsControlScreen(
    deviceManager: BluetoothDeviceManager,
    actionHandler: EarbudActionHandler,
    assistantEngine: AiAssistantEngine,
    speechManager: SpeechManager,
    preferencesRepository: UserPreferencesRepository,
    noiseController: NoiseCancellationController? = null,
    onNavigateToGestures: () -> Unit = {},
    onNavigateToBluetooth: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val earbudState by deviceManager.connectedState.collectAsState()
    val isRecognizing by speechManager.isRecognizing.collectAsState()
    val isSpeaking by speechManager.isSpeaking.collectAsState()
    val allowPhoneFallback by preferencesRepository.isPhoneFallbackAllowed.collectAsState(initial = false)
    val noiseMode = noiseController?.currentMode?.collectAsState()?.value ?: NoiseControlMode.OFF
    val ancLevel = noiseController?.ancLevel?.collectAsState()?.value ?: 0.85f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        CyberTopAppBar(
            title = "Earbuds Hardware Control",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // Live Status Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (earbudState.isConnected) Color(0x1A00E5FF) else Color(0x1AFF3366))
                        .border(
                            1.dp,
                            if (earbudState.isConnected) CyberCyan.copy(alpha = 0.5f) else NeonRed.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (earbudState.isConnected) CyberCyan else NeonRed)
                        )
                        Text(
                            text = if (earbudState.isConnected) "EARBUDS MODE ACTIVE" else "NO EARBUDS CONNECTED",
                            color = if (earbudState.isConnected) CyberCyan else NeonRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    if (!earbudState.isConnected) {
                        Text(
                            text = "Connect Bluetooth",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onNavigateToBluetooth() }
                                .padding(4.dp)
                                .testTag("connect_bluetooth_link")
                        )
                    }
                }
            }

            // 3D Earbuds Presentation
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Earbuds3DCanvas(
                        size = 190.dp,
                        showParticles = false,
                        showRings = earbudState.isConnected,
                        autoRotate = true
                    )
                }
            }

            // SECTION A: CONNECTION & HARDWARE INFORMATION
            item {
                SectionHeader(title = "A. CONNECTION & HARDWARE INFORMATION", icon = Icons.Default.Bluetooth)
            }

            item {
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfoRow(label = "Device Name", value = earbudState.deviceName)
                        InfoRow(
                            label = "Hardware Address",
                            value = if (earbudState.address.isNotBlank()) earbudState.address else "Protected by OS"
                        )
                        InfoRow(
                            label = "Device Classification",
                            value = earbudState.deviceType.replace("_", " ")
                        )
                        InfoRow(
                            label = "Audio Profile",
                            value = earbudState.audioProfileType
                        )
                        InfoRow(
                            label = "Active Audio Route",
                            value = earbudState.activeAudioRoute
                        )
                        InfoRow(
                            label = "Microphone Status",
                            value = if (earbudState.hasMicrophone) "Earbuds Bluetooth Mic Ready" else "Microphone Not Available"
                        )

                        // Battery Telemetry
                        val batteryText = if (earbudState.batteryCombined != null) {
                            "${earbudState.batteryCombined}%"
                        } else {
                            "Not Available (Hardware does not expose battery)"
                        }
                        InfoRow(label = "Battery Telemetry", value = batteryText)

                        if (earbudState.batteryLeft != null || earbudState.batteryRight != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Left: ${earbudState.batteryLeft?.let { "$it%" } ?: "N/A"}",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Right: ${earbudState.batteryRight?.let { "$it%" } ?: "N/A"}",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Case: ${earbudState.batteryCase?.let { "$it%" } ?: "N/A"}",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Switch: Phone Fallback Permission
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Allow Phone Fallback when Disconnected",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "If OFF, Airmind will never route voice/mic to phone speaker/mic when earbuds are disconnected.",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                            }

                            Switch(
                                checked = allowPhoneFallback,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        preferencesRepository.setPhoneFallbackAllowed(checked)
                                        Toast.makeText(
                                            context,
                                            if (checked) "Phone fallback enabled" else "Phone fallback blocked (Earbuds only)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CyberCyan,
                                    uncheckedTrackColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier.testTag("switch_phone_fallback")
                            )
                        }
                    }
                }
            }

            // SECTION B: NOISE CONTROL (ANC / TRANSPARENCY / OFF)
            item {
                SectionHeader(title = "B. NOISE CONTROL", icon = Icons.Default.Hearing)
            }

            item {
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Real hardware support banner
                        if (!earbudState.isAncSupported) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x2238BDF8))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Hardware ANC: Not Supported by standard Bluetooth profile. Software DSP Active.",
                                    color = TextWhite,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NoiseModePill(
                                label = "ANC",
                                isSelected = noiseMode == NoiseControlMode.ANC,
                                modifier = Modifier.weight(1f),
                                testTag = "noise_mode_anc",
                                onClick = {
                                    noiseController?.setMode(NoiseControlMode.ANC)
                                    Toast.makeText(context, "Active Noise Cancellation enabled", Toast.LENGTH_SHORT).show()
                                }
                            )

                            NoiseModePill(
                                label = "Transparency",
                                isSelected = noiseMode == NoiseControlMode.TRANSPARENCY,
                                modifier = Modifier.weight(1.3f),
                                testTag = "noise_mode_transparency",
                                onClick = {
                                    noiseController?.setMode(NoiseControlMode.TRANSPARENCY)
                                    Toast.makeText(context, "Transparency Mode enabled", Toast.LENGTH_SHORT).show()
                                }
                            )

                            NoiseModePill(
                                label = "Off",
                                isSelected = noiseMode == NoiseControlMode.OFF,
                                modifier = Modifier.weight(1f),
                                testTag = "noise_mode_off",
                                onClick = {
                                    noiseController?.turnOff()
                                    Toast.makeText(context, "Noise control set to Off", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // SECTION C: PHYSICAL CONTROLS
            item {
                SectionHeader(title = "C. PHYSICAL EARBUDS CONTROLS", icon = Icons.Default.TouchApp)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhysicalActionRow(
                        gesture = "Single Tap",
                        action = "Start / Stop Listening",
                        icon = Icons.Default.Mic,
                        onSimulate = {
                            actionHandler.executeMappedAction("SINGLE_TAP", "BOTH", "START_STOP_LISTENING")
                            Toast.makeText(context, "Simulated Single Tap -> Start/Stop Listening", Toast.LENGTH_SHORT).show()
                        }
                    )

                    PhysicalActionRow(
                        gesture = "Double Tap",
                        action = "Activate AIRMIND AI",
                        icon = Icons.Default.AutoAwesome,
                        onSimulate = {
                            actionHandler.executeMappedAction("DOUBLE_TAP", "BOTH", "ACTIVATE_AI")
                            Toast.makeText(context, "Simulated Double Tap -> Activate AI", Toast.LENGTH_SHORT).show()
                        }
                    )

                    PhysicalActionRow(
                        gesture = "Triple Tap",
                        action = "Repeat Last AI Response",
                        icon = Icons.Default.Replay,
                        onSimulate = {
                            actionHandler.executeMappedAction("TRIPLE_TAP", "BOTH", "REPEAT_RESPONSE")
                            Toast.makeText(context, "Simulated Triple Tap -> Repeat Response", Toast.LENGTH_SHORT).show()
                        }
                    )

                    PhysicalActionRow(
                        gesture = "Long Press",
                        action = "Voice Assistant",
                        icon = Icons.Default.TouchApp,
                        onSimulate = {
                            actionHandler.executeMappedAction("LONG_PRESS", "BOTH", "VOICE_ASSISTANT")
                            Toast.makeText(context, "Simulated Long Press -> Voice Assistant", Toast.LENGTH_SHORT).show()
                        }
                    )

                    PhysicalActionRow(
                        gesture = "Volume Up",
                        action = "Increase Volume (+1)",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        onSimulate = {
                            actionHandler.executeMappedAction("LONG_PRESS", "RIGHT", "VOLUME_UP")
                            Toast.makeText(context, "Volume Increased", Toast.LENGTH_SHORT).show()
                        }
                    )

                    PhysicalActionRow(
                        gesture = "Volume Down",
                        action = "Decrease Volume (-1)",
                        icon = Icons.AutoMirrored.Filled.VolumeDown,
                        onSimulate = {
                            actionHandler.executeMappedAction("LONG_PRESS", "LEFT", "VOLUME_DOWN")
                            Toast.makeText(context, "Volume Decreased", Toast.LENGTH_SHORT).show()
                        }
                    )

                    OutlinedButton(
                        onClick = onNavigateToGestures,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_custom_gestures_studio"),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Customize Mappings in Gesture Studio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // SECTION D: AIRMIND AI HANDS-FREE CONTROLS
            item {
                SectionHeader(title = "D. AIRMIND AI HANDS-FREE CONTROLS", icon = Icons.Default.AutoAwesome)
            }

            item {
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Real-time AI Status Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isRecognizing -> Color(0x33FF3366)
                                        isSpeaking -> Color(0x3300E5FF)
                                        else -> Color(0xFF0F172A)
                                    }
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isRecognizing -> NeonRed
                                            isSpeaking -> CyberCyan
                                            else -> TextGray
                                        }
                                    )
                            )
                            Text(
                                text = when {
                                    isRecognizing -> "AI IS LISTENING VIA EARBUDS MIC..."
                                    isSpeaking -> "AI IS SPEAKING THROUGH EARBUDS AUDIO..."
                                    else -> "Hands-Free Voice Engine Ready"
                                },
                                color = when {
                                    isRecognizing -> NeonRed
                                    isSpeaking -> CyberCyan
                                    else -> TextGray
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 4 Primary Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ElevatedButton(
                                onClick = {
                                    assistantEngine.startVoiceListening()
                                    Toast.makeText(context, "Listening through earbuds...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_ai_start_listening"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = CyberCyan,
                                    contentColor = Color(0xFF050811)
                                )
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Listen", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            ElevatedButton(
                                onClick = {
                                    speechManager.stopListening()
                                    Toast.makeText(context, "Listening stopped", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_ai_stop_listening"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = Color(0xFF1E293B),
                                    contentColor = TextWhite
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val repeated = assistantEngine.repeatLastResponse()
                                    if (repeated) {
                                        Toast.makeText(context, "Repeating AI response", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_ai_repeat_response"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite)
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Repeat", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    assistantEngine.interruptSpeech()
                                    Toast.makeText(context, "AI Speech Interrupted", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("btn_ai_interrupt_speech"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Interrupt", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextGray, fontSize = 13.sp)
        Text(
            text = value,
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NoiseModePill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(if (isSelected) CyberCyan else Color(0xFF1E293B))
            .border(
                1.dp,
                if (isSelected) CyberCyan else CardBorder,
                RoundedCornerShape(19.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF050811) else TextWhite,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun PhysicalActionRow(
    gesture: String,
    action: String,
    icon: ImageVector,
    onSimulate: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("action_row_${gesture.lowercase().replace(" ", "_")}"),
        onClick = onSimulate
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x1800E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = gesture,
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = action,
                        color = CyberCyan,
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Test Trigger",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
