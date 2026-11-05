package com.example.airmind.core.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _hasPermissions = MutableStateFlow(hasRequiredPermissions())
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private var isReceiverRegistered = false

    // BLE scan callback
    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                handleDeviceFound(device, result.rssi)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result ->
                result.device?.let { handleDeviceFound(it, result.rssi) }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }

    // Classic Bluetooth discovery broadcast receiver
    private val discoveryReceiver = object : BroadcastReceiver() {
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
                    device?.let { handleDeviceFound(it, rssi) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun hasRequiredPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun updatePermissionStatus() {
        _hasPermissions.value = hasRequiredPermissions()
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        updatePermissionStatus()
        if (!_hasPermissions.value) {
            _isScanning.value = false
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _isScanning.value = false
            return
        }

        // Register Classic Bluetooth discovery receiver if not registered
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.registerReceiver(
                        context,
                        discoveryReceiver,
                        filter,
                        ContextCompat.RECEIVER_EXPORTED
                    )
                } else {
                    context.registerReceiver(discoveryReceiver, filter)
                }
                isReceiverRegistered = true
            } catch (_: Exception) {}
        }

        // Pre-populate with already bonded (paired) devices so user can immediately see and connect them
        try {
            val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
            val currentList = _discoveredDevices.value.toMutableList()
            bonded.forEach { dev ->
                val name = dev.name
                val addr = dev.address
                if (!name.isNullOrBlank() && currentList.none { it.address == addr }) {
                    currentList.add(
                        DiscoveredDevice(
                            name = name,
                            address = addr,
                            isConnected = false,
                            isBonded = true,
                            deviceType = determineDeviceType(dev)
                        )
                    )
                }
            }
            _discoveredDevices.value = currentList
        } catch (_: SecurityException) {}

        // Cancel previous discovery if active
        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
            _isScanning.value = true
        } catch (_: SecurityException) {}

        // Start BLE scanning in parallel for BLE audio accessories
        try {
            val leScanner = bluetoothAdapter.bluetoothLeScanner
            leScanner?.startScan(leScanCallback)
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!_hasPermissions.value) return
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
        } catch (_: Exception) {}
        _isScanning.value = false
    }

    fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }

    fun cleanup() {
        stopScanning()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleDeviceFound(device: BluetoothDevice, rssi: Int) {
        val name = try { device.name } catch (_: Exception) { null }
        val address = device.address ?: return

        // Skip unnamed signals or beacons without human readable identifier
        val displayName = if (name.isNullOrBlank()) "Bluetooth Audio ($address)" else name

        val currentList = _discoveredDevices.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.address == address }

        val isBonded = try { device.bondState == BluetoothDevice.BOND_BONDED } catch (_: Exception) { false }
        val deviceType = determineDeviceType(device)

        val discovered = DiscoveredDevice(
            name = displayName,
            address = address,
            isConnected = false,
            isBonded = isBonded,
            deviceType = deviceType,
            rssi = rssi
        )

        if (existingIndex >= 0) {
            currentList[existingIndex] = discovered
        } else {
            currentList.add(discovered)
        }
        _discoveredDevices.value = currentList
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
}
