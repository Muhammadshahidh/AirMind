package com.example.airmind.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.airmind.core.audio.RemoteListeningEngine
import com.example.airmind.core.audio.RemoteListeningService
import com.example.airmind.core.datastore.UserPreferencesRepository
import com.example.airmind.ui.components.AirMindBottomNavigation
import com.example.airmind.ui.components.AudioWaveformVisualizer
import com.example.airmind.ui.components.CyberTopAppBar
import com.example.airmind.ui.components.GlassmorphicCard
import com.example.airmind.ui.components.NavigationTab
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepNeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun RemoteListeningScreen(
    listeningEngine: RemoteListeningEngine,
    preferencesRepository: UserPreferencesRepository,
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isListening by listeningEngine.isListening.collectAsState()
    val amplitude by listeningEngine.audioAmplitude.collectAsState()

    var volume by remember { mutableFloatStateOf(0.70f) }
    var balance by remember { mutableFloatStateOf(0.0f) }
    var noiseReduction by remember { mutableStateOf(true) }
    var voiceEnhance by remember { mutableStateOf(true) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            RemoteListeningService.start(context)
        } else {
            Toast.makeText(context, "Microphone permission is required for Remote Listening.", Toast.LENGTH_LONG).show()
        }
    }

    // Pulse animation for microphone wave rings
    val transition = rememberInfiniteTransition(label = "pulse_rings")
    val pulseRing by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ring"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        CyberTopAppBar(
            title = "Remote Listening",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Listen to your phone's audio through your earbuds",
                color = TextGray,
                fontSize = 13.sp
            )

            // Live Pulsing Microphone Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Expanding Soundwave Ring 1
                Box(
                    modifier = Modifier
                        .size(170.dp * (if (isListening) pulseRing else 1.0f))
                        .clip(CircleShape)
                        .border(1.dp, if (isListening) CyberCyan.copy(alpha = 0.35f) else Color(0x18FFFFFF), CircleShape)
                )

                // Expanding Soundwave Ring 2
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, if (isListening) CyberCyan.copy(alpha = 0.6f) else Color(0x22FFFFFF), CircleShape)
                )

                // Central Mic Bubble
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) {
                                Brush.radialGradient(listOf(CyberCyan, DeepNeonBlue, Color(0xFF030818)))
                            } else {
                                Brush.radialGradient(listOf(Color(0xFF334155), Color(0xFF0F172A)))
                            }
                        )
                        .border(2.dp, if (isListening) CyberCyan else CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = TextWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Status Card
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isListening) CyberCyan.copy(alpha = 0.5f) else CardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) NeonGreen else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isListening) "Listening..." else "Standby",
                                color = if (isListening) NeonGreen else TextGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Live audio waveform visualizer bars
                        AudioWaveformVisualizer(
                            amplitude = amplitude,
                            isLive = isListening,
                            barCount = 12,
                            maxHeight = 24.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isListening) {
                            "Your phone's microphone is actively capturing and streaming audio to connected Bluetooth earbuds."
                        } else {
                            "Tap start below to stream live room audio directly to your earbuds."
                        },
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Volume Slider Control
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Volume", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "${(volume * 100).toInt()}%", color = CyberCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            listeningEngine.updateSettings(volume, balance, noiseReduction, voiceEnhance)
                            scope.launch { preferencesRepository.setListeningVolume(it) }
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("listening_volume_slider")
                    )
                }
            }

            // Noise Reduction Toggle
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VolumeMute, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Noise Reduction", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Suppresses background hum", color = TextGray, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = noiseReduction,
                        onCheckedChange = {
                            noiseReduction = it
                            listeningEngine.updateSettings(volume, balance, noiseReduction, voiceEnhance)
                            scope.launch { preferencesRepository.setNoiseReduction(it) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CyberCyan),
                        modifier = Modifier.testTag("noise_reduction_toggle")
                    )
                }
            }

            // Voice Enhancement Toggle
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Voice Enhancement", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Boosts speech clarity frequencies", color = TextGray, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = voiceEnhance,
                        onCheckedChange = {
                            voiceEnhance = it
                            listeningEngine.updateSettings(volume, balance, noiseReduction, voiceEnhance)
                            scope.launch { preferencesRepository.setVoiceEnhancement(it) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CyberCyan),
                        modifier = Modifier.testTag("voice_enhancement_toggle")
                    )
                }
            }

            // Left / Right Balance Slider
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Left / Right Balance", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = when {
                                balance < -0.1f -> "L ${(-balance * 100).toInt()}%"
                                balance > 0.1f -> "R ${(balance * 100).toInt()}%"
                                else -> "Centered"
                            },
                            color = CyberCyan,
                            fontSize = 13.sp
                        )
                    }
                    Slider(
                        value = balance,
                        onValueChange = {
                            balance = it
                            listeningEngine.updateSettings(volume, balance, noiseReduction, voiceEnhance)
                            scope.launch { preferencesRepository.setListeningBalance(it) }
                        },
                        valueRange = -1f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.testTag("balance_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("L", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("R", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Action Start / Stop Floating Button Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) {
                                Brush.radialGradient(listOf(NeonRed, Color(0xFF8B0000)))
                            } else {
                                Brush.horizontalGradient(listOf(CyberCyan, DeepNeonBlue))
                            }
                        )
                        .clickable {
                            if (isListening) {
                                RemoteListeningService.stop(context)
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    RemoteListeningService.start(context)
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                        .testTag("listening_start_stop_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                        tint = TextWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Bottom Navigation Bar
        AirMindBottomNavigation(
            currentTab = NavigationTab.LISTEN,
            onTabSelected = { tab ->
                when (tab) {
                    NavigationTab.HOME -> onNavigateToHome()
                    NavigationTab.ASSISTANT -> onNavigateToAssistant()
                    NavigationTab.LISTEN -> { /* already here */ }
                    NavigationTab.PROFILE -> onNavigateToSettings()
                }
            }
        )
    }
}
