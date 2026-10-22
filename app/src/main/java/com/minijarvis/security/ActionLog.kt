package com.minijarvis.security

/**
 * Audit log entry capturing agent actions, tools called, and results.
 */
data class ActionLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val securityLevel: SecurityLevel,
    val argumentsJson: String,
    val status: String,
    val details: String? = null
)
