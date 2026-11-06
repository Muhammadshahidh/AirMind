package com.example.airmind.core.bluetooth

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import com.example.airmind.core.assistant.AiAssistantEngine
import com.example.airmind.core.audio.NoiseCancellationController
import com.example.airmind.core.controls.MobileControlManager
import com.example.airmind.core.database.AirMindDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class EarbudKeyEvent(
    val eventName: String,
    val side: String, // "LEFT", "RIGHT", "BOTH"
    val timestamp: Long = System.currentTimeMillis(),
    val executedAction: String = ""
)

class EarbudActionHandler(
    private val context: Context,
    private val mobileControlManager: MobileControlManager,
    private val database: AirMindDatabase,
    private val noiseCancellationController: NoiseCancellationController? = null,
    private val aiAssistantProvider: (() -> AiAssistantEngine?)? = null
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _recentEvents = MutableSharedFlow<EarbudKeyEvent>(replay = 5)
    val recentEvents: SharedFlow<EarbudKeyEvent> = _recentEvents.asSharedFlow()

    // Multi-tap sequence timing buffer for single-pulse earbuds
    private var tapCount = 0
    private val tapWindowMs = 380L
    private val tapEvaluator = Runnable {
        val count = tapCount
        tapCount = 0
        when (count) {
            1 -> executeMappedAction("SINGLE_TAP", "BOTH", "START_STOP_LISTENING")
            2 -> executeMappedAction("DOUBLE_TAP", "BOTH", "ACTIVATE_AI")
            3 -> executeMappedAction("TRIPLE_TAP", "BOTH", "REPEAT_RESPONSE")
            else -> if (count > 3) executeMappedAction("TRIPLE_TAP", "BOTH", "REPEAT_RESPONSE")
        }
    }

    fun handleMediaKeyEvent(keyEvent: KeyEvent) {
        if (keyEvent.action != KeyEvent.ACTION_DOWN) return

        when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                if (keyEvent.isLongPress || keyEvent.repeatCount > 0) {
                    mainHandler.removeCallbacks(tapEvaluator)
                    tapCount = 0
                    executeMappedAction("LONG_PRESS", "BOTH", "VOICE_ASSISTANT")
                } else {
                    mainHandler.removeCallbacks(tapEvaluator)
                    tapCount++
                    mainHandler.postDelayed(tapEvaluator, tapWindowMs)
                }
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                mainHandler.removeCallbacks(tapEvaluator)
                tapCount = 0
                executeMappedAction("DOUBLE_TAP", "BOTH", "NEXT_TRACK")
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                mainHandler.removeCallbacks(tapEvaluator)
                tapCount = 0
                executeMappedAction("TRIPLE_TAP", "BOTH", "PREV_TRACK")
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                executeMappedAction("LONG_PRESS", "RIGHT", "VOLUME_UP")
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                executeMappedAction("LONG_PRESS", "LEFT", "VOLUME_DOWN")
            }
            KeyEvent.KEYCODE_VOICE_ASSIST -> {
                executeMappedAction("LONG_PRESS", "BOTH", "VOICE_ASSISTANT")
            }
            else -> {
                executeMappedAction("SINGLE_TAP", "BOTH", "START_STOP_LISTENING")
            }
        }
    }

    fun executeMappedAction(eventType: String, side: String, fallbackAction: String) {
        scope.launch {
            val mapping = database.gestureDao().getGesture(eventType, side)
            val actionToRun = if (mapping != null && mapping.isEnabled) mapping.actionType else fallbackAction

            val resultDescription = when (actionToRun) {
                "START_STOP_LISTENING" -> {
                    mainHandler.post {
                        aiAssistantProvider?.invoke()?.toggleListening()
                    }
                    mobileControlManager.vibrateFeedback()
                    "Toggled AI Earbuds Listening"
                }
                "ACTIVATE_AI" -> {
                    mainHandler.post {
                        aiAssistantProvider?.invoke()?.startVoiceListening()
                    }
                    mobileControlManager.vibrateFeedback()
                    "Activated AIRMIND AI (Earbuds Mic)"
                }
                "REPEAT_RESPONSE" -> {
                    mainHandler.post {
                        aiAssistantProvider?.invoke()?.repeatLastResponse()
                    }
                    mobileControlManager.vibrateFeedback()
                    "Repeated Last AI Response"
                }
                "INTERRUPT_AI" -> {
                    mainHandler.post {
                        aiAssistantProvider?.invoke()?.interruptSpeech()
                    }
                    "Interrupted AI Speech"
                }
                "PLAY_PAUSE" -> {
                    dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    "Toggled Music Play/Pause"
                }
                "NEXT_TRACK" -> {
                    dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                    "Skipped to Next Track"
                }
                "PREV_TRACK" -> {
                    dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    "Rewound to Previous Track"
                }
                "VOLUME_UP" -> {
                    mobileControlManager.adjustVolume(1)
                    "Volume Increased (+1)"
                }
                "VOLUME_DOWN" -> {
                    mobileControlManager.adjustVolume(-1)
                    "Volume Decreased (-1)"
                }
                "VOICE_ASSISTANT" -> {
                    mainHandler.post {
                        aiAssistantProvider?.invoke()?.startVoiceListening()
                    }
                    mobileControlManager.vibrateFeedback()
                    "Activated Voice Assistant"
                }
                "ANC_CYCLE" -> {
                    noiseCancellationController?.cycleMode()
                    mobileControlManager.vibrateFeedback()
                    "Cycled Noise Cancellation Mode"
                }
                "TOGGLE_FLASHLIGHT" -> {
                    mobileControlManager.toggleFlashlight()
                    "Toggled Flashlight"
                }
                "NONE" -> {
                    "Gesture Disabled"
                }
                else -> {
                    mainHandler.post {
                        aiAssistantProvider?.invoke()?.toggleListening()
                    }
                    "Action Executed: $actionToRun"
                }
            }

            _recentEvents.emit(
                EarbudKeyEvent(
                    eventName = "$eventType ($side)",
                    side = side,
                    executedAction = resultDescription
                )
            )
        }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        try {
            val eventDown = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 0)
            val eventUp = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)
            audioManager.dispatchMediaKeyEvent(eventDown)
            audioManager.dispatchMediaKeyEvent(eventUp)
        } catch (_: Exception) {}
    }
}
