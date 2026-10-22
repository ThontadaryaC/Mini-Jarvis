package com.minijarvis.core

import com.minijarvis.ai.LLMResponseParser
import com.minijarvis.ai.ModelManager
import com.minijarvis.ai.PromptManager
import com.minijarvis.memory.MemoryRepository
import com.minijarvis.tools.ToolResult

/**
 * Result of a single execution turn in the Agent Loop.
 */
data class AgentTurnResult(
    val responseText: String,
    val toolCalled: String? = null,
    val toolResult: ToolResult? = null,
    val status: JarvisStatus = JarvisStatus.READY
)

/**
 * Autonomous Agent Loop that handles input -> LLM -> Tool Selection -> Validation -> Tool Execution -> Response.
 */
class AgentLoop(
    private val modelManager: ModelManager,
    private val promptManager: PromptManager,
    private val parser: LLMResponseParser,
    private val toolManager: ToolManager,
    private val memoryRepository: MemoryRepository
) {

    suspend fun executeTurn(
        userInput: String,
        sessionId: String,
        onStatusChange: (JarvisStatus) -> Unit
    ): AgentTurnResult {
        onStatusChange(JarvisStatus.THINKING)

        // 1. Fetch memories and recent history for prompt context
        val recentMemories = try {
            memoryRepository.searchMemories(userInput).map { "${it.key}: ${it.value}" }
        } catch (e: Exception) {
            emptyList()
        }

        val toolSignatures = toolManager.getAllToolSignatures()
        val prompt = promptManager.buildPrompt(userInput, toolSignatures, recentMemories)

        // 2. Generate response from local LLM
        val engine = modelManager.getEngine()
        val generationResult = engine.generate(prompt)

        val rawOutput = generationResult.getOrElse {
            onStatusChange(JarvisStatus.READY)
            return AgentTurnResult(
                responseText = "I encountered an error during neural processing: ${it.message}",
                status = JarvisStatus.READY
            )
        }

        // 3. Parse structured intent & tool call
        val parsed = parser.parse(rawOutput)

        var toolResult: ToolResult? = null
        var finalSpeech = parsed.directResponse

        // 4. Handle tool execution if requested
        if (!parsed.toolName.isNullOrBlank()) {
            onStatusChange(JarvisStatus.EXECUTING)
            toolResult = toolManager.executeTool(parsed.toolName, parsed.arguments)

            when (toolResult) {
                is ToolResult.Success -> {
                    if (finalSpeech.isBlank()) {
                        finalSpeech = toolResult.output
                    }
                }
                is ToolResult.Failed -> {
                    finalSpeech = "I couldn't complete that because: ${toolResult.reason}"
                }
                is ToolResult.NeedsConfirmation -> {
                    finalSpeech = "Action requires your confirmation: ${toolResult.prompt}"
                }
                is ToolResult.NotAvailable -> {
                    finalSpeech = "I'm sorry, but ${toolResult.reason}"
                }
            }
        }

        // 5. Save turn into episodic memory
        try {
            memoryRepository.recordMessage(
                sessionId = sessionId,
                role = "user",
                content = userInput
            )
            memoryRepository.recordMessage(
                sessionId = sessionId,
                role = "jarvis",
                content = finalSpeech,
                toolCallJson = parsed.rawJson,
                toolResultJson = toolResult?.toString()
            )
        } catch (ignored: Exception) {}

        return AgentTurnResult(
            responseText = finalSpeech,
            toolCalled = parsed.toolName,
            toolResult = toolResult,
            status = JarvisStatus.SPEAKING
        )
    }
}
