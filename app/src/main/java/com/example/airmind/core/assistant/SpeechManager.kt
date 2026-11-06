package com.example.airmind.core.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.airmind.core.audio.BluetoothAudioRouteManager
import com.example.airmind.core.bluetooth.BluetoothDeviceManager
import com.example.airmind.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val audioRouteManager: BluetoothAudioRouteManager? = null,
    private val bluetoothDeviceManager: BluetoothDeviceManager? = null,
    private val preferencesRepository: UserPreferencesRepository? = null
) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _isRecognizing = MutableStateFlow(false)
    val isRecognizing: StateFlow<Boolean> = _isRecognizing.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onResultCallback: ((String) -> Unit)? = null

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            textToSpeech?.setPitch(1.02f)
            textToSpeech?.setSpeechRate(1.0f)

            // Direct TTS audio output to Assistant / Speech stream for optimal Bluetooth Earbuds routing
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            textToSpeech?.setAudioAttributes(audioAttributes)

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    audioRouteManager?.deactivateEarbudsRoute()
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    audioRouteManager?.deactivateEarbudsRoute()
                }
            })
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        onResultCallback = onResult
        _spokenText.value = ""

        scope.launch {
            val isEarbudsConnected = bluetoothDeviceManager?.connectedState?.value?.isConnected == true
            val allowPhoneFallback = preferencesRepository?.isPhoneFallbackAllowed?.first() ?: false

            if (!isEarbudsConnected && !allowPhoneFallback) {
                val errorMsg = "Bluetooth earbuds not connected. Connect your earbuds or enable Phone Fallback in Earbuds Settings."
                _spokenText.value = errorMsg
                onResult(errorMsg)
                return@launch
            }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                val errorMsg = "Speech recognition service not available on this device."
                _spokenText.value = errorMsg
                onResult(errorMsg)
                return@launch
            }

            stopListening()

            // Activate Bluetooth communication route (SCO/BLE headset mic)
            if (isEarbudsConnected) {
                audioRouteManager?.activateEarbudsRoute(forRecording = true)
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isRecognizing.value = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isRecognizing.value = false
                    }
                    override fun onError(error: Int) {
                        _isRecognizing.value = false
                        audioRouteManager?.deactivateEarbudsRoute()
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap earbud to try again."
                            SpeechRecognizer.ERROR_AUDIO -> "Earbud audio recording error. Verify microphone permissions."
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection required for speech recognition."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out."
                            else -> "Recognition paused ($error)"
                        }
                        _spokenText.value = errorMsg
                    }
                    override fun onResults(results: Bundle?) {
                        _isRecognizing.value = false
                        audioRouteManager?.deactivateEarbudsRoute()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _spokenText.value = text
                        if (text.isNotBlank()) {
                            onResultCallback?.invoke(text)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let { _spokenText.value = it }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _isRecognizing.value = false
                audioRouteManager?.deactivateEarbudsRoute()
                _spokenText.value = "Failed to start listening: ${e.message}"
            }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isRecognizing.value = false
        audioRouteManager?.deactivateEarbudsRoute()
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (!isTtsReady || text.isBlank()) return

        val isEarbudsConnected = bluetoothDeviceManager?.connectedState?.value?.isConnected == true
        if (isEarbudsConnected) {
            audioRouteManager?.activateEarbudsRoute(forRecording = false)
        }

        _isSpeaking.value = true
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "AIRMIND_SPEECH_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "AIRMIND_SPEECH_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
        audioRouteManager?.deactivateEarbudsRoute()
    }

    fun release() {
        stopListening()
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
