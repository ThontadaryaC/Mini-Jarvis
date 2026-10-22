package com.minijarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minijarvis.JarvisViewModel
import com.minijarvis.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var modelPath by remember { mutableStateOf(viewModel.jarvisCore.modelManager.activeModelPath) }
    var voiceResponseEnabled by remember { mutableStateOf(true) }
    var wakeWordEnabled by remember { mutableStateOf(false) }
    var confirmPersonalActions by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SYSTEM CONFIGURATION // SETTINGS",
            style = MaterialTheme.typography.labelSmall,
            color = JarvisCyan
        )

        // 1. Local AI Model Config
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisSurfaceElevated, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "AI Model",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOCAL NEURAL MODEL",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 15.sp,
                        color = JarvisTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Active Architecture: Qwen3 4B (4-bit GGUF)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelPath,
                    onValueChange = {
                        modelPath = it
                        viewModel.jarvisCore.modelManager.activeModelPath = it
                    },
                    label = { Text("GGUF Weights Storage Path", color = JarvisTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisSurfaceElevated,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    singleLine = true
                )
            }
        }

        // 2. Voice & Audio Settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisSurfaceElevated, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = "Voice Settings",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VOICE SYNTHESIS & RECOGNITION",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 15.sp,
                        color = JarvisTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Voice Responses (TTS)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Speak back responses automatically",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = JarvisTextMuted
                        )
                    }
                    Switch(
                        checked = voiceResponseEnabled,
                        onCheckedChange = { voiceResponseEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Wake Word ('Hey Jarvis')",
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Experimental background listener",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.sp,
                            color = JarvisTextMuted
                        )
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = {
                            wakeWordEnabled = it
                            viewModel.jarvisCore.wakeWordManager.setEnabled(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
                    )
                }
            }
        }

        // 3. Accessibility & Global App Actions
        val context = androidx.compose.ui.platform.LocalContext.current
        val isAccessibilityEnabled = com.minijarvis.overlay.RubyAccessibilityService.isEnabled

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SiriBorderGlass, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SiriSurfaceGlass),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Accessibility",
                        tint = if (isAccessibilityEnabled) SiriGreen else SiriOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "APP AUTOMATION & CLOSING",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 15.sp,
                        color = SiriTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isAccessibilityEnabled) 
                        "Accessibility service is ACTIVE. Ruby can close apps, clear recent apps, and auto-dispatch WhatsApp messages."
                    else 
                        "Enable Ruby Accessibility Service to allow 'Close all apps' and automated WhatsApp sending.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SiriTextSecondary,
                    fontSize = 13.sp
                )

                if (!isAccessibilityEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SiriCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Enable Ruby Accessibility", color = androidx.compose.ui.graphics.Color.Black, fontSize = 13.sp)
                    }
                }
            }
        }

        // 4. Target Device Info (OPPO A3 Pro 5G)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisSurfaceElevated, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Device Specs",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEVICE TELEMETRY",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 15.sp,
                        color = JarvisTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Hardware Target: OPPO A3 Pro 5G\nCompute: Dimensity 7050 Octa-Core\nRAM Optimization: Quantized 4-bit GGUF\nArchitecture: Clean Modular Kotlin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisTextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
