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
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.airmind.core.audio.NoiseCancellationController
import com.example.airmind.core.audio.NoiseControlMode
import com.example.airmind.core.bluetooth.EarbudActionHandler
import com.example.airmind.ui.components.CyberTopAppBar
import com.example.airmind.ui.components.Earbuds3DCanvas
import com.example.airmind.ui.components.GlassmorphicCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepNeonBlue
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite

enum class EarbudSideTab { LEFT, BOTH, RIGHT }

@Composable
fun EarbudsControlScreen(
    actionHandler: EarbudActionHandler,
    noiseController: NoiseCancellationController? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedSide by remember { mutableStateOf(EarbudSideTab.BOTH) }
    var playPauseEnabled by remember { mutableStateOf(true) }
    val noiseMode = noiseController?.currentMode?.collectAsState()?.value ?: NoiseControlMode.OFF
    val ancLevel = noiseController?.ancLevel?.collectAsState()?.value ?: 0.85f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        CyberTopAppBar(
            title = "Earbuds Control",
            onBackClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Control your music, calls and more",
                    color = TextGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 3D Earbuds Presentation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Earbuds3DCanvas(
                        size = 200.dp,
                        showParticles = false,
                        showRings = true,
                        autoRotate = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Left / Both / Right Segmented Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    EarbudSideTab.entries.forEach { side ->
                        val isSelected = side == selectedSide
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) CyberCyan else Color.Transparent)
                                .clickable { selectedSide = side }
                                .testTag("side_tab_${side.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = side.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = if (isSelected) Color(0xFF050811) else TextGray,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (noiseController != null) {
                item {
                    NoiseCancellationCard(
                        currentMode = noiseMode,
                        ancLevel = ancLevel,
                        onModeSelected = { noiseController.setMode(it) },
                        onAncLevelChange = { noiseController.setAncLevel(it) }
                    )
                }
            }

            // Media & Call Action Rows
            item {
                ControlActionRow(
                    icon = Icons.Default.PlayArrow,
                    title = "Play / Pause",
                    subtitle = "Single Tap",
                    trailingWidget = {
                        Switch(
                            checked = playPauseEnabled,
                            onCheckedChange = {
                                playPauseEnabled = it
                                actionHandler.executeMappedAction("SINGLE_TAP", selectedSide.name, "PLAY_PAUSE")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyberCyan,
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    },
                    onClick = {
                        actionHandler.executeMappedAction("SINGLE_TAP", selectedSide.name, "PLAY_PAUSE")
                        Toast.makeText(context, "Executing Play/Pause", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                ControlActionRow(
                    icon = Icons.Default.SkipNext,
                    title = "Next Track",
                    subtitle = "Double Tap",
                    onClick = {
                        actionHandler.executeMappedAction("DOUBLE_TAP", selectedSide.name, "NEXT_TRACK")
                        Toast.makeText(context, "Skipped to Next Track", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                ControlActionRow(
                    icon = Icons.Default.SkipPrevious,
                    title = "Previous Track",
                    subtitle = "Triple Tap",
                    onClick = {
                        actionHandler.executeMappedAction("TRIPLE_TAP", selectedSide.name, "PREV_TRACK")
                        Toast.makeText(context, "Rewound to Previous Track", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                ControlActionRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Volume Up",
                    subtitle = "Long Press (R)",
                    onClick = {
                        actionHandler.executeMappedAction("LONG_PRESS", "RIGHT", "VOLUME_UP")
                        Toast.makeText(context, "Volume Increased", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                ControlActionRow(
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    title = "Volume Down",
                    subtitle = "Long Press (L)",
                    onClick = {
                        actionHandler.executeMappedAction("LONG_PRESS", "LEFT", "VOLUME_DOWN")
                        Toast.makeText(context, "Volume Decreased", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                ControlActionRow(
                    icon = Icons.Default.Call,
                    title = "Answer Call",
                    subtitle = "Double Tap",
                    onClick = {
                        Toast.makeText(context, "Call Answer Trigger Active", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                ControlActionRow(
                    icon = Icons.Default.CallEnd,
                    title = "Reject Call",
                    subtitle = "Long Press",
                    iconTint = NeonRed,
                    onClick = {
                        Toast.makeText(context, "Call Reject Trigger Active", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ControlActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = CyberCyan,
    trailingWidget: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("control_row_${title.lowercase().replace(" ", "_")}"),
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
                        .background(Color(0x1800E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }

            if (trailingWidget != null) {
                trailingWidget()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
