package com.minijarvis.security

/**
 * Three-tier security architecture to ensure safe autonomous agent execution.
 */
enum class SecurityLevel {
    /**
     * Level 1: Safe actions (read non-sensitive data, open apps, web searches, media playback).
     * No user confirmation needed.
     */
    LEVEL_1_SAFE,

    /**
     * Level 2: Personal actions (sending messages, reminders, calendar changes, sharing).
     * Requires explicit user confirmation by default.
     */
    LEVEL_2_PERSONAL,

    /**
     * Level 3: Sensitive/Irreversible actions (delete files, security changes, financial actions).
     * ALWAYS requires explicit user confirmation.
     */
    LEVEL_3_SENSITIVE
}
