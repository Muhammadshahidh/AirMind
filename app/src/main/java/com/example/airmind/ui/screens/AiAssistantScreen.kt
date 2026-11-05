package com.example.airmind.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airmind.core.assistant.AiAssistantEngine
import com.example.airmind.core.assistant.AssistantMessage
import com.example.airmind.core.assistant.SpeechManager
import com.example.airmind.ui.components.AirMindBottomNavigation
import com.example.airmind.ui.components.CyberTopAppBar
import com.example.airmind.ui.components.GlassmorphicCard
import com.example.airmind.ui.components.NavigationTab
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardBorderGlow
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanLight
import com.example.ui.theme.DeepNeonBlue
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    assistantEngine: AiAssistantEngine,
    speechManager: SpeechManager,
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToListening: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val messages by assistantEngine.messages.collectAsState()
    val isGenerating by assistantEngine.isGenerating.collectAsState()
    val isRecognizing by speechManager.isRecognizing.collectAsState()
    val spokenLiveText by speechManager.spokenText.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Concentric soundwave animation for AI Orb
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        CyberTopAppBar(
            title = "AI Assistant",
            onBackClick = onBackClick
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Ask anything, do anything",
                    color = TextGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Glowing AI Avatar Face & Soundwave Rings
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Soundwave Ring
                    Box(
                        modifier = Modifier
                            .size(170.dp * pulse2)
                            .clip(CircleShape)
                            .border(1.dp, CyberCyan.copy(alpha = 0.2f), CircleShape)
                    )

                    // Middle Soundwave Ring
                    Box(
                        modifier = Modifier
                            .size(130.dp * pulse1)
                            .clip(CircleShape)
                            .border(1.5.dp, CyberCyan.copy(alpha = 0.45f), CircleShape)
                    )

                    // Core Glowing AI Avatar Orb
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(CyberCyanLight, DeepNeonBlue, Color(0xFF070B19))
                                )
                            )
                            .border(2.dp, CyberCyan, CircleShape)
                            .clickable {
                                if (isRecognizing) {
                                    speechManager.stopListening()
                                } else {
                                    speechManager.startListening { query ->
                                        assistantEngine.processUserQuery(query)
                                    }
                                }
                            }
                            .testTag("ai_orb_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isRecognizing) Icons.Default.Mic else Icons.Default.SmartToy,
                                contentDescription = "AI Orb",
                                tint = TextWhite,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Suggestion Prompt Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(label = "Open YouTube") {
                        assistantEngine.processUserQuery("Open YouTube")
                    }
                    SuggestionChip(label = "What's the weather?") {
                        assistantEngine.processUserQuery("What is the weather today?")
                    }
                    SuggestionChip(label = "Turn on flashlight") {
                        assistantEngine.processUserQuery("Flashlight on")
                    }
                    SuggestionChip(label = "Check earbud battery") {
                        assistantEngine.processUserQuery("Check earbud battery status")
                    }
                    SuggestionChip(label = "Tell me a joke") {
                        assistantEngine.processUserQuery("Tell me a joke")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Message History
            items(messages) { msg ->
                MessageBubble(message = msg)
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AIRMIND thinking...",
                            color = CyberCyan,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (isRecognizing && spokenLiveText.isNotBlank()) {
                item {
                    Text(
                        text = "Listening: \"$spokenLiveText\"",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Voice & Text Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speech Mic Button
            IconButton(
                onClick = {
                    if (isRecognizing) {
                        speechManager.stopListening()
                    } else {
                        speechManager.startListening { query ->
                            assistantEngine.processUserQuery(query)
                        }
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isRecognizing) CyberCyan else Color(0xFF1E293B))
                    .border(1.dp, CyberCyan, CircleShape)
                    .testTag("assistant_mic_button")
            ) {
                Icon(
                    imageVector = if (isRecognizing) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Speak",
                    tint = if (isRecognizing) Color(0xFF030712) else CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Text Input Field
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Tap to speak or type...", color = TextGray, fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("assistant_text_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        assistantEngine.processUserQuery(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(listOf(ElectricBlue, CyberCyan))
                    )
                    .testTag("assistant_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom Navigation Bar
        AirMindBottomNavigation(
            currentTab = NavigationTab.ASSISTANT,
            onTabSelected = { tab ->
                when (tab) {
                    NavigationTab.HOME -> onNavigateToHome()
                    NavigationTab.ASSISTANT -> { /* already here */ }
                    NavigationTab.LISTEN -> onNavigateToListening()
                    NavigationTab.PROFILE -> onNavigateToSettings()
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(message: AssistantMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isUser) 18.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (message.isUser) {
                        Brush.horizontalGradient(listOf(Color(0xFF0052CC), Color(0xFF007FFF)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFF111D36), Color(0xFF0C1427)))
                    }
                )
                .border(
                    1.dp,
                    if (message.isUser) CyberCyan.copy(alpha = 0.5f) else CardBorder,
                    RoundedCornerShape(18.dp)
                )
                .padding(14.dp)
        ) {
            Text(
                text = message.text,
                color = TextWhite,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1800E5FF))
            .border(1.dp, CardBorderGlow, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("chip_${label.lowercase().replace(" ", "_")}")
    ) {
        Text(
            text = label,
            color = CyberCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
