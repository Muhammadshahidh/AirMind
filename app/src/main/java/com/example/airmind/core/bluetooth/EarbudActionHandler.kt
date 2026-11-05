package com.example.airmind.core.bluetooth

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import com.example.airmind.core.controls.MobileControlManager
import com.example.airmind.core.database.AirMindDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class EarbudKeyEvent(
    val eventName: String, // "Single Tap (Play/Pause)", "Double Tap (Next)", "Triple Tap (Prev)", etc.
    val side: String, // "LEFT", "RIGHT", "BOTH"
    val timestamp: Long = System.currentTimeMillis(),
    val executedAction: String = ""
)

class EarbudActionHandler(
    private val context: Context,
    private val mobileControlManager: MobileControlManager,
    private val database: AirMindDatabase,
    private val noiseCancellationController: com.example.airmind.core.audio.NoiseCancellationController? = null
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _recentEvents = MutableSharedFlow<EarbudKeyEvent>(replay = 5)
    val recentEvents: SharedFlow<EarbudKeyEvent> = _recentEvents.asSharedFlow()

    fun handleMediaKeyEvent(keyEvent: KeyEvent) {
        if (keyEvent.action != KeyEvent.ACTION_DOWN) return

        val (eventType, defaultAction) = when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> "SINGLE_TAP" to "PLAY_PAUSE"
            KeyEvent.KEYCODE_MEDIA_PLAY -> "SINGLE_TAP" to "PLAY"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "SINGLE_TAP" to "PAUSE"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "DOUBLE_TAP" to "NEXT_TRACK"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "TRIPLE_TAP" to "PREV_TRACK"
            KeyEvent.KEYCODE_VOLUME_UP -> "LONG_PRESS" to "VOLUME_UP"
            KeyEvent.KEYCODE_VOLUME_DOWN -> "LONG_PRESS" to "VOLUME_DOWN"
            else -> "SINGLE_TAP" to "PLAY_PAUSE"
        }

        executeMappedAction(eventType, "BOTH", defaultAction)
    }

    fun executeMappedAction(eventType: String, side: String, fallbackAction: String) {
        scope.launch {
            val mapping = database.gestureDao().getGesture(eventType, side)
            val actionToRun = if (mapping != null && mapping.isEnabled) mapping.actionType else fallbackAction

            val resultDescription = when (actionToRun) {
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
                    mobileControlManager.triggerVoiceAssistant()
                    "Activated AIRMIND AI Assistant"
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
                    dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    "Media Action Executed"
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
