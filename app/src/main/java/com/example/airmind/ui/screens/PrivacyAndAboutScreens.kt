package com.example.airmind.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.airmind.core.database.AirMindDatabase
import com.example.airmind.ui.components.CyberTopAppBar
import com.example.airmind.ui.components.GlassmorphicCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun PrivacyDashboardScreen(
    database: AirMindDatabase,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        CyberTopAppBar(
            title = "Privacy Dashboard",
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
                text = "Zero unauthorized data transmission. Your device, your control.",
                color = TextGray,
                fontSize = 13.sp
            )

            PrivacyCard(
                icon = Icons.Default.MicNone,
                title = "No Secret Audio Recording",
                description = "Microphone capture is only active when Remote Listening is explicitly switched on by you. A persistent system foreground notification is displayed at all times."
            )

            PrivacyCard(
                icon = Icons.Default.Lock,
                title = "Zero Audio Transmission to Cloud",
                description = "Remote listening audio stays strictly on-device, streamed locally over Bluetooth to your earbuds. It is never uploaded, recorded, or stored on external servers."
            )

            PrivacyCard(
                icon = Icons.Default.Storage,
                title = "100% Local Database Storage",
                description = "All gesture mappings, device profiles, and automations are stored strictly in an encrypted on-device Room SQLite database."
            )

            PrivacyCard(
                icon = Icons.Default.Security,
                title = "Permissions Transparency",
                description = "Permissions for Bluetooth, Microphone, and Camera are only requested when corresponding actions are used."
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        database.voiceNoteDao().clearNotes()
                        Toast.makeText(context, "Voice notes and history cleared", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("clear_all_data_btn"),
                shape = RoundedCornerShape(25.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Local Notes & History", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AboutApplicationScreen(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        CyberTopAppBar(
            title = "About AIRMIND AI",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200E5FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "AIRMIND AI",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0.0 (Production Build)",
                        color = CyberCyan,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Universal Bluetooth Earbuds Controller & AI Assistant",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }
            }

            AboutSection(
                title = "Hardware Support",
                content = "Compatible with standard Bluetooth earbuds including Apple AirPods, Samsung Galaxy Buds, boAt Airdopes, Xiaomi Redmi Buds, Sony WF/WH series, Bose QuietComfort, and all A2DP/HFP Bluetooth headsets."
            )

            AboutSection(
                title = "Architecture & Engineering",
                content = "Engineered with Clean Architecture, MVVM, Jetpack Compose, Material 3, Kotlin Coroutines, StateFlow, Room Database, DataStore Preferences, CameraX, and real low-latency AudioRecord/AudioTrack DSP routing."
            )

            AboutSection(
                title = "Hardware Reality Notice",
                content = "AIRMIND AI respects Android platform security constraints. Standard Bluetooth devices communicate predefined media events. Custom gestures execute actions mapped to received hardware signals without fake battery percentages."
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PrivacyCard(icon: ImageVector, title: String, description: String) {
    GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x1800E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun AboutSection(title: String, content: String) {
    GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = title, color = CyberCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = content, color = TextGray, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
