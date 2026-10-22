package com.minijarvis.tools

import android.content.Context

/**
 * Base class for all tools interacting with Android system services, Intents, or apps.
 */
abstract class AndroidTool(
    protected val context: Context
) : Tool
