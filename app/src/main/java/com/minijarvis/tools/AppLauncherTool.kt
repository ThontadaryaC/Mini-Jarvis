package com.minijarvis.tools

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.util.Log
import com.minijarvis.overlay.RubyAccessibilityService
import com.minijarvis.security.SecurityLevel

/**
 * Tool to dynamically launch, close, and manage any installed Android application
 * (e.g. ChatGPT, Gemini, Instagram, YouTube, WhatsApp, Settings, Camera, etc.)
 * and perform global actions like "close all apps".
 */
class AppLauncherTool(context: Context) : AndroidTool(context) {

    override val name: String = "AppLauncherTool"
    override val description: String = "Launches or closes any installed Android app by name, or closes all open apps."
    override val securityLevel: SecurityLevel = SecurityLevel.LEVEL_1_SAFE
    override val parameterSchema: String = """{"action": "open | close | close_all", "appName": "string (optional)", "package": "string (optional)"}"""

    companion object {
        private const val TAG = "AppLauncherTool"

        // Common known aliases for quick lookup in English & Kannada
        private val KNOWN_APP_ALIASES = mapOf(
            "chatgpt" to listOf("com.openai.chatgpt"),
            "gemini" to listOf("com.google.android.apps.bard", "com.google.android.googlequicksearchbox"),
            "instagram" to listOf("com.instagram.android", "com.instagram.lite"),
            "ಇನ್‌ಸ್ಟಾಗ್ರಾಮ್" to listOf("com.instagram.android", "com.instagram.lite"),
            "insta" to listOf("com.instagram.android"),
            "youtube" to listOf("com.google.android.youtube"),
            "ಯೂಟ್ಯೂಬ್" to listOf("com.google.android.youtube"),
            "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "ವಾಟ್ಸಾಪ್" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "chrome" to listOf("com.android.chrome"),
            "ಕ್ರೋಮ್" to listOf("com.android.chrome"),
            "browser" to listOf("com.android.chrome"),
            "maps" to listOf("com.google.android.apps.maps"),
            "ಮ್ಯಾಪ್ಸ್" to listOf("com.google.android.apps.maps"),
            "ನಕ್ಷೆ" to listOf("com.google.android.apps.maps"),
            "spotify" to listOf("com.spotify.music"),
            "telegram" to listOf("org.telegram.messenger"),
            "snapchat" to listOf("com.snapchat.android"),
            "twitter" to listOf("com.twitter.android"),
            "x" to listOf("com.twitter.android"),
            "camera" to listOf("com.oppo.camera", "com.android.camera", "com.google.android.GoogleCamera"),
            "ಕ್ಯಾಮೆರಾ" to listOf("com.oppo.camera", "com.android.camera", "com.google.android.GoogleCamera"),
            "settings" to listOf("com.android.settings", "com.coloros.settings"),
            "ಸೆಟ್ಟಿಂಗ್ಸ್" to listOf("com.android.settings", "com.coloros.settings"),
            "calculator" to listOf("com.coloros.calculator", "com.google.android.calculator", "com.android.calculator2"),
            "ಕ್ಯಾಲ್ಕುಲೇಟರ್" to listOf("com.coloros.calculator", "com.google.android.calculator", "com.android.calculator2"),
            "gallery" to listOf("com.coloros.gallery3d", "com.google.android.apps.photos", "com.android.gallery3d"),
            "ಗ್ಯಾಲರಿ" to listOf("com.coloros.gallery3d", "com.google.android.apps.photos", "com.android.gallery3d"),
            "photos" to listOf("com.google.android.apps.photos", "com.coloros.gallery3d"),
            "ಫೋಟೋಗಳು" to listOf("com.google.android.apps.photos", "com.coloros.gallery3d"),
            "clock" to listOf("com.coloros.alarmclock", "com.google.android.deskclock", "com.android.deskclock"),
            "calendar" to listOf("com.google.android.calendar", "com.coloros.calendar"),
            "files" to listOf("com.coloros.filemanager", "com.google.android.apps.nbu.files"),
            "my files" to listOf("com.coloros.filemanager"),
            "play store" to listOf("com.android.vending"),
            "ಪ್ಲೇ ಸ್ಟೋರ್" to listOf("com.android.vending"),
            "gmail" to listOf("com.google.android.gm"),
            "messages" to listOf("com.google.android.apps.messaging", "com.coloros.mms"),
            "ಸಂದೇಶ" to listOf("com.google.android.apps.messaging", "com.coloros.mms"),
            "phone" to listOf("com.google.android.dialer", "com.oppo.dialer"),
            "ಫೋನ್" to listOf("com.google.android.dialer", "com.oppo.dialer"),
            "dialer" to listOf("com.google.android.dialer", "com.oppo.dialer")
        )
    }

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val action = (arguments["action"] as? String)?.lowercase()?.trim() ?: "open"
        var appNameRaw = (arguments["appName"] as? String)?.lowercase()?.trim()
        val targetPackage = (arguments["package"] as? String)?.trim()

