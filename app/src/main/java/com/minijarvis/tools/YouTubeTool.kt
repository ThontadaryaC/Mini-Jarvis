package com.minijarvis.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.minijarvis.security.SecurityLevel
import java.net.URLEncoder

/**
 * Tool to search and play videos on YouTube.
 */
class YouTubeTool(context: Context) : AndroidTool(context) {

    override val name: String = "YouTubeTool"
    override val description: String = "Searches for and opens videos on YouTube given a search query."
    override val securityLevel: SecurityLevel = SecurityLevel.LEVEL_1_SAFE
    override val parameterSchema: String = """{"query": "string"}"""

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val query = arguments["query"] as? String ?: ""

        if (query.isBlank()) {
            // Launch YouTube Home
            return try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    ToolResult.Success("Opening YouTube...")
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolResult.Success("Opening YouTube...")
                }
            } catch (e: Exception) {
                ToolResult.Failed("Failed to open YouTube: ${e.message}")
            }
        }

        // Direct search query execution
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUri = Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")

            val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.google.android.youtube")
            }

            try {
                context.startActivity(intent)
                ToolResult.Success("Searching YouTube for '$query'...")
            } catch (ex: Exception) {
                // Browser fallback
                val webIntent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                ToolResult.Success("Searching YouTube for '$query'...")
            }
        } catch (e: Exception) {
            ToolResult.Failed("Could not execute YouTube search: ${e.message}")
        }
    }
}
