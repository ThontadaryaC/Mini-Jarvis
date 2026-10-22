package com.minijarvis.core

/**
 * Represents an atomic task step in a multi-step execution chain.
 */
data class TaskStep(
    val stepNumber: Int,
    val toolName: String,
    val arguments: Map<String, Any?>,
    val description: String
)

/**
 * Plans multi-step task executions when compound instructions are received.
 */
class TaskPlanner {

    fun planTask(instruction: String): List<TaskStep> {
        val steps = mutableListOf<TaskStep>()
        val lower = instruction.lowercase()

        if (lower.contains("and then") || lower.contains("then")) {
            val parts = lower.split(Regex("and then|then"))
            parts.forEachIndexed { index, part ->
                val trimmed = part.trim()
                if (trimmed.startsWith("open ")) {
                    val app = trimmed.removePrefix("open ").trim()
                    steps.add(
                        TaskStep(
                            stepNumber = index + 1,
                            toolName = "AppLauncherTool",
                            arguments = mapOf("appName" to app),
                            description = "Open $app"
                        )
                    )
                }
            }
        }
        return steps
    }
}
