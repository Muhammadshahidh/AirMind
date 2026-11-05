package com.example.airmind.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airmind.ui.components.Earbuds3DCanvas
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanLight
import com.example.ui.theme.DeepNeonBlue
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    var currentStatusText by remember { mutableStateOf("INITIALIZING 3D AUDIO MATRIX...") }

    val transition = rememberInfiniteTransition(label = "hud_pulse")
    val hudGlow by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud"
    )

    LaunchedEffect(Unit) {
        // Step 1: Initialize audio engine
        delay(400)
        currentStatusText = "LOADING BLUETOOTH A2DP / SCO SUBSYSTEM..."
        progress.animateTo(0.35f, tween(700, easing = FastOutSlowInEasing))

        // Step 2: Initialize AI neural voice engine
        currentStatusText = "SYNCING AI NEURAL LOGIC..."
        progress.animateTo(0.75f, tween(800, easing = FastOutSlowInEasing))

        // Step 3: Calibrate DSP & complete
        currentStatusText = "AIRMIND SYSTEM READY"
        progress.animateTo(1.0f, tween(600, easing = FastOutSlowInEasing))
        delay(300)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF03050C),
                        BackgroundDark,
                        Color(0xFF070D1A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Top HUD Status Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1400E5FF))
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = hudGlow))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AIRMIND CORE ACTIVE",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Real-Time 3D Rotating AirBuds & Energy Rings
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .testTag("splash_3d_earbuds"),
                contentAlignment = Alignment.Center
            ) {
                Earbuds3DCanvas(
                    size = 300.dp,
                    showParticles = true,
                    showRings = true,
                    autoRotate = true
                )
            }

            // Branding & Loading Progress HUD
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "AIRMIND AI",
                    color = TextWhite,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your Earbuds. Your AI Control.",
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Progress Bar with Status Text
                Column(
                    modifier = Modifier.fillMaxWidth(0.72f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyberCyan,
                        trackColor = Color(0xFF131D33)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentStatusText,
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}
