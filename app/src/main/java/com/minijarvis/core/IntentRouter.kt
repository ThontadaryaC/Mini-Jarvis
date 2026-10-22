package com.minijarvis.core

/**
 * High-level intent categorization.
 */
enum class IntentCategory {
    APP_LAUNCH,
    SYSTEM_SETTING,
    MEMORY_STORE,
    MEMORY_RECALL,
    MEDIA_CONTROL,
    CONVERSATION,
    AUTOMATION_TASK,
    UNKNOWN
}

/**
 * Fast-path local rule router before LLM inference to minimize CPU/battery on OPPO A3 Pro 5G.
 */
class IntentRouter {

    fun classify(input: String): IntentCategory {
        val lower = input.lowercase().trim()
        return when {
            lower.startsWith("open ") || lower.startsWith("launch ") -> IntentCategory.APP_LAUNCH
            lower.contains("wifi") || lower.contains("bluetooth") || lower.contains("settings") -> IntentCategory.SYSTEM_SETTING
            lower.contains("remember ") || lower.contains("my favorite ") -> IntentCategory.MEMORY_STORE
            lower.contains("what is my ") || lower.contains("do you remember") -> IntentCategory.MEMORY_RECALL
            lower.contains("play ") || lower.contains("pause") || lower.contains("volume") -> IntentCategory.MEDIA_CONTROL
            else -> IntentCategory.CONVERSATION
        }
    }
}
