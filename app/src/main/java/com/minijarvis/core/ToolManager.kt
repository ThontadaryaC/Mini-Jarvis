package com.minijarvis.core

import com.minijarvis.security.ActionLog
import com.minijarvis.security.ConfirmationManager
import com.minijarvis.security.SecurityLevel
import com.minijarvis.tools.Tool
import com.minijarvis.tools.ToolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ToolManager(
    private val confirmationManager: ConfirmationManager
) {
    private val registeredTools = mutableMapOf<String, Tool>()
    private val _actionLogs = MutableStateFlow<List<ActionLog>>(emptyList())
    val actionLogs: StateFlow<List<ActionLog>> = _actionLogs.asStateFlow()

    fun registerTool(tool: Tool) {
        registeredTools[tool.name] = tool
    }

    fun getTool(name: String): Tool? = registeredTools[name]

    fun getAllToolSignatures(): List<String> {
        return registeredTools.values.map {
            "${it.name}: ${it.description} | Params: ${it.parameterSchema} | Level: ${it.securityLevel}"
        }
    }

    suspend fun executeTool(
        toolName: String,
        arguments: Map<String, Any?>,
        onConfirmationNeeded: (suspend () -> ToolResult)? = null
    ): ToolResult {
        val tool = registeredTools[toolName]
            ?: return ToolResult.NotAvailable("Tool '$toolName' is not registered in JARVIS core.")

        // Security check
        if (confirmationManager.requiresConfirmation(tool.securityLevel)) {
            return ToolResult.NeedsConfirmation(
                title = "Permission Confirmation",
                prompt = "JARVIS wants to execute ${tool.name} with parameters: $arguments",
                pendingArguments = arguments
            )
        }

        return try {
            val result = tool.execute(arguments)
            logAction(tool, arguments, result)
            result
        } catch (e: Exception) {
            val failed = ToolResult.Failed("Exception during tool execution: ${e.message}")
            logAction(tool, arguments, failed)
            failed
        }
    }

    private fun logAction(tool: Tool, arguments: Map<String, Any?>, result: ToolResult) {
        val status = when (result) {
            is ToolResult.Success -> "SUCCESS"
            is ToolResult.Failed -> "FAILED: ${result.reason}"
            is ToolResult.NeedsConfirmation -> "NEEDS_CONFIRMATION"
            is ToolResult.NotAvailable -> "NOT_AVAILABLE"
        }
        val entry = ActionLog(
            toolName = tool.name,
            securityLevel = tool.securityLevel,
            argumentsJson = arguments.toString(),
            status = status
        )
        _actionLogs.value = listOf(entry) + _actionLogs.value.take(49)
    }
}
