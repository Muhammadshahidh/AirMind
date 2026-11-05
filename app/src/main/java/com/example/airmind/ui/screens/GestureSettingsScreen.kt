package com.example.airmind.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.airmind.core.bluetooth.EarbudActionHandler
import com.example.airmind.core.database.AirMindDatabase
import com.example.airmind.core.database.entities.GestureMappingEntity
import com.example.airmind.ui.components.CyberTopAppBar
import com.example.airmind.ui.components.Earbuds3DCanvas
import com.example.airmind.ui.components.GlassmorphicCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

data class GestureActionDef(
    val key: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val category: String
)

@Composable
fun GestureSettingsScreen(
    database: AirMindDatabase,
    actionHandler: EarbudActionHandler? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedEarbudSide by remember { mutableStateOf("LEFT") } // "LEFT", "RIGHT", "BOTH"
    val gestureList by database.gestureDao().getAllGestures().collectAsState(initial = emptyList())

    // Active editing gesture
    var editingGesture by remember { mutableStateOf<GestureMappingEntity?>(null) }
    var lastExecutedFeedback by remember { mutableStateOf<String?>(null) }

    val availableActions = remember {
        listOf(
            GestureActionDef("PLAY_PAUSE", "Play / Pause", "Toggle music or podcast playback", Icons.Default.PlayArrow, "Media"),
            GestureActionDef("NEXT_TRACK", "Next Track", "Skip ahead to the next track", Icons.Default.SkipNext, "Media"),
            GestureActionDef("PREV_TRACK", "Previous Track", "Go back to the start or previous track", Icons.Default.SkipPrevious, "Media"),
            GestureActionDef("VOLUME_UP", "Volume Up", "Increase system playback volume", Icons.AutoMirrored.Filled.VolumeUp, "Audio"),
            GestureActionDef("VOLUME_DOWN", "Volume Down", "Decrease system playback volume", Icons.AutoMirrored.Filled.VolumeDown, "Audio"),
            GestureActionDef("ANC_CYCLE", "Cycle Noise Control", "Switch ANC, Transparency, and Normal", Icons.Default.Hearing, "Audio"),
            GestureActionDef("VOICE_ASSISTANT", "AIRMIND AI Assistant", "Trigger on-device voice assistant", Icons.Default.AutoAwesome, "Smart AI"),
            GestureActionDef("TOGGLE_FLASHLIGHT", "Toggle Flashlight", "Switch device torch on or off", Icons.Default.FlashlightOn, "Utilities"),
            GestureActionDef("NONE", "None (Disabled)", "Ignore this touch event sequence", Icons.Default.Block, "Utilities")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        CyberTopAppBar(
            title = "Gesture Mapping",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Assign smart actions to earbud touch sequences",
                    color = TextGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Earbud Selector Switcher (Left, Both, Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    EarbudTabItem(
                        title = "Left Earbud",
                        isSelected = selectedEarbudSide == "LEFT",
                        onClick = { selectedEarbudSide = "LEFT" },
                        testTag = "tab_left_earbud",
                        modifier = Modifier.weight(1f)
                    )
                    EarbudTabItem(
                        title = "Both (Sync)",
                        isSelected = selectedEarbudSide == "BOTH",
                        onClick = { selectedEarbudSide = "BOTH" },
                        testTag = "tab_both_earbuds",
                        modifier = Modifier.weight(1f)
                    )
                    EarbudTabItem(
                        title = "Right Earbud",
                        isSelected = selectedEarbudSide == "RIGHT",
                        onClick = { selectedEarbudSide = "RIGHT" },
                        testTag = "tab_right_earbud",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Earbud 3D Sensor Visual Graphic
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Earbuds3DCanvas(
                        size = 170.dp,
                        showParticles = false,
                        showRings = true,
                        autoRotate = true
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // Gesture Items Section Header
            item {
                Text(
                    text = "TOUCH SENSOR SEQUENCES",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Single Tap
            item {
                GestureItemCard(
                    gestureName = "Single Tap",
                    currentAction = getActionLabel(gestureList, "SINGLE_TAP", selectedEarbudSide, availableActions),
                    badge = "1x",
                    onClick = {
                        val mapping = gestureList.find { it.eventType == "SINGLE_TAP" && (it.earbudSide == selectedEarbudSide || it.earbudSide == "BOTH") }
                            ?: GestureMappingEntity(eventType = "SINGLE_TAP", earbudSide = selectedEarbudSide, actionType = "PLAY_PAUSE")
                        editingGesture = mapping
                    }
                )
            }

            // Double Tap
            item {
                GestureItemCard(
                    gestureName = "Double Tap",
                    currentAction = getActionLabel(gestureList, "DOUBLE_TAP", selectedEarbudSide, availableActions),
                    badge = "2x",
                    onClick = {
                        val mapping = gestureList.find { it.eventType == "DOUBLE_TAP" && (it.earbudSide == selectedEarbudSide || it.earbudSide == "BOTH") }
                            ?: GestureMappingEntity(eventType = "DOUBLE_TAP", earbudSide = selectedEarbudSide, actionType = "NEXT_TRACK")
                        editingGesture = mapping
                    }
                )
            }

            // Triple Tap
            item {
                GestureItemCard(
                    gestureName = "Triple Tap",
                    currentAction = getActionLabel(gestureList, "TRIPLE_TAP", selectedEarbudSide, availableActions),
                    badge = "3x",
                    onClick = {
                        val mapping = gestureList.find { it.eventType == "TRIPLE_TAP" && (it.earbudSide == selectedEarbudSide || it.earbudSide == "BOTH") }
                            ?: GestureMappingEntity(eventType = "TRIPLE_TAP", earbudSide = selectedEarbudSide, actionType = "PREV_TRACK")
                        editingGesture = mapping
                    }
                )
            }

            // Long Press
            item {
                GestureItemCard(
                    gestureName = "Long Press (3s)",
                    currentAction = getActionLabel(gestureList, "LONG_PRESS", selectedEarbudSide, availableActions),
                    badge = "3s",
                    onClick = {
                        val mapping = gestureList.find { it.eventType == "LONG_PRESS" && (it.earbudSide == selectedEarbudSide || it.earbudSide == "BOTH") }
                            ?: GestureMappingEntity(eventType = "LONG_PRESS", earbudSide = selectedEarbudSide, actionType = if (selectedEarbudSide == "LEFT") "VOLUME_DOWN" else "VOLUME_UP")
                        editingGesture = mapping
                    }
                )
            }

            // Interactive Live Test Section
            item {
                Spacer(modifier = Modifier.height(10.dp))
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Test Gesture Simulator",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Tap any sequence below to simulate the touch trigger on the $selectedEarbudSide earbud and run its mapped action.",
                            color = TextGray,
                            fontSize = 12.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TestGesturePill(
                                label = "1x Tap",
                                modifier = Modifier.weight(1f),
                                testTag = "test_single_tap",
                                onClick = {
                                    actionHandler?.executeMappedAction("SINGLE_TAP", selectedEarbudSide, "PLAY_PAUSE")
                                    val act = getActionLabel(gestureList, "SINGLE_TAP", selectedEarbudSide, availableActions)
                                    lastExecutedFeedback = "Triggered: Single Tap -> $act"
                                    Toast.makeText(context, "Single Tap Executed: $act", Toast.LENGTH_SHORT).show()
                                }
                            )

                            TestGesturePill(
                                label = "2x Tap",
                                modifier = Modifier.weight(1f),
                                testTag = "test_double_tap",
                                onClick = {
                                    actionHandler?.executeMappedAction("DOUBLE_TAP", selectedEarbudSide, "NEXT_TRACK")
                                    val act = getActionLabel(gestureList, "DOUBLE_TAP", selectedEarbudSide, availableActions)
                                    lastExecutedFeedback = "Triggered: Double Tap -> $act"
                                    Toast.makeText(context, "Double Tap Executed: $act", Toast.LENGTH_SHORT).show()
                                }
                            )

                            TestGesturePill(
                                label = "3x Tap",
                                modifier = Modifier.weight(1f),
                                testTag = "test_triple_tap",
                                onClick = {
                                    actionHandler?.executeMappedAction("TRIPLE_TAP", selectedEarbudSide, "PREV_TRACK")
                                    val act = getActionLabel(gestureList, "TRIPLE_TAP", selectedEarbudSide, availableActions)
                                    lastExecutedFeedback = "Triggered: Triple Tap -> $act"
                                    Toast.makeText(context, "Triple Tap Executed: $act", Toast.LENGTH_SHORT).show()
                                }
                            )

                            TestGesturePill(
                                label = "Long Press",
                                modifier = Modifier.weight(1.3f),
                                testTag = "test_long_press",
                                onClick = {
                                    actionHandler?.executeMappedAction("LONG_PRESS", selectedEarbudSide, "VOLUME_UP")
                                    val act = getActionLabel(gestureList, "LONG_PRESS", selectedEarbudSide, availableActions)
                                    lastExecutedFeedback = "Triggered: Long Press -> $act"
                                    Toast.makeText(context, "Long Press Executed: $act", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = lastExecutedFeedback != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            lastExecutedFeedback?.let { feedback ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x1A00E5FF))
                                        .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = feedback,
                                        color = CyberCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reset to Default button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            database.gestureDao().clearAll()
                            val defaults = listOf(
                                GestureMappingEntity(eventType = "SINGLE_TAP", earbudSide = "BOTH", actionType = "PLAY_PAUSE"),
                                GestureMappingEntity(eventType = "DOUBLE_TAP", earbudSide = "BOTH", actionType = "NEXT_TRACK"),
                                GestureMappingEntity(eventType = "TRIPLE_TAP", earbudSide = "BOTH", actionType = "PREV_TRACK"),
                                GestureMappingEntity(eventType = "LONG_PRESS", earbudSide = "RIGHT", actionType = "VOLUME_UP"),
                                GestureMappingEntity(eventType = "LONG_PRESS", earbudSide = "LEFT", actionType = "VOLUME_DOWN")
                            )
                            database.gestureDao().insertAll(defaults)
                            Toast.makeText(context, "Gestures reset to factory defaults", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reset_gestures_button"),
                    shape = RoundedCornerShape(25.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset to Factory Defaults", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Modal Dialog to Select Action
    editingGesture?.let { gesture ->
        AlertDialog(
            onDismissRequest = { editingGesture = null },
            containerColor = Color(0xFF0F172A),
            title = {
                Column {
                    Text(
                        text = "Assign Action",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${gesture.eventType.replace("_", " ")} on $selectedEarbudSide Earbud",
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableActions.size) { index ->
                        val action = availableActions[index]
                        val isSelected = gesture.actionType == action.key
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0x3300E5FF) else Color(0x11FFFFFF))
                                .border(
                                    1.dp,
                                    if (isSelected) CyberCyan else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        database.gestureDao().insertAll(
                                            listOf(
                                                gesture.copy(
                                                    earbudSide = selectedEarbudSide,
                                                    actionType = action.key,
                                                    isEnabled = true
                                                )
                                            )
                                        )
                                        editingGesture = null
                                        Toast.makeText(context, "${gesture.eventType} assigned to ${action.title}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(12.dp)
                                .testTag("action_option_${action.key.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) CyberCyan else Color(0x22FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF050811) else TextWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = action.title,
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = action.description,
                                        color = TextGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingGesture = null }) {
                    Text("Close", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun EarbudTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CyberCyan else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color(0xFF050811) else TextGray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun TestGesturePill(
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GestureItemCard(
    gestureName: String,
    currentAction: String,
    badge: String,
    onClick: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gesture_card_${gestureName.lowercase().replace(" ", "_").replace("(", "").replace(")", "")}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x1800E5FF))
                        .border(1.dp, Color(0x3300E5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge,
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = gestureName,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentAction,
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun getActionLabel(
    mappings: List<GestureMappingEntity>,
    eventType: String,
    side: String,
    availableActions: List<GestureActionDef>
): String {
    val match = mappings.find { it.eventType == eventType && (it.earbudSide == side || it.earbudSide == "BOTH") }
    val actionKey = match?.actionType ?: when (eventType) {
        "SINGLE_TAP" -> "PLAY_PAUSE"
        "DOUBLE_TAP" -> "NEXT_TRACK"
        "TRIPLE_TAP" -> "PREV_TRACK"
        "LONG_PRESS" -> if (side == "RIGHT") "VOLUME_UP" else "VOLUME_DOWN"
        else -> "PLAY_PAUSE"
    }

    return availableActions.find { it.key == actionKey }?.title ?: actionKey
}
