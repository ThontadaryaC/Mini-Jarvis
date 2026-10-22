package com.minijarvis.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Manages runtime Android permissions required for voice, tools, and automation.
 */
class PermissionManager(private val context: Context) {

    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasRequiredBasePermissions(): Boolean {
        return hasRecordAudioPermission()
    }
}
