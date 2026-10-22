package com.minijarvis.tools

/**
 * Structured result returned by any tool execution.
 * Enforces structured communication back to the Agent Core and LLM.
 */
sealed class ToolResult {
    data class Success(
        val output: String,
        val data: Map<String, Any?> = emptyMap()
    ) : ToolResult()

    data class Failed(
        val reason: String,
        val errorCode: String? = null
    ) : ToolResult()

    data class NeedsConfirmation(
        val title: String,
        val prompt: String,
        val pendingArguments: Map<String, Any?>
    ) : ToolResult()

    data class NotAvailable(
        val reason: String
    ) : ToolResult()
}
