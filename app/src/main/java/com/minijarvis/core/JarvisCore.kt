package com.minijarvis.core

import android.content.Context
import com.minijarvis.ai.LLMResponseParser
import com.minijarvis.ai.ModelManager
import com.minijarvis.ai.PromptManager
import com.minijarvis.memory.JarvisDatabase
import com.minijarvis.memory.MemoryRepository
import com.minijarvis.security.ConfirmationManager
import com.minijarvis.security.PermissionManager
import com.minijarvis.tools.AppLauncherTool
import com.minijarvis.tools.DateTimeTool
import com.minijarvis.tools.SettingsTool
import com.minijarvis.tools.WhatsAppTool
import com.minijarvis.tools.YouTubeTool
import com.minijarvis.tools.WebSearchTool
import com.minijarvis.tools.Tool
import com.minijarvis.tools.ToolResult
import com.minijarvis.security.SecurityLevel
import com.minijarvis.voice.SpeechRecognizerManager
import com.minijarvis.voice.TextToSpeechManager
import com.minijarvis.voice.VoiceMode
import com.minijarvis.voice.WakeWordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Top-level Core orchestrator for RUBY.
 * Continuous 24/7 background listener with wake-phrase ("Hi Ruby"), multi-turn dialog, and complete shutdown capabilities.
 */
class JarvisCore(private val context: Context) {

    private val coreScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val sessionId = UUID.randomUUID().toString()

    // Subsystems
    val confirmationManager = ConfirmationManager()
    val permissionManager = PermissionManager(context)
    val modelManager = ModelManager()
    val promptManager = PromptManager()
    val parser = LLMResponseParser()
    val toolManager = ToolManager(confirmationManager)
    val database = JarvisDatabase.getDatabase(context)
    val memoryRepository = MemoryRepository(database.memoryDao(), database.conversationDao())
    val intentRouter = IntentRouter()
    val taskPlanner = TaskPlanner()

    val agentLoop = AgentLoop(
        modelManager = modelManager,
        promptManager = promptManager,
        parser = parser,
        toolManager = toolManager,
        memoryRepository = memoryRepository
    )

    // Voice
    val ttsManager = TextToSpeechManager(context)
    val wakeWordManager = WakeWordManager()
    lateinit var speechRecognizerManager: SpeechRecognizerManager

    // State
    private val _status = MutableStateFlow(JarvisStatus.READY)
    val status: StateFlow<JarvisStatus> = _status.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _latestResponse = MutableStateFlow("Ready. Say 'Hi Ruby' or tap the orb.")
    val latestResponse: StateFlow<String> = _latestResponse.asStateFlow()

    private val _showCreatorAvatar = MutableStateFlow(false)
    val showCreatorAvatar: StateFlow<Boolean> = _showCreatorAvatar.asStateFlow()

    init {
        // Register default tools
        toolManager.registerTool(AppLauncherTool(context))
        toolManager.registerTool(SettingsTool(context))
        toolManager.registerTool(DateTimeTool())
        toolManager.registerTool(YouTubeTool(context))
        toolManager.registerTool(WhatsAppTool(context))
        toolManager.registerTool(WebSearchTool(context))

        // Creator Tool for "Who built you?"
        toolManager.registerTool(object : Tool {
            override val name: String = "CreatorTool"
            override val description: String = "Displays creator avatar for 3 seconds."
            override val securityLevel: SecurityLevel = SecurityLevel.LEVEL_1_SAFE
            override val parameterSchema: String = "{}"
            override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
                triggerCreatorAvatar(3000L)
                return ToolResult.Success("Displaying creator.")
            }
        })

        // Complete Shutdown Tool
        toolManager.registerTool(object : Tool {
            override val name: String = "ShutdownTool"
            override val description: String = "Completely terminates and powers off the Ruby background program."
            override val securityLevel: SecurityLevel = SecurityLevel.LEVEL_1_SAFE
            override val parameterSchema: String = "{}"
            override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
                shutdownCompletely()
                return ToolResult.Success("Shutting down Ruby completely.")
            }
        })

        speechRecognizerManager = SpeechRecognizerManager(
            context = context,
            onCommandCallback = { speechText ->
                processUserInput(speechText)
            },
            onWakeWordDetected = {
                _status.value = JarvisStatus.READY
                _latestResponse.value = "Yes sir, how can I help you?"
                ttsManager.speak("Yes sir, how can I help you?")
            },
            onSleepDetected = {
                _status.value = JarvisStatus.READY
                _latestResponse.value = "Going to sleep. Say 'Hi Ruby' to wake me."
                ttsManager.speak("Going to sleep. Just say Hi Ruby whenever you need me.")
            },
            onShutdownDetected = {
                shutdownCompletely()
            },
            onErrorCallback = { errorMsg ->
                _status.value = JarvisStatus.READY
            }
        )
    }

    fun shutdownCompletely() {
        coreScope.launch {
            _status.value = JarvisStatus.SPEAKING
            _latestResponse.value = "Shutting down Ruby completely. Open the app whenever you want to restart."
            ttsManager.speak("Shutting down Ruby completely. Open the app whenever you want to restart.") {
                performFullExit()
            }
            delay(2800)
            performFullExit()
        }
    }

    private fun performFullExit() {
        try {
            speechRecognizerManager.stopContinuousListening()
            com.minijarvis.overlay.JarvisFloatingService.stop(context)
            ttsManager.shutdown()
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (ignored: Exception) {}
    }

    fun triggerCreatorAvatar(durationMs: Long = 3000L) {
        coreScope.launch {
            _showCreatorAvatar.value = true
            delay(durationMs)
            _showCreatorAvatar.value = false
        }
    }

    fun setStatus(newStatus: JarvisStatus) {
        _status.value = newStatus
    }

    fun processUserInput(input: String) {
        if (input.isBlank()) return

        val lower = input.lowercase().trim()
        if (lower == "shutdown" || lower == "shut down" || lower == "power off" || lower == "kill program" || lower == "stop ruby" || lower == "exit completely") {
            shutdownCompletely()
            return
        }

        speechRecognizerManager.setProcessingState(true)

        coreScope.launch {
            _status.value = JarvisStatus.THINKING
            val result = agentLoop.executeTurn(
                userInput = input,
                sessionId = sessionId,
                onStatusChange = { _status.value = it }
            )

            _latestResponse.value = result.responseText
            _status.value = JarvisStatus.SPEAKING

            ttsManager.speak(result.responseText) {
                _status.value = JarvisStatus.READY
                speechRecognizerManager.setProcessingState(false)
            }

            // Fallback reset status after delay if TTS completed asynchronously
            delay(2000)
            if (_status.value == JarvisStatus.SPEAKING) {
                _status.value = JarvisStatus.READY
                speechRecognizerManager.setProcessingState(false)
            }
        }
    }

    fun toggleVoiceInput() {
        if (speechRecognizerManager.voiceMode.value == VoiceMode.ACTIVE_LISTENING) {
            speechRecognizerManager.putToSleepManually()
        } else {
            speechRecognizerManager.wakeUpManually()
        }
    }

    fun shutdown() {
        speechRecognizerManager.stopContinuousListening()
        ttsManager.shutdown()
    }
}
