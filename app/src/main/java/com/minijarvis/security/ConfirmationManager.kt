package com.minijarvis.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data packet representing a pending confirmation request for Level 2 or Level 3 actions.
 */
data class PendingConfirmation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val securityLevel: SecurityLevel,
    val toolName: String,
    val arguments: Map<String, Any?>,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

/**
 * Manages user confirmation gates for sensitive actions.
 * Never allows the LLM to silently execute Level 2/3 actions.
 */
class ConfirmationManager {

    private val _pendingRequest = MutableStateFlow<PendingConfirmation?>(null)
    val pendingRequest: StateFlow<PendingConfirmation?> = _pendingRequest.asStateFlow()

    /**
     * Checks whether an action requires user confirmation.
     */
    fun requiresConfirmation(level: SecurityLevel, userBypassPersonal: Boolean = false): Boolean {
        return when (level) {
            SecurityLevel.LEVEL_1_SAFE -> false
            SecurityLevel.LEVEL_2_PERSONAL -> !userBypassPersonal
            SecurityLevel.LEVEL_3_SENSITIVE -> true
        }
    }

    /**
     * Request user confirmation for a sensitive action.
     */
    fun requestConfirmation(
        title: String,
        description: String,
        securityLevel: SecurityLevel,
        toolName: String,
        arguments: Map<String, Any?>,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        _pendingRequest.value = PendingConfirmation(
            title = title,
            description = description,
            securityLevel = securityLevel,
            toolName = toolName,
            arguments = arguments,
            onConfirm = {
                _pendingRequest.value = null
                onConfirm()
            },
            onCancel = {
                _pendingRequest.value = null
                onCancel()
            }
        )
    }

    fun dismiss() {
        _pendingRequest.value?.onCancel?.invoke()
        _pendingRequest.value = null
    }
}
