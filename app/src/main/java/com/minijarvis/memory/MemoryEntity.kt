package com.minijarvis.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Long-term user memory entity (preferences, facts, contacts, habits).
 */
@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general",
    val importance: Int = 1, // 1 (low) to 5 (critical)
    val source: String = "user_dialogue",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Conversation message entity for episodic chat history.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val role: String, // "user" or "jarvis" or "system"
    val content: String,
    val toolCallJson: String? = null,
    val toolResultJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
