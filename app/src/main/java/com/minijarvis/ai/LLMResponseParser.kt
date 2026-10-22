package com.minijarvis.ai

import org.json.JSONObject

/**
 * Structured container for parsed LLM decisions.
 */
data class ParsedResponse(
    val intent: String,
    val toolName: String?,
    val arguments: Map<String, Any?>,
    val directResponse: String,
    val rawJson: String?
)

/**
 * Robust JSON and unstructured response parser.
 * Handles clean JSON, embedded JSON markdown blocks (```json ... ```), and raw text fallback.
 */
class LLMResponseParser {

    fun parse(rawResponse: String): ParsedResponse {
        val trimmed = rawResponse.trim()
        val extractedJson = extractJsonString(trimmed)

        if (extractedJson != null) {
            try {
                val json = JSONObject(extractedJson)
                val intent = json.optString("intent", "chat")
                val toolName = if (json.has("tool") && !json.isNull("tool") && json.getString("tool").isNotBlank()) {
                    json.getString("tool")
                } else null

                val argsMap = mutableMapOf<String, Any?>()
                if (json.has("arguments") && !json.isNull("arguments")) {
                    val argsObj = json.getJSONObject("arguments")
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.get(key)
                    }
                }

                val directResponse = json.optString("response", "")

                return ParsedResponse(
                    intent = intent,
                    toolName = toolName,
                    arguments = argsMap,
                    directResponse = directResponse.ifBlank { "Acknowledged." },
                    rawJson = extractedJson
                )
            } catch (e: Exception) {
                // Fallback to text parsing if JSON syntax is invalid
            }
        }

        // Plain text fallback
        return ParsedResponse(
            intent = "chat",
            toolName = null,
            arguments = emptyMap(),
            directResponse = trimmed,
            rawJson = null
        )
    }

    private fun extractJsonString(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }
        val markdownMatch = Regex("""```(?:json)?\s*(\{[\s\S]*?\})\s*```""").find(trimmed)
        if (markdownMatch != null) {
            return markdownMatch.groupValues[1]
        }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return null
    }
}
