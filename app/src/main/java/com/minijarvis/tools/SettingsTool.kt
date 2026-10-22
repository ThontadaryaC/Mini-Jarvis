package com.minijarvis.tools

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.minijarvis.security.SecurityLevel

/**
 * Tool to open specific Android system settings panels.
 */
class SettingsTool(context: Context) : AndroidTool(context) {

    override val name: String = "SettingsTool"
    override val description: String = "Opens Android Settings panels such as Wi-Fi, Bluetooth, Sound, Display, or General Settings."
    override val securityLevel: SecurityLevel = SecurityLevel.LEVEL_1_SAFE
    override val parameterSchema: String = """{"settingType": "wifi | bluetooth | sound | display | accessibility | main"}"""

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val settingType = (arguments["settingType"] as? String)?.lowercase() ?: "main"
        val action = when (settingType) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult.Success("Opened $settingType settings.")
        } catch (e: Exception) {
            ToolResult.Failed("Unable to open $settingType settings: ${e.message}")
        }
    }
}
