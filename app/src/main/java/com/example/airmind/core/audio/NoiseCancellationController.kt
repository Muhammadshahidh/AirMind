package com.example.airmind.core.audio

import android.content.Context
import android.media.AudioManager
import com.example.airmind.core.controls.MobileControlManager
import com.example.airmind.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class NoiseControlMode(val displayName: String, val description: String) {
    ANC("Active Noise Cancelling", "Blocks external noise and ambient rumble"),
    TRANSPARENCY("Transparency Mode", "Passes through natural environmental sound & voices"),
    OFF("Noise Control Off", "Standard neutral passive isolation")
}

class NoiseCancellationController(
    private val context: Context,
    private val listeningEngine: RemoteListeningEngine,
    private val mobileControlManager: MobileControlManager,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentMode = MutableStateFlow(NoiseControlMode.OFF)
    val currentMode: StateFlow<NoiseControlMode> = _currentMode.asStateFlow()

    private val _ancLevel = MutableStateFlow(1.0f) // 0.0 to 1.0 intensity
    val ancLevel: StateFlow<Float> = _ancLevel.asStateFlow()

    @Volatile
    private var hasExplicitModeSet = false

    init {
        // Load initial state once from preferences if mode was not already explicitly set
        scope.launch {
            try {
                val initial = preferencesRepository.remoteListeningConfig.first()
                if (!hasExplicitModeSet) {
                    if (initial.noiseReduction && !initial.voiceEnhancement) {
                        _currentMode.value = NoiseControlMode.ANC
                    } else if (initial.voiceEnhancement) {
                        _currentMode.value = NoiseControlMode.TRANSPARENCY
                    } else {
                        _currentMode.value = NoiseControlMode.OFF
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun setMode(mode: NoiseControlMode) {
        hasExplicitModeSet = true
        _currentMode.value = mode
        mobileControlManager.vibrateFeedback()

        when (mode) {
            NoiseControlMode.ANC -> {
                // Enable high-grade noise reduction DSP
                listeningEngine.isNoiseReductionEnabled = true
                listeningEngine.isVoiceEnhancementEnabled = false
                listeningEngine.updateSettings(
                    volume = 0.8f,
                    balance = 0.0f,
                    noiseReduction = true,
                    voiceEnhance = false
                )
                scope.launch {
                    preferencesRepository.setNoiseReduction(true)
                    preferencesRepository.setVoiceEnhancement(false)
                }
            }
            NoiseControlMode.TRANSPARENCY -> {
                // Activate acoustic pass-through with voice frequency enhancement
                listeningEngine.isNoiseReductionEnabled = false
                listeningEngine.isVoiceEnhancementEnabled = true
                listeningEngine.updateSettings(
                    volume = 1.0f,
                    balance = 0.0f,
                    noiseReduction = false,
                    voiceEnhance = true
                )
                scope.launch {
                    preferencesRepository.setNoiseReduction(false)
                    preferencesRepository.setVoiceEnhancement(true)
                }
            }
            NoiseControlMode.OFF -> {
                // Neutral mode
                listeningEngine.isNoiseReductionEnabled = false
                listeningEngine.isVoiceEnhancementEnabled = false
                listeningEngine.updateSettings(
                    volume = 0.7f,
                    balance = 0.0f,
                    noiseReduction = false,
                    voiceEnhance = false
                )
                scope.launch {
                    preferencesRepository.setNoiseReduction(false)
                    preferencesRepository.setVoiceEnhancement(false)
                }
            }
        }
    }

    fun toggleANC() {
        if (_currentMode.value == NoiseControlMode.ANC) {
            setMode(NoiseControlMode.OFF)
        } else {
            setMode(NoiseControlMode.ANC)
        }
    }

    fun toggleTransparency() {
        if (_currentMode.value == NoiseControlMode.TRANSPARENCY) {
            setMode(NoiseControlMode.OFF)
        } else {
            setMode(NoiseControlMode.TRANSPARENCY)
        }
    }

    fun turnOff() {
        setMode(NoiseControlMode.OFF)
    }

    fun cycleMode() {
        val nextMode = when (_currentMode.value) {
            NoiseControlMode.OFF -> NoiseControlMode.ANC
            NoiseControlMode.ANC -> NoiseControlMode.TRANSPARENCY
            NoiseControlMode.TRANSPARENCY -> NoiseControlMode.OFF
        }
        setMode(nextMode)
    }

    fun setAncLevel(level: Float) {
        _ancLevel.value = level.coerceIn(0f, 1f)
    }
}
