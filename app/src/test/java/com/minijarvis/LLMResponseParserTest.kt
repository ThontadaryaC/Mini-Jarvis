package com.minijarvis

import com.minijarvis.ai.LLMResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LLMResponseParserTest {

    private lateinit var parser: LLMResponseParser

    @Before
    fun setup() {
        parser = LLMResponseParser()
    }

    @Test
    fun parse_validJsonToolCall_returnsParsedResponse() {
        val raw = """
            {
              "intent": "open_app",
              "tool": "AppLauncherTool",
              "arguments": {
                "appName": "youtube"
              },
              "response": "Opening YouTube for you, sir."
            }
        """.trimIndent()

        val parsed = parser.parse(raw)

        assertEquals("open_app", parsed.intent)
        assertEquals("AppLauncherTool", parsed.toolName)
        assertEquals("youtube", parsed.arguments["appName"])
        assertEquals("Opening YouTube for you, sir.", parsed.directResponse)
        assertNotNull(parsed.rawJson)
    }

    @Test
    fun parse_markdownWrappedJson_extractsJsonCleanly() {
        val raw = """
            Here is the action:
            ```json
            {
              "intent": "open_settings",
              "tool": "SettingsTool",
              "arguments": {
                "settingType": "wifi"
              },
              "response": "Opening Wi-Fi settings."
            }
            ```
        """.trimIndent()

        val parsed = parser.parse(raw)

        assertEquals("open_settings", parsed.intent)
        assertEquals("SettingsTool", parsed.toolName)
        assertEquals("wifi", parsed.arguments["settingType"])
        assertEquals("Opening Wi-Fi settings.", parsed.directResponse)
    }

    @Test
    fun parse_unstructuredPlainText_fallbacksToChatIntent() {
        val raw = "Hello, I am ready to help you."

        val parsed = parser.parse(raw)

        assertEquals("chat", parsed.intent)
        assertNull(parsed.toolName)
        assertEquals("Hello, I am ready to help you.", parsed.directResponse)
    }
}
