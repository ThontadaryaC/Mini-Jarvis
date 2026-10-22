package com.minijarvis.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minijarvis.JarvisViewModel
import com.minijarvis.core.JarvisStatus
import com.minijarvis.ui.theme.*

enum class ScreenTab {
    HUD,
    CHAT,
    MEMORY,
    SETTINGS
}

@Composable
fun MainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(ScreenTab.HUD) }
    val status by viewModel.status.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val latestResponse by viewModel.latestResponse.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val soundLevel by viewModel.jarvisCore.speechRecognizerManager.soundLevel.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()

    var showTextInput by remember { mutableStateOf(false) }
    val inputText by viewModel.inputText.collectAsState()

    val quickPrompts = listOf(
        "Open WhatsApp",
        "Open YouTube",
        "Open Instagram",
        "Open ChatGPT",
        "Close all apps",
        "What is the time?"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SiriBackground,
        bottomBar = {
            // Siri Bottom Navigation & Floating Control Pill
            SiriBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.CHAT -> ChatScreen(viewModel = viewModel)
                ScreenTab.MEMORY -> MemoryScreen(viewModel = viewModel)
                ScreenTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                ScreenTab.HUD -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Top HUD Bar
                        TopHUDBar(
                            status = status,
                            isOnline = isOnline,
                            onLaunchFloatingOverlay = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                                    !android.provider.Settings.canDrawOverlays(viewModel.getApplication())
                                ) {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${viewModel.getApplication<android.app.Application>().packageName}")
                                    ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    viewModel.getApplication<android.app.Application>().startActivity(intent)
                                } else {
                                    com.minijarvis.overlay.JarvisFloatingService.start(viewModel.getApplication())
                                }
                            }
                        )

                        // Confirmation Alert if pending
                        pendingConfirmation?.let { confirmation ->
                            Spacer(modifier = Modifier.height(12.dp))
                            ConfirmationCard(
                                confirmation = confirmation,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Accessibility Notification Pill if not enabled
                        if (!com.minijarvis.overlay.RubyAccessibilityService.isEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF332005)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(12.dp))
                                    .clickable {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        viewModel.getApplication<android.app.Application>().startActivity(intent)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ Enable Ruby in Accessibility for 1-tap WhatsApp auto-send & app closing",
                                        color = Color(0xFFFFE0B2),
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "TURN ON",
                                        color = Color(0xFFFFB74D),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(0.7f))

                        // 2. Siri Iridescent Orb
                        JarvisOrb(
                            status = status,
                            size = 230.dp,
                            soundLevel = soundLevel,
                            onClick = { viewModel.toggleVoice() }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 3. Siri Status & Headline
                        Text(
                            text = when (status) {
                                JarvisStatus.LISTENING -> "Listening..."
                                JarvisStatus.THINKING -> "Processing..."
                                JarvisStatus.EXECUTING -> "Executing..."
                                JarvisStatus.SPEAKING -> "Responding..."
                                JarvisStatus.OFFLINE -> "Offline Mode"
                                else -> "What can I help you with?"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontSize = 22.sp,
                            color = SiriTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4. Floating Siri Glass Response Card
                        AnimatedVisibility(
                            visible = latestResponse.isNotBlank(),
                            enter = fadeIn() + slideInVertically { it / 2 },
                            exit = fadeOut() + slideOutVertically { it / 2 }
                        ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SiriSurfaceGlass),
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .border(1.dp, SiriBorderGlass, RoundedCornerShape(20.dp))
                            ) {
                                Text(
                                    text = latestResponse,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = SiriTextPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // 5. Quick Action Chips
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickPrompts) { prompt ->
                                QuickPromptChip(
                                    text = prompt,
                                    onClick = { viewModel.submitUserInput(prompt) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 6. Floating Siri Input Dock (Mic / Keyboard Toggle)
                        if (showTextInput) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { viewModel.onInputTextChanged(it) },
                                    placeholder = { Text("Ask anything...", color = SiriTextMuted) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SiriCyan,
                                        unfocusedBorderColor = SiriBorderGlass,
                                        focusedContainerColor = SiriSurfaceGlass,
                                        unfocusedContainerColor = SiriSurfaceGlass,
                                        focusedTextColor = SiriTextPrimary,
                                        unfocusedTextColor = SiriTextPrimary
                                    ),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        viewModel.submitUserInput()
                                        showTextInput = false
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(SiriCyan)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.Black
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(SiriSurfaceGlass)
                                    .border(1.dp, SiriBorderGlass, RoundedCornerShape(32.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mic Button
                                IconButton(
                                    onClick = { viewModel.toggleVoice() },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isListening) Brush.linearGradient(listOf(SiriMagenta, SiriRed))
                                            else Brush.linearGradient(listOf(SiriIndigo, SiriBlue))
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Siri Voice Input",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Keyboard input toggle
                                IconButton(
                                    onClick = { showTextInput = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SiriSurfaceElevated)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Keyboard,
                                        contentDescription = "Keyboard Input",
                                        tint = SiriCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TopHUDBar(
    status: JarvisStatus,
    isOnline: Boolean,
    onLaunchFloatingOverlay: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SiriSurfaceGlass)
                .border(1.dp, SiriBorderGlass, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (status) {
                            JarvisStatus.READY -> SiriCyan
                            JarvisStatus.LISTENING -> SiriBlue
                            JarvisStatus.THINKING -> SiriMagenta
                            JarvisStatus.EXECUTING -> SiriOrange
                            JarvisStatus.SPEAKING -> SiriGreen
                            JarvisStatus.OFFLINE -> SiriTextMuted
                        }
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "RUBY // ${status.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = SiriTextPrimary,
                fontSize = 11.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Floating Orb Mode Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SiriSurfaceGlass)
                    .border(1.dp, SiriCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable(onClick = onLaunchFloatingOverlay)
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SiriCyan)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "FLOATING BALL",
                    style = MaterialTheme.typography.labelSmall,
                    color = SiriCyan,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Online/Offline status badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SiriSurfaceGlass)
                    .border(1.dp, SiriBorderGlass, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) SiriGreen else SiriOrange)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isOnline) "ONLINE" else "OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOnline) SiriGreen else SiriOrange,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun QuickPromptChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SiriSurfaceGlass)
            .border(1.dp, SiriBorderGlass, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = SiriTextPrimary,
            fontSize = 13.sp
        )
    }
}

@Composable
fun SiriBottomBar(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SiriBackground)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SiriTabItem(
            icon = Icons.Default.Mic,
            label = "Siri HUD",
            isSelected = currentTab == ScreenTab.HUD,
            onClick = { onTabSelected(ScreenTab.HUD) }
        )

        SiriTabItem(
            icon = Icons.AutoMirrored.Filled.Chat,
            label = "Logs",
            isSelected = currentTab == ScreenTab.CHAT,
            onClick = { onTabSelected(ScreenTab.CHAT) }
        )

        SiriTabItem(
            icon = Icons.Default.Psychology,
            label = "Memory",
            isSelected = currentTab == ScreenTab.MEMORY,
            onClick = { onTabSelected(ScreenTab.MEMORY) }
        )

        SiriTabItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            isSelected = currentTab == ScreenTab.SETTINGS,
            onClick = { onTabSelected(ScreenTab.SETTINGS) }
        )
    }
}

@Composable
fun SiriTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) SiriCyan else SiriTextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) SiriCyan else SiriTextMuted,
            fontSize = 10.sp
        )
    }
}
