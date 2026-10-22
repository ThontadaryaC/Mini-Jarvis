package com.minijarvis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.minijarvis.core.JarvisStatus
import com.minijarvis.memory.ConversationEntity
import com.minijarvis.memory.MemoryEntity
import com.minijarvis.security.PendingConfirmation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    val jarvisCore = (application as JarvisApplication).jarvisCore

    val status: StateFlow<JarvisStatus> = jarvisCore.status
    val isOnline: StateFlow<Boolean> = jarvisCore.isOnline
    val latestResponse: StateFlow<String> = jarvisCore.latestResponse
    val pendingConfirmation: StateFlow<PendingConfirmation?> = jarvisCore.confirmationManager.pendingRequest

    val isListening: StateFlow<Boolean> = jarvisCore.speechRecognizerManager.isListening

    val messages: StateFlow<List<ConversationEntity>> = jarvisCore.memoryRepository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = jarvisCore.memoryRepository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun submitUserInput(text: String = _inputText.value) {
        if (text.isBlank()) return
        _inputText.value = ""
        jarvisCore.processUserInput(text)
    }

    fun toggleVoice() {
        jarvisCore.toggleVoiceInput()
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            jarvisCore.memoryRepository.deleteMemory(id)
        }
    }

    fun saveMemory(key: String, value: String) {
        viewModelScope.launch {
            jarvisCore.memoryRepository.saveMemory(key, value)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            jarvisCore.memoryRepository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Do not shutdown jarvisCore here so background floating service continues listening!
    }
}