        // Clean app name
        if (appNameRaw != null) {
            appNameRaw = appNameRaw
                .removePrefix("the ")
                .removePrefix("app ")
                .removePrefix("application ")
                .removeSuffix(" app")
                .removeSuffix(" application")
                .trim()
        }

        Log.d(TAG, "Executing AppLauncherTool action='$action', appName='$appNameRaw', package='$targetPackage'")

        return when (action) {
            "close_all" -> closeAllApps()
            "close" -> closeApp(appNameRaw)
            else -> openApp(appNameRaw, targetPackage)
        }
    }

    private fun closeAllApps(): ToolResult {
        val accessibility = RubyAccessibilityService.instance
        if (accessibility != null) {
            accessibility.closeAllApps()
            return ToolResult.Success("Closed all open applications.")
        } else {
            // Fallback: Go to Home screen
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            return ToolResult.Success("Returning to home screen.")
        }
    }

    private fun closeApp(appName: String?): ToolResult {
        val accessibility = RubyAccessibilityService.instance
        if (accessibility != null) {
            accessibility.goHome()
            return ToolResult.Success("Closed ${appName ?: "application"}.")
        } else {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            return ToolResult.Success("Closed ${appName ?: "application"}.")
        }
    }

    private fun openApp(appNameRaw: String?, targetPackage: String?): ToolResult {
        val pm = context.packageManager

        // 1. Try explicit target package if provided
        if (!targetPackage.isNullOrBlank()) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ToolResult.Success("Opening $targetPackage...")
            }
        }

        if (appNameRaw.isNullOrBlank()) {
            return ToolResult.Failed("No application name specified to open.")
        }

        // 2. Check known package aliases
        val aliases = KNOWN_APP_ALIASES[appNameRaw]
        if (aliases != null) {
            for (pkg in aliases) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolResult.Success("Opening ${appNameRaw.replaceFirstChar { it.uppercase() }}...")
                }
            }
        }

        // 3. Scan all installed launcher apps on device
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveList: List<ResolveInfo> = try {
            pm.queryIntentActivities(launcherIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        var matchedResolveInfo: ResolveInfo? = null

        // Pass A: Exact match on label
        for (info in resolveList) {
            val label = info.loadLabel(pm).toString().lowercase().trim()
            if (label == appNameRaw) {
                matchedResolveInfo = info
                break
            }
        }

        // Pass B: Substring match on label or package name
        if (matchedResolveInfo == null) {
            for (info in resolveList) {
                val label = info.loadLabel(pm).toString().lowercase().trim()
                val pkg = info.activityInfo.packageName.lowercase()
                if (label.contains(appNameRaw) || appNameRaw.contains(label) || pkg.contains(appNameRaw)) {
                    matchedResolveInfo = info
                    break
                }
            }
        }

        if (matchedResolveInfo != null) {
            val pkg = matchedResolveInfo.activityInfo.packageName
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val label = matchedResolveInfo.loadLabel(pm).toString()
                return ToolResult.Success("Opening $label...")
            }
        }

        // 4. Fallback: If app not installed, offer to search in Google Play Store
        try {
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appNameRaw")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
            return ToolResult.Success("Searching for $appNameRaw on Google Play Store...")
        } catch (e: Exception) {
            return ToolResult.NotAvailable("Could not find '$appNameRaw' installed on this device.")
        }
    }
}
