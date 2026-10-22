package com.minijarvis.memory

import kotlinx.coroutines.flow.Flow

class MemoryRepository(
    private val memoryDao: MemoryDao,
    private val conversationDao: ConversationDao
) {
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemoriesFlow()
    val allMessages: Flow<List<ConversationEntity>> = conversationDao.getAllMessagesFlow()

    suspend fun saveMemory(key: String, value: String, category: String = "general", importance: Int = 1) {
        val entity = MemoryEntity(
            key = key,
            value = value,
            category = category,
            importance = importance
        )
        memoryDao.insertMemory(entity)
    }

    suspend fun findMemory(key: String): MemoryEntity? {
        return memoryDao.getMemoryByKey(key)
    }

    suspend fun searchMemories(query: String): List<MemoryEntity> {
        return memoryDao.searchMemories(query)
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteById(id)
    }

    suspend fun clearMemories() {
        memoryDao.clearAll()
    }

    suspend fun recordMessage(
        sessionId: String,
        role: String,
        content: String,
        toolCallJson: String? = null,
        toolResultJson: String? = null
    ) {
        val entity = ConversationEntity(
            sessionId = sessionId,
            role = role,
            content = content,
            toolCallJson = toolCallJson,
            toolResultJson = toolResultJson
        )
        conversationDao.insertMessage(entity)
    }

    suspend fun getRecentMessages(limit: Int = 10): List<ConversationEntity> {
        return conversationDao.getRecentMessages(limit)
    }

    suspend fun clearHistory() {
        conversationDao.clearHistory()
    }
}
