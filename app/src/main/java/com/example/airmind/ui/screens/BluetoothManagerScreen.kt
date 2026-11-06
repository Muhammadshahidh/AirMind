package com.example.airmind.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.airmind.core.bluetooth.BluetoothDeviceManager
import com.example.airmind.ui.components.CyberTopAppBar
import com.example.airmind.ui.components.GlassmorphicCard
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite

@Composable
fun BluetoothManagerScreen(
    deviceManager: BluetoothDeviceManager,
    onBackClick: () -> Unit,
    onNavigateToPairing: () -> Unit
) {
    val context = LocalContext.current
    val connectedState by deviceManager.connectedState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        CyberTopAppBar(
            title = "Bluetooth Manager",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Manage your connected devices",
                color = TextGray,
                fontSize = 13.sp
            )

            // Current Device Hero Card
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bt_manager_device_card"),
                borderColor = if (connectedState.isConnected) CyberCyan.copy(alpha = 0.5f) else CardBorder
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x2200E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = if (connectedState.isConnected) connectedState.deviceName else "No Device Connected",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (connectedState.isConnected) "Connected" else "Disconnected",
                                    color = if (connectedState.isConnected) NeonGreen else TextGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                val bLeft = connectedState.batteryLeft
                                val bRight = connectedState.batteryRight
                                if (connectedState.isConnected && (bLeft != null || bRight != null)) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "L ${bLeft?.let { "$it%" } ?: "--"}  R ${bRight?.let { "$it%" } ?: "--"}",
                                        color = TextGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onNavigateToPairing,
                        modifier = Modifier.testTag("device_manager_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Device Settings",
                            tint = TextGray
                        )
                    }
                }
            }

            // Info rows
            ManagerInfoRow(
                icon = Icons.Default.Tune,
                title = "Paired Devices",
                subtitle = "View all paired earbuds & headphones",
                onClick = onNavigateToPairing
            )

            ManagerInfoRow(
                icon = Icons.Default.Wifi,
                title = "Connection Status",
                subtitle = if (connectedState.isConnected) connectedState.signalStrength else "Disconnected",
                valueColor = NeonGreen
            )

            ManagerInfoRow(
                icon = Icons.Default.BatteryStd,
                title = "Earbuds Battery",
                subtitle = if (connectedState.isConnected) {
                    val bL = connectedState.batteryLeft?.let { "$it%" } ?: "Not Available"
                    val bR = connectedState.batteryRight?.let { "$it%" } ?: "Not Available"
                    "Left: $bL  Right: $bR"
                } else {
                    "Hardware battery telemetry not reported"
                }
            )

            ManagerInfoRow(
                icon = Icons.Default.Notifications,
                title = "Connection Alerts",
                subtitle = "Get notified on connect or disconnect",
                onClick = {
                    Toast.makeText(context, "Connection alerts enabled", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Disconnect / Connect Button
            OutlinedButton(
                onClick = {
                    if (connectedState.isConnected) {
                        deviceManager.disconnectDevice()
                        Toast.makeText(context, "Earbuds disconnected", Toast.LENGTH_SHORT).show()
                    } else {
                        deviceManager.openSystemBluetoothSettings()
                    }
                },
                shape = RoundedCornerShape(26.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (connectedState.isConnected) NeonRed else CardBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (connectedState.isConnected) NeonRed else TextGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("disconnect_button")
            ) {
                Text(
                    text = if (connectedState.isConnected) "Disconnect Earbuds" else "Open Phone Bluetooth Settings",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ManagerInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueColor: Color = TextGray,
    onClick: (() -> Unit)? = null
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = valueColor,
                        fontSize = 12.sp
                    )
                }
            }

            if (onClick != null) {
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
