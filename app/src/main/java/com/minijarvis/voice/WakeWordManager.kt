package com.minijarvis.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Interface and scaffolding for wake word detection ("Hey Jarvis").
 * Will be fully integrated in later milestones using on-device wake-word engines.
 */
class WakeWordManager {
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
    }

    fun startListening(onWakeWordDetected: () -> Unit) {
        // Scaffolding for lightweight wake word detector
    }

    fun stopListening() {
        // Stop wake word engine
    }
}
