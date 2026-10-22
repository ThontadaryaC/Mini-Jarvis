package com.minijarvis.core

/**
 * Represents the operational state of MINI JARVIS.
 * Drives the UI animations, orb visual effects, and audio feedback.
 */
enum class JarvisStatus(val displayName: String) {
    READY("READY"),
    LISTENING("LISTENING"),
    THINKING("THINKING"),
    EXECUTING("EXECUTING"),
    SPEAKING("SPEAKING"),
    OFFLINE("OFFLINE")
}
