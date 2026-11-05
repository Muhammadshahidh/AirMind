package com.example.airmind.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import com.example.airmind.ui.components.Earbuds3DInspectionDialog
import com.example.airmind.ui.components.Earbuds3DSceneView
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.airmind.core.audio.NoiseCancellationController
import com.example.airmind.core.audio.NoiseControlMode
import com.example.airmind.core.bluetooth.BluetoothDeviceManager
import com.example.airmind.core.bluetooth.ConnectedEarbudsState
import com.example.airmind.core.controls.OrientationSensorManager
import com.example.airmind.ui.components.AirMindBottomNavigation
import com.example.airmind.ui.components.Earbuds3DSceneView
import com.example.airmind.ui.components.GlassmorphicCard
import com.example.airmind.ui.components.GlowingCyberButton
import com.example.airmind.ui.components.NavigationTab
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardBorderGlow
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanLight
import com.example.ui.theme.DeepNeonBlue
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite

@Composable
fun MainDashboardScreen(
    deviceManager: BluetoothDeviceManager,
    noiseController: NoiseCancellationController,
    sensorManager: OrientationSensorManager,
    onNavigateToConnect: () -> Unit,
    onNavigateToEarbudsControl: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToListening: () -> Unit,
    onNavigateToGestures: () -> Unit,
    onNavigateToBluetoothManager: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val connectedState by deviceManager.connectedState.collectAsState()
    val noiseMode by noiseController.currentMode.collectAsState()
    val ancLevel by noiseController.ancLevel.collectAsState()
    val orientation by sensorManager.orientationAngles.collectAsState()

    var hasBluetoothPermission by remember { mutableStateOf(deviceManager.hasBluetoothConnectPermission()) }
    var show3DInspectionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasBluetoothPermission = deviceManager.hasBluetoothConnectPermission()
        deviceManager.refreshConnectedDevice()
        if (hasBluetoothPermission) {
            Toast.makeText(context, "Bluetooth permission granted! Detecting earbuds...", Toast.LENGTH_SHORT).show()
        }
    }

    // Automatically check for already connected Bluetooth earbuds & prompt permission on first entry
    LaunchedEffect(Unit) {
        deviceManager.refreshConnectedDevice()
        if (!deviceManager.hasBluetoothConnectPermission()) {
            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
            } else {
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
            }
            permissionLauncher.launch(perms)
        }
    }

    // Auto-detect when app comes back to foreground (e.g. after pairing in phone Bluetooth settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasBluetoothPermission = deviceManager.hasBluetoothConnectPermission()
                deviceManager.refreshConnectedDevice()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_assistant")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
    ) {
        // App Header: AIRMIND AI [Pro] and Settings icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AIRMIND AI",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(listOf(ElectricBlue, CyberCyan))
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Pro",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x18FFFFFF))
                    .testTag("dashboard_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Main scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasBluetoothPermission) {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonPurple.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bluetooth Permission Needed",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Grant Bluetooth permission so AIRMIND AI can auto-detect your connected earbuds.",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                                } else {
                                    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                                permissionLauncher.launch(perms)
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                        ) {
                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Connected Earbuds Status Card (Auto-detects active Bluetooth connection)
            ConnectedStatusCard(
                state = connectedState,
                onConnectClick = onNavigateToConnect,
                onOpenSettings = { deviceManager.openSystemBluetoothSettings() },
                onRefresh = {
                    deviceManager.refreshConnectedDevice()
                    Toast.makeText(context, "Scanning active Bluetooth audio routes...", Toast.LENGTH_SHORT).show()
                }
            )

            // High-Fidelity 3D Earbuds Model with Interactive AR SceneView Orbit & Inspection
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sceneview_earbuds_card"),
                onClick = { show3DInspectionDialog = true }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (connectedState.isConnected) connectedState.deviceName else "3D Earbuds Preview",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 3D Motion Orientation Badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x1F00E5FF))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MotionPhotosOn,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tilt: ${orientation.roll.toInt()}°",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Interactive 3D AR Inspect Studio Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x3300E5FF))
                                    .border(1.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .clickable { show3DInspectionDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ViewInAr,
                                        contentDescription = "Inspect 3D",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Inspect 3D",
                                        color = CyberCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Render Orientation-Tracked 3D Earbuds
                    Earbuds3DSceneView(
                        sensorManager = sensorManager,
                        size = 200.dp,
                        showRings = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x0CFFFFFF))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tap to open 3D AR Studio • 360° Orbit & Hardware Inspection",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (show3DInspectionDialog) {
                Earbuds3DInspectionDialog(
                    deviceName = if (connectedState.isConnected) connectedState.deviceName else "AIRMIND AI Earbuds",
                    sensorManager = sensorManager,
                    onDismiss = { show3DInspectionDialog = false }
                )
            }

            // Noise Cancellation Controller Service Card (ANC / Transparency / Off)
            NoiseCancellationCard(
                currentMode = noiseMode,
                ancLevel = ancLevel,
                onModeSelected = { mode ->
                    noiseController.setMode(mode)
                    Toast.makeText(context, "${mode.displayName} Activated", Toast.LENGTH_SHORT).show()
                },
                onAncLevelChange = { noiseController.setAncLevel(it) }
            )

            // AI Assistant Glowing Banner Card
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_ai_banner"),
                borderColor = CyberCyan.copy(alpha = 0.5f),
                onClick = onNavigateToAssistant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing AI Orb Avatar
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(CyberCyanLight, DeepNeonBlue, Color(0xFF0A0F24))
                                )
                            )
                            .border(2.dp, CyberCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "AI Assistant",
                            color = TextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap to talk with your AI",
                            color = CyberCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2x3 Feature Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DashboardTile(
                    title = "Earbuds Control",
                    subtitle = "Media, Calls & ANC",
                    icon = Icons.Default.Headphones,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToEarbudsControl,
                    testTag = "tile_earbuds_control"
                )
                DashboardTile(
                    title = "Remote Listening",
                    subtitle = "Listen Around You",
                    icon = Icons.Default.Hearing,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToListening,
                    testTag = "tile_remote_listening"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DashboardTile(
                    title = "Gestures",
                    subtitle = "Customize Touch",
                    icon = Icons.Default.Gesture,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToGestures,
                    testTag = "tile_gestures"
                )
                DashboardTile(
                    title = "Bluetooth Manager",
                    subtitle = "Devices & Battery",
                    icon = Icons.Default.Bluetooth,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToBluetoothManager,
                    testTag = "tile_bluetooth_manager"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DashboardTile(
                    title = "Automation",
                    subtitle = "Smart Actions",
                    icon = Icons.Default.AutoMode,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAutomation,
                    testTag = "tile_automation"
                )
                DashboardTile(
                    title = "Settings",
                    subtitle = "App & Permissions",
                    icon = Icons.Default.Settings,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSettings,
                    testTag = "tile_settings"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Bottom Navigation Bar
        AirMindBottomNavigation(
            currentTab = NavigationTab.HOME,
            onTabSelected = { tab ->
                when (tab) {
                    NavigationTab.HOME -> { /* already on home */ }
                    NavigationTab.ASSISTANT -> onNavigateToAssistant()
                    NavigationTab.LISTEN -> onNavigateToListening()
                    NavigationTab.PROFILE -> onNavigateToSettings()
                }
            }
        )
    }
}

@Composable
private fun ConnectedStatusCard(
    state: ConnectedEarbudsState,
    onConnectClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_connected_card"),
        borderColor = if (state.isConnected) CyberCyan.copy(alpha = 0.5f) else CardBorder,
        onClick = onConnectClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (state.isConnected) NeonGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isConnected) "Connected (${state.audioProfileType})" else "No Bluetooth Earbuds",
                        color = if (state.isConnected) NeonGreen else TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Bluetooth Audio",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isConnected) state.deviceName else "Connect Your Earbuds",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (state.isConnected) {
                        if (state.batteryLeft >= 0 || state.batteryRight >= 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.batteryLeft >= 0) BatteryBadge(label = "L", percent = state.batteryLeft)
                                if (state.batteryRight >= 0) BatteryBadge(label = "R", percent = state.batteryRight)
                            }
                        } else {
                            Text(
                                text = "Standard Bluetooth Audio Active",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = "Already connected in phone settings? Tap refresh or connect below to link AIRMIND AI.",
                            color = TextGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (state.isConnected) Color(0x3300E5FF) else Color(0x1800E5FF))
                        .border(1.dp, CardBorderGlow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isConnected) Icons.Default.BluetoothConnected else Icons.Default.Headphones,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (!state.isConnected) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        GlowingCyberButton(
                            text = "Scan Earbuds",
                            onClick = onConnectClick,
                            testTag = "quick_connect_earbuds_btn"
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(26.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(50.dp)
                            .testTag("open_system_bt_btn")
                    ) {
                        Text("Phone Bluetooth", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun NoiseCancellationCard(
    currentMode: NoiseControlMode,
    ancLevel: Float,
    onModeSelected: (NoiseControlMode) -> Unit,
    onAncLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = when (currentMode) {
            NoiseControlMode.ANC -> ElectricBlue.copy(alpha = 0.6f)
            NoiseControlMode.TRANSPARENCY -> CyberCyan.copy(alpha = 0.6f)
            NoiseControlMode.OFF -> CardBorder
        }
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (currentMode) {
                                    NoiseControlMode.ANC -> Color(0x330080FF)
                                    NoiseControlMode.TRANSPARENCY -> Color(0x3300E5FF)
                                    NoiseControlMode.OFF -> Color(0x1F94A3B8)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (currentMode) {
                                NoiseControlMode.ANC -> Icons.AutoMirrored.Filled.VolumeMute
                                NoiseControlMode.TRANSPARENCY -> Icons.Default.Hearing
                                NoiseControlMode.OFF -> Icons.Default.GraphicEq
                            },
                            contentDescription = null,
                            tint = when (currentMode) {
                                NoiseControlMode.ANC -> ElectricBlue
                                NoiseControlMode.TRANSPARENCY -> CyberCyan
                                NoiseControlMode.OFF -> TextGray
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Noise Control",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentMode.displayName,
                            color = when (currentMode) {
                                NoiseControlMode.ANC -> ElectricBlue
                                NoiseControlMode.TRANSPARENCY -> CyberCyan
                                NoiseControlMode.OFF -> TextGray
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Mode Segmented Pill Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A0F1D))
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NoiseControlMode.entries.forEach { mode ->
                    val isSelected = mode == currentMode
                    val activeBg = when (mode) {
                        NoiseControlMode.ANC -> ElectricBlue
                        NoiseControlMode.TRANSPARENCY -> CyberCyan
                        NoiseControlMode.OFF -> Color(0xFF334155)
                    }
                    val textColor = if (isSelected) Color(0xFF030712) else TextGray

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) activeBg else Color.Transparent)
                            .clickable { onModeSelected(mode) }
                            .testTag("noise_mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                NoiseControlMode.ANC -> "ANC"
                                NoiseControlMode.TRANSPARENCY -> "Transparency"
                                NoiseControlMode.OFF -> "Off"
                            },
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = currentMode.description,
                color = TextGray,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            if (currentMode == NoiseControlMode.ANC) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Suppression Strength", color = TextGray, fontSize = 11.sp)
                    Text(text = "${(ancLevel * 100).toInt()}%", color = ElectricBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = ancLevel,
                    onValueChange = onAncLevelChange,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricBlue,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("anc_strength_slider")
                )
            }
        }
    }
}

@Composable
private fun BatteryBadge(label: String, percent: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x1400E5FF))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label ",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$percent%",
            color = TextWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DashboardTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    GlassmorphicCard(
        modifier = modifier
            .height(130.dp)
            .testTag(testTag),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x1F00E5FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
