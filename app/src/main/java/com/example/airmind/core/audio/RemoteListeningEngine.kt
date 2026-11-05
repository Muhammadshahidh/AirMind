package com.example.airmind.core.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

class RemoteListeningEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null

    private var audioJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Configurable runtime parameters
    var volumeMultiplier: Float = 1.0f
    var leftBalance: Float = 1.0f
    var rightBalance: Float = 1.0f
    var isNoiseReductionEnabled: Boolean = true
    var isVoiceEnhancementEnabled: Boolean = true

    fun updateSettings(volume: Float, balance: Float, noiseReduction: Boolean, voiceEnhance: Boolean) {
        volumeMultiplier = volume * 1.5f // Scaled digital boost
        // balance ranges from -1.0 (full left) to +1.0 (full right)
        leftBalance = if (balance > 0f) 1f - balance else 1f
        rightBalance = if (balance < 0f) 1f + balance else 1f
        isNoiseReductionEnabled = noiseReduction
        isVoiceEnhancementEnabled = voiceEnhance

        noiseSuppressor?.enabled = noiseReduction
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun startListening(): Boolean {
        if (_isListening.value) return true

        val sampleRate = 44100
        val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
        val channelConfigOut = AudioFormat.CHANNEL_OUT_STEREO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT

        val minBufferSizeIn = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioEncoding)
        val minBufferSizeOut = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioEncoding)

        val bufferSizeIn = max(minBufferSizeIn, 4096)
        val bufferSizeOut = max(minBufferSizeOut, 8192)

        try {
            // Enable Bluetooth SCO or communication device if headset is connected
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val commDevices = audioManager.availableCommunicationDevices
                val target = commDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                }
                if (target != null) {
                    audioManager.setCommunicationDevice(target)
                }
            } else {
                @Suppress("DEPRECATION")
                if (audioManager.isBluetoothScoAvailableOffCall) {
                    @Suppress("DEPRECATION")
                    audioManager.startBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = true
                }
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfigIn,
                audioEncoding,
                bufferSizeIn
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to standard MIC
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfigIn,
                    audioEncoding,
                    bufferSizeIn
                )
            }

            val sessionId = audioRecord?.audioSessionId ?: 0
            if (sessionId != 0 && NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                    enabled = isNoiseReductionEnabled
                }
            }
            if (sessionId != 0 && AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                    enabled = true
                }
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(audioEncoding)
                .setChannelMask(channelConfigOut)
                .build()

            audioTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSizeOut,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioRecord?.startRecording()
            audioTrack?.play()
            _isListening.value = true

            // Streaming processing loop
            audioJob = scope.launch {
                val inputBuffer = ShortArray(bufferSizeIn / 2)
                val outputBuffer = ShortArray(inputBuffer.size * 2) // Mono to stereo

                var previousSample = 0

                while (isActive && _isListening.value) {
                    val readSamples = audioRecord?.read(inputBuffer, 0, inputBuffer.size) ?: 0
                    if (readSamples > 0) {
                        var maxAmp = 0

                        for (i in 0 until readSamples) {
                            var sample = inputBuffer[i].toInt()

                            // Voice enhancement high-pass / pre-emphasis filter
                            if (isVoiceEnhancementEnabled) {
                                val filtered = sample - (0.95f * previousSample).toInt()
                                previousSample = sample
                                sample = filtered
                            }

                            // Volume boost with soft-clipping protection
                            val boosted = (sample * volumeMultiplier).toInt()
                            val clamped = boosted.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                            // Track amplitude for visualizer
                            val absVal = abs(clamped)
                            if (absVal > maxAmp) maxAmp = absVal

                            // Stereo channel panning (L / R)
                            val leftSample = (clamped * leftBalance).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            val rightSample = (clamped * rightBalance).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

                            outputBuffer[i * 2] = leftSample.toShort()
                            outputBuffer[i * 2 + 1] = rightSample.toShort()
                        }

                        // Write to playback track
                        audioTrack?.write(outputBuffer, 0, readSamples * 2)

                        // Update meter (normalized 0.0 to 1.0)
                        val normalizedAmp = (maxAmp.toFloat() / Short.MAX_VALUE).coerceIn(0f, 1f)
                        _audioAmplitude.value = normalizedAmp
                    }
                }
            }

            return true
        } catch (_: Exception) {
            stopListening()
            return false
        }
    }

    @Synchronized
    fun stopListening() {
        _isListening.value = false
        audioJob?.cancel()
        audioJob = null

        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
            echoCanceler?.release()
            echoCanceler = null

            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                if (audioManager.isBluetoothScoOn) {
                    @Suppress("DEPRECATION")
                    audioManager.stopBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = false
                }
            }
        } catch (_: Exception) {}

        _audioAmplitude.value = 0f
    }
}
