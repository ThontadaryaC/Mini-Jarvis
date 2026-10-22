package com.minijarvis.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.minijarvis.JarvisApplication
import com.minijarvis.MainActivity
import com.minijarvis.core.JarvisCore
import com.minijarvis.core.JarvisStatus
import com.minijarvis.ui.JarvisOrb
import com.minijarvis.ui.theme.MiniJarvisTheme
import com.minijarvis.ui.theme.SiriBlue
import com.minijarvis.ui.theme.SiriBorderGlass
import com.minijarvis.ui.theme.SiriCyan
import com.minijarvis.ui.theme.SiriIndigo
import com.minijarvis.ui.theme.SiriMagenta
import com.minijarvis.ui.theme.SiriSurfaceGlass
import com.minijarvis.ui.theme.SiriTextMuted
import com.minijarvis.ui.theme.SiriTextPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class JarvisFloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var jarvisCore: JarvisCore
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "jarvis_floating_channel"
        const val NOTIFICATION_ID = 101
        var isRunning = false
            private set

        private val _isOrbVisible = MutableStateFlow(true)
        val isOrbVisible: StateFlow<Boolean> = _isOrbVisible.asStateFlow()

        fun hideOrb() {
            _isOrbVisible.value = false
        }

        fun showOrb() {
            _isOrbVisible.value = true
        }

        fun toggleOrb() {
            _isOrbVisible.value = !_isOrbVisible.value
        }

        const val ACTION_STOP = "com.minijarvis.overlay.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, JarvisFloatingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, JarvisFloatingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        jarvisCore = (application as JarvisApplication).jarvisCore

        // Acquire WakeLock so phone CPU doesn't sleep while listening for "Hi Ruby"
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MiniJarvis::WakeWordListenerLock"
            ).apply {
                acquire(24 * 60 * 60 * 1000L) // 24 hours
            }
        } catch (ignored: Exception) {}

        startForeground(NOTIFICATION_ID, createNotification())
        initFloatingOverlay()

        jarvisCore.speechRecognizerManager.startContinuousListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            isRunning = false
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            } catch (ignored: Exception) {}
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        jarvisCore.speechRecognizerManager.startContinuousListening()
        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ruby Always-On Core",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs 24/7 background voice listener for Hi Ruby"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ruby Active")
            .setContentText("Listening 24/7. Say 'Hi Ruby' to activate.")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initFloatingOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 350
        }

        val composeView = ComposeView(this).apply {
            setContent {
                MiniJarvisTheme {
                    FloatingSiriOrbWidget(
                        jarvisCore = jarvisCore,
                        onDrag = { dx, dy ->
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            try {
                                windowManager.updateViewLayout(floatingView, params)
                            } catch (ignored: Exception) {}
                        },
                        onOpenFullApp = {
                            val intent = Intent(this@JarvisFloatingService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        },
                        onCloseService = {
                            hideOrb()
                        }
                    )
                }
            }
        }

        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)

        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(lifecycleOwner)

        floatingView = composeView
        try {
            windowManager.addView(floatingView, params)
        } catch (ignored: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (ignored: Exception) {}
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (ignored: Exception) {}
        }
        floatingView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingSiriOrbWidget(
    jarvisCore: JarvisCore,
    onDrag: (Float, Float) -> Unit,
    onOpenFullApp: () -> Unit,
    onCloseService: () -> Unit
) {
    val status by jarvisCore.status.collectAsState()
    val latestResponse by jarvisCore.latestResponse.collectAsState()
    val soundLevel by jarvisCore.speechRecognizerManager.soundLevel.collectAsState()
    val isOrbVisible by JarvisFloatingService.isOrbVisible.collectAsState()
    val showCreatorAvatar by jarvisCore.showCreatorAvatar.collectAsState()

    var showMiniResponse by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isOrbVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
            ) {
                // Expandable Response Card
                AnimatedVisibility(
                    visible = showMiniResponse && latestResponse.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .padding(bottom = 8.dp)
                            .border(1.dp, SiriBorderGlass, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SiriSurfaceGlass),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RUBY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SiriCyan,
                                    fontSize = 11.sp
                                )
                                Row {
                                    IconButton(
                                        onClick = onOpenFullApp,
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Expand Full App",
                                            tint = SiriTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { showMiniResponse = false },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = SiriTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = latestResponse,
                                style = MaterialTheme.typography.bodySmall,
                                color = SiriTextPrimary,
                                fontSize = 12.sp,
                                maxLines = 4
                            )
                        }
                    }
                }

                // Horizontal Row containing Creator Avatar (shown for 3s when asked) beside the Glowing Ball
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Creator Avatar (Thontadarya C)
                    AnimatedVisibility(
                        visible = showCreatorAvatar,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CreatorAvatarBadge(size = 76.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }

                    // Apple Siri Glowing Iridescent Ball
                    JarvisOrb(
                        status = status,
                        soundLevel = soundLevel,
                        size = 76.dp,
                        onClick = {
                            if (latestResponse.isNotBlank()) {
                                showMiniResponse = !showMiniResponse
                            } else {
                                jarvisCore.speechRecognizerManager.wakeUpManually()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CreatorAvatarBadge(size: Dp = 76.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        SiriCyan.copy(alpha = 0.8f),
                        SiriIndigo.copy(alpha = 0.9f),
                        Color.Black
                    )
                )
            )
            .border(2.5.dp, SiriCyan, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = com.minijarvis.R.drawable.creator_photo),
            contentDescription = "Thontadarya C (Creator)",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
