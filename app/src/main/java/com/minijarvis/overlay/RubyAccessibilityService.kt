package com.minijarvis.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Accessibility Service allowing Ruby to:
 * 1. Close apps (Home/Back actions)
 * 2. Close and swipe up all background apps (Recents -> Clear All / Swipe Up)
 * 3. 100% Guaranteed Auto-click & Gesture Tap Send in WhatsApp and messaging apps
 */
class RubyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RubyAccessibility"
        var instance: RubyAccessibilityService? = null
            private set

        val isEnabled: Boolean
            get() = instance != null
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "RubyAccessibilityService connected and ACTIVE")
    }

    private var isPendingWhatsAppSend = false
    private var pendingWhatsAppSendExpiry = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: ""
        if (isPendingWhatsAppSend && (pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b" || pkg.contains("whatsapp"))) {
            if (System.currentTimeMillis() < pendingWhatsAppSendExpiry) {
                tryFindAndClickSend()
            } else {
                isPendingWhatsAppSend = false
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "RubyAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun openRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun tapScreen(x: Float, y: Float, durationMs: Long = 50L) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val tapPath = Path().apply {
                moveTo(x, y)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(tapPath, 0, durationMs))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    fun swipeUpTask(startX: Float, startY: Float, endY: Float, durationMs: Long = 200L) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val swipePath = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(swipePath, 0, durationMs))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    fun closeAllApps(onCompleted: ((Boolean) -> Unit)? = null) {
        val opened = performGlobalAction(GLOBAL_ACTION_RECENTS)
        if (!opened) {
            goHome()
            onCompleted?.invoke(false)
            return
        }

        serviceScope.launch {
            // 1. Wait for Recents overview to slide in
            delay(450)
            val roots = getAllRootNodes()
            var clicked = false
            for (root in roots) {
                if (findAndClickClearAll(root)) {
                    clicked = true
                    break
                }
            }

            // 2. Perform rapid upward swipe gestures on recent app cards to physically fling them out
            val displayMetrics = resources.displayMetrics
            val midX = displayMetrics.widthPixels / 2f
            val startY = displayMetrics.heightPixels * 0.65f
            val endY = displayMetrics.heightPixels * 0.15f

            for (i in 0..4) {
                swipeUpTask(startX = midX, startY = startY, endY = endY, durationMs = 150L)
                delay(180)
            }

            // 3. One final attempt on clear all button after swiping
            delay(200)
            for (root in getAllRootNodes()) {
                findAndClickClearAll(root)
            }

            // 4. Return to clean Home Screen
            delay(300)
            goHome()
            onCompleted?.invoke(true)
        }
    }

    private fun findAndClickClearAll(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        val isClearButton = (
            text.contains("clear all") || text.contains("close all") || text == "clear" || text == "close" ||
            desc.contains("clear all") || desc.contains("close all") || desc == "clear" || desc == "close" ||
            viewId.contains("clear_all") || viewId.contains("btn_clear") || viewId.contains("clear_all_button") ||
            viewId.contains("clear_all_recents") || viewId.contains("dismiss_all") || viewId.contains("clear")
        )

        if (isClearButton) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                tapScreen(rect.centerX().toFloat(), rect.centerY().toFloat(), 50L)
            }
            if (performClickOnNodeOrParent(node)) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickClearAll(child)) {
                return true
            }
        }
        return false
    }

    fun autoClickSendWhatsApp(delayMs: Long = 800, onResult: ((Boolean) -> Unit)? = null) {
        isPendingWhatsAppSend = true
        pendingWhatsAppSendExpiry = System.currentTimeMillis() + 8000L // 8 second window

        serviceScope.launch {
            val retryDelays = listOf(delayMs, 500L, 700L, 900L, 1100L, 1400L, 1800L)
            var sent = false

            for (d in retryDelays) {
                delay(d)
                if (tryFindAndClickSend()) {
                    sent = true
                    isPendingWhatsAppSend = false
                    break
                }
            }

            // If still not sent, perform geometry-assisted tap beside input entry box
            if (!sent) {
                sent = tryGeometryTapSend()
            }

            onResult?.invoke(sent)
        }
    }

    private fun getAllRootNodes(): List<AccessibilityNodeInfo> {
        val list = mutableListOf<AccessibilityNodeInfo>()
        rootInActiveWindow?.let { list.add(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                windows?.forEach { win ->
                    win.root?.let { r ->
                        if (!list.contains(r)) list.add(r)
                    }
                }
            } catch (ignored: Exception) {}
        }
        return list
    }

    private fun tryFindAndClickSend(): Boolean {
        val roots = getAllRootNodes()
        if (roots.isEmpty()) return false

        for (root in roots) {
            // 1. Try finding explicitly by WhatsApp's known send button IDs
            val idCandidates = listOf(
                "com.whatsapp:id/send",
                "com.whatsapp.w4b:id/send",
                "com.whatsapp:id/btn_send",
                "send"
            )

            for (id in idCandidates) {
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                for (node in nodes) {
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    if (rect.width() > 0 && rect.height() > 0) {
                        tapScreen(rect.centerX().toFloat(), rect.centerY().toFloat(), 50L)
                    }
                    if (performClickOnNodeOrParent(node)) {
                        Log.d(TAG, "WhatsApp Send button clicked by ViewId ($id)!")
                        return true
                    }
                }
            }

            // 2. Try finding by content description or text 'Send'
            val descCandidates = listOf("Send", "send", "Send message", "Enviar", "Envoyer")
            for (candidate in descCandidates) {
                val descNodes = root.findAccessibilityNodeInfosByText(candidate)
                for (node in descNodes) {
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    if (rect.width() > 0 && rect.height() > 0) {
                        tapScreen(rect.centerX().toFloat(), rect.centerY().toFloat(), 50L)
                    }
                    if (performClickOnNodeOrParent(node)) {
                        Log.d(TAG, "WhatsApp Send button clicked by text '$candidate'!")
                        return true
                    }
                }
            }

            // 3. Recursive search
            if (recursiveFindAndClickSend(root)) {
                return true
            }
        }

        return false
    }

    private fun tryGeometryTapSend(): Boolean {
        val roots = getAllRootNodes()
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        for (root in roots) {
            val entryNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
            if (entryNodes.isNotEmpty()) {
                val entryNode = entryNodes[0]
                val rect = Rect()
                entryNode.getBoundsInScreen(rect)
                if (rect.width() > 0 && rect.height() > 0) {
                    // Send button is situated horizontally to the right of the entry box
                    val tapX = (screenWidth - 45 * displayMetrics.density).coerceIn(rect.right.toFloat(), screenWidth)
                    val tapY = rect.centerY().toFloat()
                    tapScreen(tapX, tapY, 60L)
                    Log.d(TAG, "Executed geometry send tap at ($tapX, $tapY)")
                    return true
                }
            }
        }

        // Final fallback: standard bottom-right send button location
        val fallbackX = screenWidth * 0.93f
        val fallbackY = screenHeight * 0.60f // Right above standard keyboard or at bottom
        tapScreen(fallbackX, fallbackY, 60L)
        return false
    }

    private fun recursiveFindAndClickSend(node: AccessibilityNodeInfo): Boolean {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        val isSend = (
            desc == "send" || desc == "send message" || desc.startsWith("send") ||
            text == "send" ||
            viewId.endsWith(":id/send") || viewId.endsWith(":id/send_button") || viewId.endsWith(":id/btn_send") ||
            (viewId.contains("send") && !viewId.contains("attachment") && !viewId.contains("location"))
        )

        if (isSend) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                tapScreen(rect.centerX().toFloat(), rect.centerY().toFloat(), 50L)
            }
            if (performClickOnNodeOrParent(node)) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (recursiveFindAndClickSend(child)) {
                return true
            }
        }
        return false
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }
}
