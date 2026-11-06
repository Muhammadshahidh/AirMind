package com.example.airmind.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothAudioRouteManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var audioFocusRequest: AudioFocusRequest? = null
    private var isRouteActive = false

    private val _activeRouteInfo = MutableStateFlow("Disconnected")
    val activeRouteInfo: StateFlow<String> = _activeRouteInfo.asStateFlow()

    private val _isEarbudMicActive = MutableStateFlow(false)
    val isEarbudMicActive: StateFlow<Boolean> = _isEarbudMicActive.asStateFlow()

    /**
     * Activates audio routing specifically for Bluetooth earbuds.
     * @param forRecording When true, prioritizes Bluetooth SCO / BLE headset microphone input
     *                     and puts AudioManager into MODE_IN_COMMUNICATION.
     */
    fun activateEarbudsRoute(forRecording: Boolean = false): Boolean {
        try {
            requestAudioFocus()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ Communication Device Routing
                val commDevices = audioManager.availableCommunicationDevices
                val btDevice = commDevices.firstOrNull { dev ->
                    dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                }

                if (btDevice != null) {
                    if (forRecording) {
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    } else {
                        audioManager.mode = AudioManager.MODE_NORMAL
                    }
                    val success = audioManager.setCommunicationDevice(btDevice)
                    _isEarbudMicActive.value = forRecording && success
                    _activeRouteInfo.value = "${btDevice.productName} (${if (forRecording) "Microphone In" else "Audio Out"})"
                    isRouteActive = true
                    return true
                }
            } else {
                // Android 11 and below SCO handling
                if (forRecording) {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    @Suppress("DEPRECATION")
                    audioManager.startBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = true
                    _isEarbudMicActive.value = true
                    _activeRouteInfo.value = "Bluetooth SCO Active (Legacy)"
                } else {
                    audioManager.mode = AudioManager.MODE_NORMAL
                    @Suppress("DEPRECATION")
                    audioManager.stopBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = false
                    _isEarbudMicActive.value = false
                    _activeRouteInfo.value = "Bluetooth A2DP Stereo"
                }
                isRouteActive = true
                return true
            }

            _activeRouteInfo.value = "Phone Fallback Route"
            return false
        } catch (e: Exception) {
            Log.e("BluetoothAudioRoute", "Failed to activate audio route", e)
            _activeRouteInfo.value = "Route Error: ${e.message}"
            return false
        }
    }

    /**
     * Deactivates communication routing and restores system audio mode.
     */
    fun deactivateEarbudsRoute() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
            }
            audioManager.mode = AudioManager.MODE_NORMAL
            abandonAudioFocus()
            isRouteActive = false
            _isEarbudMicActive.value = false
            _activeRouteInfo.value = "Normal Media Route"
        } catch (e: Exception) {
            Log.e("BluetoothAudioRoute", "Failed to deactivate audio route", e)
        }
    }

    /**
     * Checks if a Bluetooth microphone (SCO, BLE Headset) is physically connected and available.
     */
    fun isBluetoothMicrophoneAvailable(): Boolean {
        return try {
            val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            inputs.any { dev ->
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if Bluetooth audio output (A2DP, SCO, BLE) is currently connected.
     */
    fun isBluetoothOutputAvailable(): Boolean {
        return try {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            outputs.any { dev ->
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                dev.type == AudioDeviceInfo.TYPE_HEARING_AID
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            deactivateEarbudsRoute()
                        }
                    }
                    .build()

                audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }
        } catch (_: Exception) {}
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (_: Exception) {}
    }
}
