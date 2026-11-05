package com.example.airmind.ui.screens

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airmind.core.bluetooth.BluetoothDeviceManager
import com.example.airmind.core.bluetooth.BluetoothScanner
import com.example.airmind.core.bluetooth.DiscoveredDevice
import com.example.airmind.ui.components.CyberTopAppBar
import com.example.airmind.ui.components.Earbuds3DCanvas
import com.example.airmind.ui.components.GlassmorphicCard
import com.example.airmind.ui.components.GlowingCyberButton
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun DeviceScannerScreen(
    scanner: BluetoothScanner,
    deviceManager: BluetoothDeviceManager,
    onBackClick: () -> Unit,
    onDeviceSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val discoveredDevices by scanner.discoveredDevices.collectAsState()
    val isScanning by scanner.isScanning.collectAsState()
    val hasPermissions by scanner.hasPermissions.collectAsState()
    val connectedState by deviceManager.connectedState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        scanner.updatePermissionStatus()
        val allGranted = results.values.all { it }
        if (allGranted) {
            scanner.startScanning()
        } else {
            Toast.makeText(context, "Bluetooth scan permissions are required to detect nearby earbuds.", Toast.LENGTH_LONG).show()
        }
    }

    // Auto-request runtime permissions & start scanning directly
    LaunchedEffect(Unit) {
        if (!scanner.hasRequiredPermissions()) {
            permissionLauncher.launch(scanner.getRequiredPermissions())
        } else {
            scanner.startScanning()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.stopScanning()
        }
    }

    val transition = rememberInfiniteTransition(label = "radar_sweep")
    val radarPulse by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CyberTopAppBar(
                title = "Connect Your Earbuds",
                onBackClick = onBackClick
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Make sure your earbuds are in pairing mode.",
                        color = TextGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 3D AirBuds Animated Presentation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0C1424))
                            .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Earbuds3DCanvas(
                            size = 170.dp,
                            showParticles = true,
                            showRings = true,
                            autoRotate = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!hasPermissions) {
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            borderColor = NeonRed
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Bluetooth Permissions Required",
                                    color = NeonRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Grant runtime scan permissions to detect nearby available earbuds directly.",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        permissionLauncher.launch(scanner.getRequiredPermissions())
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                                ) {
                                    Text("Grant Permissions", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Bluetooth Devices (${discoveredDevices.size})",
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (isScanning) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scanning...", color = CyberCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (discoveredDevices.isEmpty()) {
                    item {
                        GlassmorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp * (if (isScanning) radarPulse else 1f))
                                        .clip(CircleShape)
                                        .background(Color(0x1800E5FF))
                                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothSearching,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (isScanning) "Scanning nearby Bluetooth earbuds..." else "No Bluetooth devices found yet",
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "1. Open your earbuds charging case lid.\n2. Press and hold the case pairing button until the light flashes.\n3. Tap 'Scan Again' below.",
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                                ) {
                                    Text("Open System Bluetooth Settings", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    items(discoveredDevices) { device ->
                        RealDeviceItemCard(
                            device = device,
                            onConnectClick = {
                                if (device.isConnected) {
                                    onDeviceSelected(device.name)
                                } else {
                                    val paired = deviceManager.pairDevice(device.address)
                                    if (paired) {
                                        Toast.makeText(context, "Pairing with ${device.name}...", Toast.LENGTH_SHORT).show()
                                        onDeviceSelected(device.name)
                                    } else {
                                        try {
                                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Bottom Scan Again Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                GlowingCyberButton(
                    text = if (isScanning) "Scanning Bluetooth..." else "Scan Again",
                    onClick = {
                        if (isScanning) {
                            scanner.stopScanning()
                        } else {
                            if (!scanner.hasRequiredPermissions()) {
                                permissionLauncher.launch(scanner.getRequiredPermissions())
                            } else {
                                scanner.startScanning()
                            }
                        }
                    },
                    testTag = "scan_again_button"
                )
            }
        }
    }
}

@Composable
private fun RealDeviceItemCard(
    device: DiscoveredDevice,
    onConnectClick: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (device.isConnected) CyberCyan.copy(alpha = 0.5f) else CardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (device.isConnected) Color(0x3300E5FF) else Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = if (device.isConnected) CyberCyan else TextGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = device.name,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (device.isConnected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Connected",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (device.batteryLeft >= 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "L ${device.batteryLeft}%  R ${device.batteryRight}%",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else if (device.isBonded) {
                        Text(
                            text = "Paired Device",
                            color = CyberCyan,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "Available to Pair (${device.address})",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (device.isConnected) {
                IconButton(
                    onClick = onConnectClick,
                    modifier = Modifier.testTag("device_settings_${device.address}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = NeonGreen
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onConnectClick,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    modifier = Modifier.testTag("connect_btn_${device.address}")
                ) {
                    Text(if (device.isBonded) "Select" else "Connect", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
