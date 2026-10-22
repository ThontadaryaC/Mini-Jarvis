# MINI JARVIS - Personal Autonomous Android Assistant

MINI JARVIS is an on-device personal AI assistant designed primarily for the **OPPO A3 Pro 5G** (and modern Android devices). It provides natural voice/text interaction, an Apple Siri-inspired fluid visual interface, local memory, multi-step task execution, and a strict 3-tier security confirmation layer.

---

## Architecture Overview

```
MiniJarvis/
├── app/
│   └── src/main/
│       ├── java/com/minijarvis/
│       │   ├── core/           # AgentLoop, JarvisCore, IntentRouter, TaskPlanner, ToolManager
│       │   ├── ai/             # LocalLLM abstraction, ModelManager, PromptManager, LLMResponseParser
│       │   ├── voice/          # SpeechRecognizerManager, TextToSpeechManager, WakeWordManager
│       │   ├── memory/         # Room Database, MemoryEntity, ConversationEntity, MemoryRepository
│       │   ├── tools/          # Tool, ToolResult, AndroidTool, AppLauncherTool, SettingsTool
│       │   ├── security/       # SecurityLevel (Level 1/2/3), ConfirmationManager, PermissionManager
│       │   ├── ui/             # Siri-inspired HUD, JarvisOrb, ChatScreen, SettingsScreen, MemoryScreen
│       │   │   └── theme/      # Siri dark glassmorphic palette, typography, color scheme
│       │   └── MainActivity.kt # Entry point
```

---

## Security Architecture

1. **LEVEL 1 (Safe Actions)**: Launch apps, open settings, search, play/pause media. Executed automatically without confirmation.
2. **LEVEL 2 (Personal Actions)**: Send messages, set reminders, update calendar. Requires explicit user confirmation by default.
3. **LEVEL 3 (Sensitive/Irreversible Actions)**: Delete files, modify security credentials, financial transactions. **ALWAYS** requires explicit user confirmation.

---

## Local AI Model (Qwen3 4B GGUF)

- Model: **Qwen3 4B** (4-bit Quantized GGUF `qwen3:4b`).
- Architecture: Pluggable `LocalLLM` interface allowing seamless swapping between **Qwen**, **Llama**, and **Gemma**.
- Local Path: Models can be placed in device storage (`/sdcard/Download/qwen3-4b-q4_k_m.gguf` or internal app files).

---

## Milestone 1 Implemented Features

1. **Android Project Setup**: Android Studio compatible Gradle Kotlin DSL project with Jetpack Compose & Material 3.
2. **Apple Siri-Style UI**:
   - Swirling iridescent fluid **JARVIS / Siri Orb** with chromatic gradients and animated audio waveforms reacting to speech and status.
   - Dynamic floating Siri glass cards for responses.
   - Quick prompt suggestion chips.
   - Bottom floating input dock with microphone button and keyboard input switch.
3. **Chat / Neural Logs**: Full message history with timestamps, tool execution badges, and confirmation dialog cards.
4. **Room Database Memory**: Local long-term key-value memory store and episodic conversation history.
5. **Settings Screen**: Model path configuration, voice toggles, wake word switches, and device telemetry.
6. **Agent Loop & Tools**: Base `AgentLoop`, `ToolManager`, `AppLauncherTool`, and `SettingsTool`.
7. **Unit Tests**: Full test suite for response parsing, tool execution gating, and security level verification.

---

## How to Test on OPPO A3 Pro 5G

### Method 1: Android Studio
1. Open Android Studio.
2. Select **Open** and choose the `Mini-Jarvis` folder (`c:\Users\cool\Desktop\Mini-Jarvis`).
3. Connect your OPPO A3 Pro 5G via USB with **USB Debugging** enabled in Developer Options.
4. Click **Run** (`Shift + F10`) to install and launch the app.

### Method 2: Command Line (Gradle)
1. Run `./gradlew assembleDebug`
2. The generated APK will be at:
   `app/build/outputs/apk/debug/app-debug.apk`
3. Install on phone:
   `adb install -r app/build/outputs/apk/debug/app-debug.apk`

---

## Verification Steps on Device

1. **Orb Animation**:
   - Observe the ambient iridescent breathing glow on the main Siri HUD.
   - Tap the microphone button or the Orb -> Status switches to `LISTENING` with wave oscillations.
2. **Text / Voice Commands**:
   - Tap the keyboard icon in the bottom dock.
   - Type `"Open YouTube"` -> JARVIS announces the action and launches YouTube.
   - Type `"Open Settings"` or `"Wi-Fi Settings"` -> JARVIS opens device settings.
   - Type `"Remember my favorite language is Python"` -> Stored in Room memory.
3. **Memory Screen**:
   - Tap the **Memory** tab on the bottom navigation.
   - Verify the stored preference is visible; test adding and deleting memories.
4. **Logs / Chat**:
   - Tap the **Logs** tab to inspect raw neural logs and tool execution cards.
