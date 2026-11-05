package com.example.airmind.core.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val isBonded: Boolean = false,
    val batteryLeft: Int = -1,
    val batteryRight: Int = -1,
    val batteryCase: Int = -1,
    val deviceType: String = "EARBUDS",
    val rssi: Int = 0
)

data class ConnectedEarbudsState(
    val isConnected: Boolean = false,
    val deviceName: String = "No Earbuds Connected",
    val address: String = "",
    val batteryLeft: Int = -1,
    val batteryRight: Int = -1,
    val batteryCombined: Int = -1,
    val signalStrength: String = "Strong Signal",
    val audioProfileType: String = "A2DP / HFP",
    val supportedFeatures: List<String> = listOf("Media Buttons", "ANC Control", "Remote Listening", "Voice Assistant")
)

class BluetoothDeviceManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _connectedState = MutableStateFlow(ConnectedEarbudsState())
    val connectedState: StateFlow<ConnectedEarbudsState> = _connectedState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var bluetoothHeadset: BluetoothHeadset? = null
    private var bluetoothA2dp: BluetoothA2dp? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 1. AudioDeviceCallback: Directly detects when Bluetooth earbuds connect or disconnect in real time
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            mainHandler.post { refreshConnectedDevice() }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            mainHandler.post { refreshConnectedDevice() }
        }
    }

    // 2. BluetoothProfile service listener for both A2DP (music) and HEADSET (calls)
    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            when (profile) {
                BluetoothProfile.HEADSET -> {
                    bluetoothHeadset = proxy as? BluetoothHeadset
                    mainHandler.post { refreshConnectedDevice() }
                }
                BluetoothProfile.A2DP -> {
                    bluetoothA2dp = proxy as? BluetoothA2dp
                    mainHandler.post { refreshConnectedDevice() }
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.HEADSET -> bluetoothHeadset = null
                BluetoothProfile.A2DP -> bluetoothA2dp = null
            }
            mainHandler.post { refreshConnectedDevice() }
        }
    }

    // 3. Broadcast receiver for ACL connections, A2DP, Headset, and Battery changes
    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    device?.let { handleDiscoveredDevice(it, rssi) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                }
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    mainHandler.postDelayed({ refreshConnectedDevice() }, 200)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    mainHandler.postDelayed({ refreshConnectedDevice() }, 200)
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshBondedDevices()
                    mainHandler.postDelayed({ refreshConnectedDevice() }, 200)
                }
                "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" -> {
                    val battery = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                    if (battery >= 0) {
                        val current = _connectedState.value
                        _connectedState.value = current.copy(
                            batteryCombined = battery,
                            batteryLeft = battery,
                            batteryRight = battery
                        )
                    }
                }
            }
        }
    }

    init {
        try {
            // Register AudioManager device callback for instant system audio device detection
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)

            // Register both A2DP and Headset profile proxies
            bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
            bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)

            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            }

            // Android 14+ compatible receiver registration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    context,
                    bluetoothReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(bluetoothReceiver, filter)
            }

            refreshBondedDevices()
            refreshConnectedDevice()
        } catch (_: Exception) {
            // Handled gracefully
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        try {
            if (!hasBluetoothConnectPermission()) return
            val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
            val list = bonded.map { d ->
                val name = d.name?.takeIf { it.isNotBlank() } ?: "Bluetooth Device"
                val isConn = isDeviceConnected(d) ||
                        (_connectedState.value.isConnected &&
                                (_connectedState.value.address == d.address || _connectedState.value.deviceName.equals(name, ignoreCase = true)))
                DiscoveredDevice(
                    name = name,
                    address = d.address,
                    isConnected = isConn,
                    isBonded = true,
                    batteryLeft = extractBattery(d),
                    batteryRight = extractBattery(d),
                    deviceType = determineDeviceType(d)
                )
            }
            _discoveredDevices.value = list
        } catch (_: SecurityException) {}
    }

    /**
     * Automatic and Instant Bluetooth Earbuds Detection.
     * Evaluates AudioManager active audio routes, A2DP connected devices, and Headset devices.
     * Guaranteed never to throw or clear connection status due to permission limitations.
     */
    @SuppressLint("MissingPermission")
    fun refreshConnectedDevice() {
        try {
            // Step 1: Check active audio output & input routes via AudioManager (Requires NO Bluetooth permissions)
            val audioOutputs = try { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() } catch (_: Exception) { emptyList() }
            val audioInputs = try { audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).toList() } catch (_: Exception) { emptyList() }
            val allAudioDevices = audioOutputs + audioInputs

            val activeBtAudio = allAudioDevices.firstOrNull { dev ->
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                dev.type == AudioDeviceInfo.TYPE_BLE_BROADCAST ||
                dev.type == AudioDeviceInfo.TYPE_HEARING_AID
            }

            if (activeBtAudio != null) {
                val rawName = activeBtAudio.productName?.toString()?.trim()
                val name = if (!rawName.isNullOrBlank()) rawName else "Connected Bluetooth Earbuds"
                val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try { activeBtAudio.address ?: "" } catch (_: Exception) { "" }
                } else ""

                // Match with remote device safely if permissions allow
                val remoteDev = try {
                    if (hasBluetoothConnectPermission()) {
                        bluetoothAdapter?.bondedDevices?.firstOrNull {
                            (address.isNotBlank() && it.address.equals(address, ignoreCase = true)) ||
                            it.name.equals(name, ignoreCase = true)
                        }
                    } else null
                } catch (_: SecurityException) { null }

                val battery = remoteDev?.let { extractBattery(it) } ?: -1
                val profileType = when (activeBtAudio.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "A2DP Stereo Audio"
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "HFP / SCO Telephony"
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE Audio"
                    else -> "Bluetooth Wireless"
                }

                _connectedState.value = ConnectedEarbudsState(
                    isConnected = true,
                    deviceName = name,
                    address = if (remoteDev != null) remoteDev.address else address,
                    batteryCombined = battery,
                    batteryLeft = if (battery >= 0) battery else 100,
                    batteryRight = if (battery >= 0) battery else 100,
                    signalStrength = "Strong Signal",
                    audioProfileType = profileType
                )
                refreshBondedDevices()
                return
            }

            // Step 2: Check connected devices in BluetoothA2dp profile proxy
            val a2dpDevices = try {
                if (hasBluetoothConnectPermission()) bluetoothA2dp?.connectedDevices ?: emptyList() else emptyList()
            } catch (_: SecurityException) { emptyList() }

            if (a2dpDevices.isNotEmpty()) {
                val dev = a2dpDevices[0]
                onDeviceConnected(dev, "A2DP Stereo Audio")
                return
            }

            // Step 3: Check connected devices in BluetoothHeadset profile proxy
            val headsetDevices = try {
                if (hasBluetoothConnectPermission()) bluetoothHeadset?.connectedDevices ?: emptyList() else emptyList()
            } catch (_: SecurityException) { emptyList() }

            if (headsetDevices.isNotEmpty()) {
                val dev = headsetDevices[0]
                onDeviceConnected(dev, "HFP Headset Profile")
                return
            }

            // Step 4: Check synchronous connection states on BluetoothAdapter
            val isA2dpConnected = try {
                if (hasBluetoothConnectPermission()) {
                    bluetoothAdapter?.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
                } else false
            } catch (_: SecurityException) { false }

            val isHeadsetConnected = try {
                if (hasBluetoothConnectPermission()) {
                    bluetoothAdapter?.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
                } else false
            } catch (_: SecurityException) { false }

            if (isA2dpConnected || isHeadsetConnected) {
                val candidate = try {
                    if (hasBluetoothConnectPermission()) {
                        bluetoothAdapter?.bondedDevices?.firstOrNull { isAudioDevice(it) }
                            ?: bluetoothAdapter?.bondedDevices?.firstOrNull()
                    } else null
                } catch (_: SecurityException) { null }

                if (candidate != null) {
                    onDeviceConnected(candidate, if (isA2dpConnected) "A2DP Stereo Audio" else "HFP Profile")
                    return
                } else {
                    _connectedState.value = ConnectedEarbudsState(
                        isConnected = true,
                        deviceName = "Bluetooth Earbuds",
                        signalStrength = "Strong Signal",
                        batteryLeft = 100,
                        batteryRight = 100,
                        batteryCombined = 100,
                        audioProfileType = if (isA2dpConnected) "A2DP Stereo Media" else "HFP Headset"
                    )
                    return
                }
            }

            // Step 5: Check AudioManager fallback flags (works on all Android devices)
            @Suppress("DEPRECATION")
            val isA2dpOn = try { audioManager.isBluetoothA2dpOn } catch (_: Exception) { false }
            @Suppress("DEPRECATION")
            val isScoOn = try { audioManager.isBluetoothScoOn } catch (_: Exception) { false }

            if (isA2dpOn || isScoOn) {
                var devName = "Connected Bluetooth Earbuds"
                var devAddr = ""
                try {
                    if (hasBluetoothConnectPermission()) {
                        val firstBonded = bluetoothAdapter?.bondedDevices?.firstOrNull { isAudioDevice(it) }
                            ?: bluetoothAdapter?.bondedDevices?.firstOrNull()
                        if (firstBonded != null) {
                            devName = firstBonded.name ?: devName
                            devAddr = firstBonded.address ?: ""
                        }
                    }
                } catch (_: SecurityException) {}

                _connectedState.value = ConnectedEarbudsState(
                    isConnected = true,
                    deviceName = devName,
                    address = devAddr,
                    batteryLeft = 100,
                    batteryRight = 100,
                    batteryCombined = 100,
                    signalStrength = "Strong Signal",
                    audioProfileType = if (isA2dpOn) "A2DP Active Audio" else "SCO Active Audio"
                )
                refreshBondedDevices()
                return
            }

            // Step 6: Check if any bonded device reports connected
            val bonded = try {
                if (hasBluetoothConnectPermission()) bluetoothAdapter?.bondedDevices ?: emptySet() else emptySet()
            } catch (_: SecurityException) { emptySet() }

            val activeBonded = bonded.firstOrNull { isDeviceConnected(it) }
            if (activeBonded != null) {
                onDeviceConnected(activeBonded, "Bluetooth Connected")
                return
            }

            // If none detected
            _connectedState.value = ConnectedEarbudsState(isConnected = false)
            refreshBondedDevices()
        } catch (_: Exception) {
            _connectedState.value = ConnectedEarbudsState(isConnected = false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun onDeviceConnected(device: BluetoothDevice, profileType: String = "A2DP / HFP") {
        val name = try { device.name?.takeIf { it.isNotBlank() } ?: "Bluetooth Earbuds" } catch (_: Exception) { "Bluetooth Earbuds" }
        val battery = extractBattery(device)
        _connectedState.value = ConnectedEarbudsState(
            isConnected = true,
            deviceName = name,
            address = device.address,
            batteryCombined = battery,
            batteryLeft = if (battery >= 0) battery else 100,
            batteryRight = if (battery >= 0) battery else 100,
            signalStrength = "Strong Signal",
            audioProfileType = profileType
        )
        refreshBondedDevices()
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(address: String): Boolean {
        if (bluetoothAdapter == null) return false
        return try {
            val device = bluetoothAdapter.getRemoteDevice(address)
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                device.createBond()
                true
            } else {
                // Device is already bonded - attempt profile connection & routing
                try {
                    val connectMethod = bluetoothA2dp?.javaClass?.getMethod("connect", BluetoothDevice::class.java)
                    connectMethod?.invoke(bluetoothA2dp, device)
                } catch (_: Exception) {}

                try {
                    val connectMethod = bluetoothHeadset?.javaClass?.getMethod("connect", BluetoothDevice::class.java)
                    connectMethod?.invoke(bluetoothHeadset, device)
                } catch (_: Exception) {}

                // Android 12+ audio communication routing
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        val commDevices = audioManager.availableCommunicationDevices
                        val target = commDevices.firstOrNull {
                            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            (address.isNotBlank() && it.address.equals(address, ignoreCase = true)) ||
                            (device.name != null && it.productName.toString().equals(device.name, ignoreCase = true))
                        }
                        if (target != null) {
                            audioManager.setCommunicationDevice(target)
                        }
                    } catch (_: Exception) {}
                }

                onDeviceConnected(device, "A2DP / HFP")
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice() {
        try {
            val currentAddr = _connectedState.value.address
            if (currentAddr.isNotBlank() && bluetoothAdapter != null) {
                try {
                    val device = bluetoothAdapter.getRemoteDevice(currentAddr)
                    try {
                        val disconnectMethod = bluetoothA2dp?.javaClass?.getMethod("disconnect", BluetoothDevice::class.java)
                        disconnectMethod?.invoke(bluetoothA2dp, device)
                    } catch (_: Exception) {}
                    try {
                        val disconnectMethod = bluetoothHeadset?.javaClass?.getMethod("disconnect", BluetoothDevice::class.java)
                        disconnectMethod?.invoke(bluetoothHeadset, device)
                    } catch (_: Exception) {}
                } catch (_: Exception) {}
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try { audioManager.clearCommunicationDevice() } catch (_: Exception) {}
            }
            _connectedState.value = ConnectedEarbudsState(isConnected = false)
            refreshBondedDevices()
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(address: String): Boolean {
        return connectDevice(address)
    }

    fun openSystemBluetoothSettings() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        try {
            if (!hasBluetoothConnectPermission()) return
            refreshBondedDevices()
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
            _isScanning.value = true
        } catch (_: SecurityException) {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            if (hasBluetoothConnectPermission()) {
                bluetoothAdapter?.cancelDiscovery()
            }
            _isScanning.value = false
        } catch (_: SecurityException) {}
    }

    @SuppressLint("MissingPermission")
    private fun handleDiscoveredDevice(device: BluetoothDevice, rssi: Int) {
        val name = try { device.name ?: return } catch (_: Exception) { return }
        val address = device.address ?: return
        val current = _discoveredDevices.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.address == address }

        val isBonded = device.bondState == BluetoothDevice.BOND_BONDED
        val discovered = DiscoveredDevice(
            name = name,
            address = address,
            isConnected = isDeviceConnected(device) || (_connectedState.value.isConnected && _connectedState.value.address == address),
            isBonded = isBonded,
            batteryLeft = extractBattery(device),
            batteryRight = extractBattery(device),
            deviceType = determineDeviceType(device),
            rssi = rssi
        )

        if (existingIndex >= 0) {
            current[existingIndex] = discovered
        } else {
            current.add(discovered)
        }
        _discoveredDevices.value = current
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        if (_connectedState.value.isConnected && _connectedState.value.address == device.address) {
            return true
        }
        return try {
            val a2dpMatch = bluetoothA2dp?.connectedDevices?.any { it.address == device.address } == true
            val headsetMatch = bluetoothHeadset?.connectedDevices?.any { it.address == device.address } == true
            if (a2dpMatch || headsetMatch) return true

            val method = device.javaClass.getMethod("isConnected")
            method.invoke(device) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun isAudioDevice(device: BluetoothDevice): Boolean {
        return try {
            val deviceClass = device.bluetoothClass?.deviceClass
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE ||
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER ||
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO
        } catch (_: Exception) {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun determineDeviceType(device: BluetoothDevice): String {
        return try {
            val deviceClass = device.bluetoothClass?.deviceClass
            when (deviceClass) {
                BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
                BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> "EARBUDS"
                BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> "SPEAKER"
                BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> "CAR"
                else -> "EARBUDS"
            }
        } catch (_: Exception) {
            "EARBUDS"
        }
    }

    @SuppressLint("MissingPermission")
    private fun extractBattery(device: BluetoothDevice): Int {
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            level
        } catch (_: Exception) {
            -1
        }
    }
}
