package com.minijarvis.tools

import com.minijarvis.security.SecurityLevel

/**
 * Common abstraction for all tools usable by MINI JARVIS.
 */
interface Tool {
    val name: String
    val description: String
    val securityLevel: SecurityLevel
    val parameterSchema: String

    /**
     * Executes the tool with the validated arguments.
     */
    suspend fun execute(arguments: Map<String, Any?>): ToolResult
}
