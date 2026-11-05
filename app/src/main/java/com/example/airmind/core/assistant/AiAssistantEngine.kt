package com.example.airmind.core.assistant

import android.content.Context
import com.example.airmind.core.bluetooth.BluetoothDeviceManager
import com.example.airmind.core.controls.MobileControlManager
import com.example.airmind.core.database.AirMindDatabase
import com.example.airmind.core.database.entities.VoiceNoteEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AssistantMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AiAssistantEngine(
    private val context: Context,
    private val speechManager: SpeechManager,
    private val mobileControlManager: MobileControlManager,
    private val bluetoothDeviceManager: BluetoothDeviceManager,
    private val database: AirMindDatabase
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _messages = MutableStateFlow<List<AssistantMessage>>(
        listOf(
            AssistantMessage(
                text = "Hello! I'm your AIRMIND AI assistant. You can speak commands, control your phone, or ask questions.",
                isUser = false
            )
        )
    )
    val messages: StateFlow<List<AssistantMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val generativeModel by lazy {
        try {
            Firebase.ai.generativeModel("gemini-3.8-flash")
        } catch (_: Throwable) {
            null
        }
    }

    fun processUserQuery(input: String, speakReply: Boolean = true) {
        val query = input.trim()
        if (query.isBlank()) return

        // Add user message to UI
        val userMsg = AssistantMessage(text = query, isUser = true)
        _messages.value = _messages.value + userMsg

        // Check if query matches local offline commands first
        val localCommandHandled = handleLocalCommand(query, speakReply)
        if (localCommandHandled) return

        // Otherwise send to AI
        queryGenerativeAi(query, speakReply)
    }

    private fun handleLocalCommand(query: String, speakReply: Boolean): Boolean {
        val lower = query.lowercase(Locale.ROOT)

        val responseText: String? = when {
            lower.contains("flashlight on") || lower.contains("turn on flash") || lower.contains("torch on") -> {
                mobileControlManager.setFlashlight(true)
                "Turning flashlight ON."
            }
            lower.contains("flashlight off") || lower.contains("turn off flash") || lower.contains("torch off") -> {
                mobileControlManager.setFlashlight(false)
                "Turning flashlight OFF."
            }
            lower.contains("open camera") || lower.contains("take photo") || lower.contains("launch camera") -> {
                mobileControlManager.openCamera()
                "Opening Camera app."
            }
            lower.contains("open youtube") -> {
                val launched = mobileControlManager.openApp("com.google.android.youtube")
                if (!launched) mobileControlManager.openWebSearch("YouTube")
                "Opening YouTube."
            }
            lower.contains("volume up") || lower.contains("increase volume") || lower.contains("louder") -> {
                mobileControlManager.adjustVolume(1)
                "Increasing media volume."
            }
            lower.contains("volume down") || lower.contains("decrease volume") || lower.contains("quieter") -> {
                mobileControlManager.adjustVolume(-1)
                "Decreasing media volume."
            }
            lower.contains("battery") -> {
                val state = bluetoothDeviceManager.connectedState.value
                if (state.isConnected && state.batteryCombined >= 0) {
                    "Your ${state.deviceName} battery is at ${state.batteryCombined}%."
                } else if (state.isConnected) {
                    "${state.deviceName} is connected. Hardware does not broadcast separate battery telemetry."
                } else {
                    "No Bluetooth earbuds are currently connected."
                }
            }
            lower.contains("what time") || lower.contains("current time") -> {
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                "The current time is $timeStr."
            }
            lower.contains("joke") || lower.contains("tell me a joke") -> {
                "Why did the Bluetooth earbuds break up? Because there was no connection between them!"
            }
            lower.contains("weather") -> {
                mobileControlManager.openWebSearch("current weather")
                "Checking current local weather forecast."
            }
            else -> null
        }

        if (responseText != null) {
            postAssistantResponse(responseText, speakReply)
            return true
        }

        return false
    }

    private fun queryGenerativeAi(query: String, speakReply: Boolean) {
        _isGenerating.value = true

        scope.launch {
            try {
                val model = generativeModel
                val aiResponse = if (model != null) {
                    val prompt = "You are AIRMIND AI, a concise, smart audio assistant for Android earbuds. Answer in 1 to 2 clear sentences: $query"
                    val response = model.generateContent(prompt)
                    response.text ?: "I heard your request: $query."
                } else {
                    getOfflineSmartResponse(query)
                }

                postAssistantResponse(aiResponse, speakReply)

                // Save to Room database notes
                database.voiceNoteDao().insert(
                    VoiceNoteEntity(prompt = query, response = aiResponse)
                )
            } catch (_: Exception) {
                // If offline or network unavailable, give smart offline response
                val fallback = getOfflineSmartResponse(query)
                postAssistantResponse(fallback, speakReply)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun getOfflineSmartResponse(query: String): String {
        return "I've processed '$query'. For deep AI reasoning, ensure internet is active. You can also command 'flashlight on', 'open camera', 'volume up', or 'check battery' completely offline."
    }

    private fun postAssistantResponse(text: String, speakReply: Boolean) {
        val botMsg = AssistantMessage(text = text, isUser = false)
        _messages.value = _messages.value + botMsg

        if (speakReply) {
            speechManager.speak(text)
        }
    }
}
