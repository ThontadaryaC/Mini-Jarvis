package com.minijarvis.ai

/**
 * Constructs system prompts, injects tool signatures, available memory context, and constraints.
 */
class PromptManager {

    companion object {
        const val SYSTEM_PROMPT_HEADER = """
You are MINI JARVIS, an autonomous personal AI assistant running on Android (OPPO A3 Pro 5G).
You assist the user through natural voice and text commands, executing tools and recalling memory.

Always reason carefully before choosing a tool.
Output your response STRICTLY as valid JSON matching this schema:
{
  "intent": "<intent_name>",
  "tool": "<ToolName or null>",
  "arguments": { <tool_arguments> },
  "response": "<spoken or displayed user response>"
}

Do not hallucinate tools that do not exist.
Never perform destructive actions without confirmation.
"""
    }

    fun buildPrompt(
        userInput: String,
        availableTools: List<String>,
        relevantMemories: List<String> = emptyList(),
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): String {
        val toolsSection = if (availableTools.isNotEmpty()) {
            "\nAvailable Tools:\n" + availableTools.joinToString("\n") { "- $it" }
        } else ""

        val memorySection = if (relevantMemories.isNotEmpty()) {
            "\nUser Memories:\n" + relevantMemories.joinToString("\n") { "- $it" }
        } else ""

        val historySection = if (conversationHistory.isNotEmpty()) {
            "\nRecent Conversation:\n" + conversationHistory.joinToString("\n") { (role, text) ->
                "$role: $text"
            }
        } else ""

        return "$SYSTEM_PROMPT_HEADER$toolsSection$memorySection$historySection\n\nUser: $userInput\nJARVIS:"
    }
}
