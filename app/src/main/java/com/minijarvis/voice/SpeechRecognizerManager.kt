package com.minijarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceMode {
    STANDBY_WAKEWORD, // Passive listening 24/7 for "Hi Ruby" / "Hey Ruby"
    ACTIVE_LISTENING, // Patient active listening with sentence accumulator
    MUTED
}

/**
 * Intelligent Speech and Wake-Phrase Listener for Ruby.
 * Features a Patient Sentence Accumulator & Silence Debounce Engine:
 * - Patiently listens to full multi-clause commands without cutting the user off.
 * - Accumulates pauses and fragments into a complete sentence before dispatching to LLM.
 * - Completely terminates on shutdown.
 */
class SpeechRecognizerManager(
    private val context: Context,
    private val onCommandCallback: (String) -> Unit,
    private val onWakeWordDetected: () -> Unit,
    private val onSleepDetected: () -> Unit,
    private val onShutdownDetected: () -> Unit,
    private val onErrorCallback: (String) -> Unit
) {
    companion object {
        private const val TAG = "SpeechRecognizerManager"
        private const val SILENCE_DEBOUNCE_MS = 500L // Fast 500ms silence tolerance
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _voiceMode = MutableStateFlow(VoiceMode.STANDBY_WAKEWORD)
    val voiceMode: StateFlow<VoiceMode> = _voiceMode.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private var isContinuousRunning = true
    private var isCurrentlyProcessing = false

    // Sentence Accumulator
    private val speechAccumulator = StringBuilder()
    private var debounceRunnable: Runnable? = null

    private val wakeKeywords = listOf(
        "ruby", "hi ruby", "hey ruby", "jarvis", "hey jarvis", "hi jarvis",
        "ರೂಬಿ", "ಹಾಯ್ ರೂಬಿ", "ಹೇ ರೂಬಿ", "ರೂಬೀ", "ಹಲೋ ರೂಬಿ", "ನಮಸ್ಕಾರ ರೂಬಿ", "ನಮಸ್ತೆ ರೂಬಿ"
    )
    private val shutdownKeywords = listOf(
        "shutdown", "shut down", "power off", "kill program", "stop ruby", "exit completely", "turn off ruby", "kill ruby",
        "ಶಟ್‌ಡೌನ್", "ಆಫ್ ಮಾಡು", "ನಿಲ್ಲಿಸು"
    )
    private val exitKeywords = listOf(
        "exit", "please exit", "stop", "bye", "goodbye", "sleep", "go to sleep",
        "ಸಾಕು", "ಮಲಗು"
    )

    init {
        startContinuousListening()
    }

    fun setProcessingState(processing: Boolean) {
        isCurrentlyProcessing = processing
        if (!processing && isContinuousRunning) {
            scheduleRestart(250)
        }
    }

    fun wakeUpManually() {
        cancelDebounce()
        speechAccumulator.clear()
        _voiceMode.value = VoiceMode.ACTIVE_LISTENING
        onWakeWordDetected()
        scheduleRestart(150)
    }

    fun putToSleepManually() {
        cancelDebounce()
        speechAccumulator.clear()
        _voiceMode.value = VoiceMode.STANDBY_WAKEWORD
        onSleepDetected()
        scheduleRestart(250)
    }

    fun startContinuousListening() {
        isContinuousRunning = true
        scheduleRestart(150)
    }

    fun stopContinuousListening() {
        isContinuousRunning = false
        cancelDebounce()
        speechAccumulator.clear()
        stopListening()
    }

    private fun cancelDebounce() {
        debounceRunnable?.let { handler.removeCallbacks(it) }
        debounceRunnable = null
    }

    private fun scheduleRestart(delayMillis: Long) {
        if (!isContinuousRunning || isCurrentlyProcessing) return
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (isContinuousRunning && !isCurrentlyProcessing) {
                beginListening()
            }
        }, delayMillis)
    }

    private fun beginListening() {
        if (!isContinuousRunning || isCurrentlyProcessing || !SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        try {
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {}

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (!isContinuousRunning) {
                            stopListening()
                            return
                        }
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                        cancelDebounce()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _soundLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _soundLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _soundLevel.value = 0f

                        if (!isContinuousRunning) {
                            stopListening()
                            return
                        }

                        if (speechAccumulator.isNotBlank() && _voiceMode.value == VoiceMode.ACTIVE_LISTENING) {
                            scheduleDebounceExecution(150L)
                        } else if (isContinuousRunning && !isCurrentlyProcessing) {
                            // Only restart in Active mode, or back off to 1.5s in standby to prevent sound looping
                            val restartDelay = if (_voiceMode.value == VoiceMode.ACTIVE_LISTENING) 500L else 1500L
                            scheduleRestart(restartDelay)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _soundLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""

                        handleRecognizedSegment(text)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partialText = partials?.firstOrNull()?.lowercase() ?: ""

                        if (_voiceMode.value == VoiceMode.STANDBY_WAKEWORD) {
                            val isKannada = partialText.any { it.code in 0x0C80..0x0CFF }
                            if (wakeKeywords.any { partialText.contains(it) } || isKannada) {
                                _voiceMode.value = VoiceMode.ACTIVE_LISTENING
                                onWakeWordDetected()
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "kn-IN", "hi-IN", "en-US"))
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                // Snappy silence parameters for fast voice turnaround
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000L)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            if (isContinuousRunning && !isCurrentlyProcessing) {
                scheduleRestart(1500L)
            }
        }
    }

    private fun handleRecognizedSegment(rawText: String) {
        if (!isContinuousRunning || rawText.isBlank()) return

        val lower = rawText.lowercase().trim()
        val isKannadaScript = rawText.any { it.code in 0x0C80..0x0CFF }

        // 1. Immediate Shutdown Check
        if (shutdownKeywords.any { lower == it || lower.contains(it) }) {
            isContinuousRunning = false
            handler.removeCallbacksAndMessages(null)
            cancelDebounce()
            speechAccumulator.clear()
            stopListening()
            onShutdownDetected()
            return
        }

        when (_voiceMode.value) {
            VoiceMode.STANDBY_WAKEWORD -> {
                if (wakeKeywords.any { lower.contains(it) } || isKannadaScript) {
                    _voiceMode.value = VoiceMode.ACTIVE_LISTENING
                    onWakeWordDetected()

                    var cleaned = rawText
                    wakeKeywords.forEach { kw ->
                        cleaned = cleaned.replace(Regex("(?i)$kw"), "").trim()
                    }

                    if (cleaned.isNotBlank()) {
                        cancelDebounce()
                        speechAccumulator.clear()
                        isCurrentlyProcessing = true
                        onCommandCallback(cleaned)
                    } else {
                        scheduleRestart(200)
                    }
                } else {
                    if (isContinuousRunning) {
                        scheduleRestart(1500L)
                    }
                }
            }

            VoiceMode.ACTIVE_LISTENING -> {
                if (exitKeywords.any { lower == it || lower.contains(it) }) {
                    cancelDebounce()
                    speechAccumulator.clear()
                    _voiceMode.value = VoiceMode.STANDBY_WAKEWORD
                    onSleepDetected()
                    scheduleRestart(1500L)
                    return
                }

                cancelDebounce()
                speechAccumulator.clear()
                isCurrentlyProcessing = true
                onCommandCallback(rawText)
            }

            VoiceMode.MUTED -> {}
        }
    }

    private fun scheduleDebounceExecution(delayMs: Long) {
        cancelDebounce()
        debounceRunnable = Runnable {
            val fullSentence = speechAccumulator.toString().trim()
            speechAccumulator.clear()

            if (fullSentence.isNotBlank() && isContinuousRunning && !isCurrentlyProcessing) {
                Log.i(TAG, "Finalizing complete speech command for LLM: '$fullSentence'")
                onCommandCallback(fullSentence)
            }
        }
        handler.postDelayed(debounceRunnable!!, delayMs)
    }

    private fun stopListening() {
        handler.removeCallbacksAndMessages(null)
        cancelDebounce()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {}
        speechRecognizer = null
        _isListening.value = false
        _soundLevel.value = 0f
    }
}
