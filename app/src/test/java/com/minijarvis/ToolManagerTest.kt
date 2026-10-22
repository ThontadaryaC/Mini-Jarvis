package com.minijarvis

import com.minijarvis.core.ToolManager
import com.minijarvis.security.ConfirmationManager
import com.minijarvis.security.SecurityLevel
import com.minijarvis.tools.Tool
import com.minijarvis.tools.ToolResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToolManagerTest {

    private lateinit var confirmationManager: ConfirmationManager
    private lateinit var toolManager: ToolManager

    @Before
    fun setup() {
        confirmationManager = ConfirmationManager()
        toolManager = ToolManager(confirmationManager)
    }

    @Test
    fun registerAndExecuteLevel1Tool_succeedsWithoutConfirmation() = runBlocking {
        val testTool = object : Tool {
            override val name: String = "TestSafeTool"
            override val description: String = "Safe tool description"
            override val securityLevel: SecurityLevel = SecurityLevel.LEVEL_1_SAFE
            override val parameterSchema: String = "{}"

            override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
                return ToolResult.Success("Safe execution complete")
            }
        }

        toolManager.registerTool(testTool)
        val result = toolManager.executeTool("TestSafeTool", emptyMap())

        assertTrue(result is ToolResult.Success)
        assertEquals("Safe execution complete", (result as ToolResult.Success).output)
    }

    @Test
    fun executeLevel2Tool_requiresConfirmation() = runBlocking {
        val testSensitiveTool = object : Tool {
            override val name: String = "TestPersonalTool"
            override val description: String = "Personal tool description"
            override val securityLevel: SecurityLevel = SecurityLevel.LEVEL_2_PERSONAL
            override val parameterSchema: String = "{}"

            override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
                return ToolResult.Success("Executed sensitive action")
            }
        }

        toolManager.registerTool(testSensitiveTool)
        val result = toolManager.executeTool("TestPersonalTool", emptyMap())

        assertTrue(result is ToolResult.NeedsConfirmation)
    }

    @Test
    fun executeNonExistentTool_returnsNotAvailable() = runBlocking {
        val result = toolManager.executeTool("GhostTool", emptyMap())
        assertTrue(result is ToolResult.NotAvailable)
    }
}
