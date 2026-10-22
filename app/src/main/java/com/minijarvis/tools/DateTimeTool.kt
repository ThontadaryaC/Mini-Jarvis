package com.minijarvis.tools

import com.minijarvis.security.SecurityLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tool to retrieve current local date, time, and day of week.
 */
class DateTimeTool : Tool {

    override val name: String = "DateTimeTool"
    override val description: String = "Provides current local time, date, and day of week."
    override val securityLevel: SecurityLevel = SecurityLevel.LEVEL_1_SAFE
    override val parameterSchema: String = """{"type": "time | date | both"}"""

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        val currentTime = timeFormat.format(now)
        val currentDate = dateFormat.format(now)

        val type = (arguments["type"] as? String)?.lowercase() ?: "both"

        val response = when (type) {
            "time" -> "The time is $currentTime."
            "date" -> "Today is $currentDate."
            else -> "The time is $currentTime, and today is $currentDate."
        }

        return ToolResult.Success(
            output = response,
            data = mapOf("time" to currentTime, "date" to currentDate)
        )
    }
}
