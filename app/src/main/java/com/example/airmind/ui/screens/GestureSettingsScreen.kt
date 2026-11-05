package com.example.airmind.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun GestureSettingsScreen(
    database: AirMindDatabase,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedEarbudSide by remember { mutableStateOf("LEFT") }
    val gestureList by database.gestureDao().getAllGestures().collectAsState(initial = emptyList())

    // Modal state for editing an action
    var editingGesture by remember { mutableStateOf<GestureMappingEntity?>(null) }

    val availableActions = listOf(
        "PLAY_PAUSE" to "Play / Pause",
        "NEXT_TRACK" to "Next Track",
        "PREV_TRACK" to "Previous Track",
        "VOLUME_UP" to "Volume Up",
        "VOLUME_DOWN" to "Volume Down",
        "VOICE_ASSISTANT" to "AIRMIND Voice Assistant",
        "TOGGLE_FLASHLIGHT" to "Toggle Flashlight"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        CyberTopAppBar(
            title = "Gestures Customization",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Set your own actions for touch gestures",
                    color = TextGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Left Earbud / Right Earbud Toggle Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedEarbudSide == "LEFT") CyberCyan else Color.Transparent)
                            .clickable { selectedEarbudSide = "LEFT" }
                            .testTag("tab_left_earbud"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Left Earbud",
                            color = if (selectedEarbudSide == "LEFT") Color(0xFF050811) else TextGray,
                            fontSize = 13.sp,
                            fontWeight = if (selectedEarbudSide == "LEFT") FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedEarbudSide == "RIGHT") CyberCyan else Color.Transparent)
                            .clickable { selectedEarbudSide = "RIGHT" }
                            .testTag("tab_right_earbud"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Right Earbud",
                            color = if (selectedEarbudSide == "RIGHT") Color(0xFF050811) else TextGray,
                            fontSize = 13.sp,
                            fontWeight = if (selectedEarbudSide == "RIGHT") FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Earbud 3D Sensor Visual
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Earbuds3DCanvas(
                        size = 180.dp,
                        showParticles = false,
                        showRings = false,
                        autoRotate = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Gesture Rows
            item {
                GestureItemCard(
                    gestureName = "Single Tap",
                    currentAction = getActionLabel(gestureList, "SINGLE_TAP", selectedEarbudSide),
                    badge = "1x",
                    onClick = {
                        val mapping = gestureList.find { it.eventType == "SINGLE_TAP" && (it.earbudSide == selectedEarbudSide || it.earbudSide == "BOTH") }
                            ?: GestureMappingEntity(eventType = "SINGLE_TAP", earbudSide = selectedEarbudSide, actionType = "PLAY_PAUSE")
                        editingGesture = mapping
                    }
                )
            }

            item {
                GestureItemCard(
                    gestureName = "Double Tap",
                    currentAction = getActionLabel(gestureList, "DOUBLE_TAP", selectedEarbudSide),
                    badge = "2x",
                    onClick = {
                        val mapping = gestureList.find { it.eventType == "DOUBLE_TAP" && (it.earbudSide == selectedEarbudSide || it.earbudSide == "BOTH") }
                            ?: GestureMappingEntity(eventType = "DOUBLE_TAP", earbudSide = selectedEarbudSide, actionType = "NEXT_TRACK")
                        editingGesture = mapping
                    }
                )
            }

            item {
                GestureItemCard(
                    gestureName = "Triple Tap",
                    currentAction = getActionLabel(gestureList, "TRIPLE_TAP", selectedEarbudSide),
                    badge = "3x",
                    onClick = {
                        val mapping = gestureList.find { it.eventType == "TRIPLE_TAP" && (it.earbudSide == selectedEarbudSide || it.earbudSide == "BOTH") }
                            ?: GestureMappingEntity(eventType = "TRIPLE_TAP", earbudSide = selectedEarbudSide, actionType = "PREV_TRACK")
                        editingGesture = mapping
                    }
                )
            }

            item {
                GestureItemCard(
                    gestureName = "Long Press",
                    currentAction = getActionLabel(gestureList, "LONG_PRESS", selectedEarbudSide),
                    badge = "3s",
                    onClick = {
                        val mapping = gestureList.find { it.eventType == "LONG_PRESS" && (it.earbudSide == selectedEarbudSide || it.earbudSide == "BOTH") }
                            ?: GestureMappingEntity(eventType = "LONG_PRESS", earbudSide = selectedEarbudSide, actionType = "VOICE_ASSISTANT")
                        editingGesture = mapping
                    }
                )
            }

            // Reset to Default button
            item {
                Spacer(modifier = Modifier.height(10.dp))
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
                            Toast.makeText(context, "Gestures reset to defaults", Toast.LENGTH_SHORT).show()
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
                    Text("Reset to Default", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal Dialog to Select Action
    editingGesture?.let { gesture ->
        AlertDialog(
            onDismissRequest = { editingGesture = null },
            containerColor = Color(0xFF0F172A),
            title = {
                Text(
                    text = "Assign Action to ${gesture.eventType.replace("_", " ")}",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableActions.forEach { (actionKey, label) ->
                        val isSelected = gesture.actionType == actionKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0x3300E5FF) else Color(0x11FFFFFF))
                                .clickable {
                                    scope.launch {
                                        database.gestureDao().insertAll(
                                            listOf(
                                                gesture.copy(
                                                    earbudSide = selectedEarbudSide,
                                                    actionType = actionKey
                                                )
                                            )
                                        )
                                        editingGesture = null
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, color = TextWhite, fontSize = 13.sp)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingGesture = null }) {
                    Text("Cancel", color = CyberCyan)
                }
            }
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
            .testTag("gesture_card_${gestureName.lowercase().replace(" ", "_")}"),
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x1800E5FF)),
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
    side: String
): String {
    val match = mappings.find { it.eventType == eventType && (it.earbudSide == side || it.earbudSide == "BOTH") }
    return when (match?.actionType) {
        "PLAY_PAUSE" -> "Play / Pause"
        "NEXT_TRACK" -> "Next Track"
        "PREV_TRACK" -> "Previous Track"
        "VOLUME_UP" -> "Volume Up"
        "VOLUME_DOWN" -> "Volume Down"
        "VOICE_ASSISTANT" -> "AIRMIND Voice Assistant"
        "TOGGLE_FLASHLIGHT" -> "Toggle Flashlight"
        null -> when (eventType) {
            "SINGLE_TAP" -> "Play / Pause"
            "DOUBLE_TAP" -> "Next Track"
            "TRIPLE_TAP" -> "Previous Track"
            "LONG_PRESS" -> if (side == "RIGHT") "Volume Up" else "Volume Down"
            else -> "Play / Pause"
        }
        else -> match.actionType
    }
}
