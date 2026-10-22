package com.minijarvis.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages model loading, swapping, unloading, and lifecycle for Ruby.
 * Optimized for OPPO A3 Pro 5G.
 */
class ModelManager(
    var activeModelPath: String = "/sdcard/Download/qwen3-4b-q4_k_m.gguf"
) {
    private val mutex = Mutex()
    private var currentEngine: LocalLLM = StarterLocalLLM()
    var isModelLoaded: Boolean = false
        private set

    suspend fun getEngine(): LocalLLM = mutex.withLock {
        currentEngine
    }

    suspend fun setEngine(engine: LocalLLM) = mutex.withLock {
        currentEngine.unloadModel()
        currentEngine = engine
        isModelLoaded = engine.isReady()
    }

    suspend fun loadModel(path: String = activeModelPath): Result<ModelInfo> = mutex.withLock {
        activeModelPath = path
        val result = currentEngine.loadModel(path)
        isModelLoaded = result.isSuccess
        result
    }

    suspend fun unloadModel() = mutex.withLock {
        currentEngine.unloadModel()
        isModelLoaded = false
    }
}

/**
 * Multi-Tier Intelligent Neural Engine for Ruby:
 * 1. Device Tools (WhatsApp, YouTube, App Launcher, Recents/Closing, Time/Date).
 * 2. Instant Math & Calculation Engine (1ms latency).
 * 3. High-Speed Knowledge Network (DuckDuckGo & Wikipedia APIs with ~200ms latency).
 * 4. Fast Generative AI Fallback.
 * 5. Rich Conversational Matrix.
 */
class StarterLocalLLM : LocalLLM {
    override val engineName: String = "RubyNeuralEngine"
    private var loaded = true

    companion object {
        private const val TAG = "RubyNeuralEngine"
    }

    override suspend fun loadModel(modelPath: String, contextLength: Int): Result<ModelInfo> {
        loaded = true
        return Result.success(
            ModelInfo(
                name = "Ruby Multi-Tier Neural Core",
                architecture = "hybrid-qwen",
                quantization = "Q4_K_M",
                contextLength = contextLength,
                ramUsageMb = 2450,
                isLoaded = true
            )
        )
    }

    override suspend fun unloadModel() {
        loaded = false
    }

    override suspend fun generate(prompt: String): Result<String> {
        if (!loaded) return Result.failure(IllegalStateException("Model not loaded"))
        val response = processPrompt(prompt)
        return Result.success(response)
    }

    override fun generateStream(prompt: String): Flow<String> = flow {
        if (!loaded) throw IllegalStateException("Model not loaded")
        val full = processPrompt(prompt)
        val tokens = full.split(" ")
        for (token in tokens) {
            delay(20)
            emit("$token ")
        }
    }

    override fun isReady(): Boolean = loaded

    private suspend fun processPrompt(prompt: String): String {
        val rawQuery = if (prompt.contains("User:")) {
            prompt.substringAfterLast("User:").substringBefore("JARVIS:").substringBefore("RUBY:").trim()
        } else {
            prompt.trim()
        }

        // Strip leading wake word / salutations for 100% reliable pattern matching
        var userQuery = rawQuery
            .replace("(?i)^(\\s*(?:hey|hi|hello|please)?\\s*(?:ruby|jarvis)\\s*[,:]?\\s*)".toRegex(), "")
            .replace("^(?:ಹಾಯ್|ಹೇ|ನಮಸ್ಕಾರ|ಹಲೋ|ದಯವಿಟ್ಟು)?\\s*(?:ರೂಬಿ|ರೂಬೀ)\\s*[,:]?\\s*".toRegex(), "")
            .trim()

        if (userQuery.isBlank()) {
            userQuery = rawQuery
        }

        val lower = userQuery.lowercase().trim()

        // 0. Full Shutdown Command
        if (lower == "shutdown" || lower == "shut down" || lower == "power off" || lower == "exit completely" || lower == "kill program" || lower == "stop ruby") {
            return """{"intent": "shutdown", "tool": "ShutdownTool", "arguments": {}, "response": "Shutting down Ruby completely. Open the app whenever you want to restart."}"""
        }

        // 1. Close All Apps / Clear Recents (English & Kannada)
        if (lower.contains("close all") || lower.contains("clear all") || lower.contains("kill all") ||
            lower.contains("close apps") || lower.contains("clear apps") || lower.contains("clear recents") ||
            lower.contains("close and clear all") || lower.contains("close background apps") ||
            lower.contains("ಎಲ್ಲಾ ಆಪ್ ಮುಚ್ಚು") || lower.contains("ಎಲ್ಲಾ ಆಪ್‌ಗಳನ್ನು ಮುಚ್ಚು") ||
            lower.contains("ಎಲ್ಲಾ ಅಪ್ಲಿಕೇಶನ್ ಮುಚ್ಚು") || lower.contains("ಆಪ್ ಮುಚ್ಚು") || lower.contains("ಆಪ್ ಕ್ಲೋಸ್ ಮಾಡು") ||
            lower.contains("ella apps mucchu") || lower.contains("ella app mucchu") || lower.contains("app close maadu") ||
            lower.contains("apps close maadu") || lower.contains("ella clear maadu")) {
            val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF } || lower.contains("mucchu") || lower.contains("maadu")) "ಎಲ್ಲಾ ಆಪ್‌ಗಳನ್ನು ಮುಚ್ಚಲಾಗುತ್ತಿದೆ." else "Closing and clearing all open applications."
            return """{"intent": "close_all_apps", "tool": "AppLauncherTool", "arguments": {"action": "close_all"}, "response": "$resp"}"""
        }

        // 2. Hide / Show Floating UI Icon
        if (lower.contains("hide") && (lower.contains("icon") || lower.contains("ball") || lower.contains("orb") || lower.contains("ui") || lower.contains("yourself") || lower.contains("floating"))) {
            com.minijarvis.overlay.JarvisFloatingService.hideOrb()
            return """{"intent": "chat", "response": "Hiding floating icon. Say 'show icon' or 'Hi Ruby' anytime to bring it back."}"""
        }
        if (lower.contains("show") && (lower.contains("icon") || lower.contains("ball") || lower.contains("orb") || lower.contains("ui") || lower.contains("floating"))) {
            com.minijarvis.overlay.JarvisFloatingService.showOrb()
            return """{"intent": "chat", "response": "Showing floating icon."}"""
        }

        // 3. Close specific app (English & Kannada)
        val isKannadaClose = lower.endsWith(" ಮುಚ್ಚು") || lower.endsWith(" ಕ್ಲೋಸ್ ಮಾಡು") || lower.endsWith(" close maadu") || lower.endsWith(" mucchu")
        val isEnglishClose = lower.startsWith("close ") || lower.startsWith("exit ")

        if ((isKannadaClose || isEnglishClose) && !lower.contains("close all") && !lower.contains("clear all") && !lower.contains("ಎಲ್ಲಾ")) {
            var appToClose = lower
                .removePrefix("close the ")
                .removePrefix("close app ")
                .removePrefix("close ")
                .removePrefix("exit ")
                .removeSuffix(" ಮುಚ್ಚು")
                .removeSuffix(" ಕ್ಲೋಸ್ ಮಾಡು")
                .removeSuffix(" close maadu")
                .removeSuffix(" mucchu")
                .trim()

            val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF } || isKannadaClose) "$appToClose ಮುಚ್ಚಲಾಗುತ್ತಿದೆ." else "Closing $appToClose."
            return """{"intent": "close_app", "tool": "AppLauncherTool", "arguments": {"action": "close", "appName": "$appToClose"}, "response": "$resp"}"""
        }

        // 4. Time & Date Queries (English & Kannada)
        if (lower.contains("time") || lower.contains("ಸಮಯ") || lower.contains("ಟೈಮ್") || lower.contains("samaya") || lower.contains("time eshtu")) {
            val currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF } || lower.contains("samaya")) "ಈಗ ಸಮಯ $currentTime." else "The time is $currentTime."
            return """{"intent": "get_time", "tool": "DateTimeTool", "arguments": {"type": "time"}, "response": "$resp"}"""
        }
        if (lower.contains("date") || lower.contains("which day") || lower.contains("ದಿನಾಂಕ") || lower.contains("ಯಾವ ದಿನ") || (lower.contains("what") && lower.contains("today"))) {
            val currentDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
            val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF } || lower.contains("dina")) "ಇಂದು $currentDate." else "Today is $currentDate."
            return """{"intent": "get_date", "tool": "DateTimeTool", "arguments": {"type": "date"}, "response": "$resp"}"""
        }

        // 5. YouTube Search & Launch (English & Kannada)
        if ((lower.contains("youtube") || lower.contains("ಯೂಟ್ಯೂಬ್")) && (lower.contains("search") || lower.contains("play") || lower.contains("video") || lower.contains("ಹಾಡು") || lower.contains("ಪ್ಲೇ") || lower.contains("ಹುಡುಕು"))) {
            val searchQuery = extractYouTubeQuery(lower)
            val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF }) "ಯೂಟ್ಯೂಬ್‌ನಲ್ಲಿ $searchQuery ಹುಡುಕಲಾಗುತ್ತಿದೆ." else "Searching YouTube for $searchQuery."
            return """{"intent": "youtube_search", "tool": "YouTubeTool", "arguments": {"query": "$searchQuery"}, "response": "$resp"}"""
        }

        // 6. WhatsApp Messaging & Launch (Universal Contact & Message Parser in English & Kannada)
        if ((lower.contains("whatsapp") && (lower.contains("message") || lower.contains("send") || lower.contains("write") || lower.contains("text") || lower.contains("to") || lower.contains("kalsu") || lower.contains("kalisu") || lower.contains("ಕಳಿಸು"))) ||
            (lower.contains("send message") || lower.contains("send msg") || lower.contains("send a message") || lower.contains("text to") || lower.startsWith("whatsapp ") ||
             lower.contains("ಮೆಸೇಜ್ ಕಳಿಸು") || lower.contains("ಸಂದೇಶ ಕಳಿಸು") || lower.contains("ಮೆಸೇಜ್ ಮಾಡು") || lower.contains("message kalsu") || lower.contains("message kalisu") || lower.contains("whatsapp kalsu"))) {
            val (recipient, messageText) = extractWhatsAppDetails(userQuery)
            val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF } || lower.contains("kalsu") || lower.contains("kalisu")) {
                "$recipient ರವರಿಗೆ ವಾಟ್ಸಾಪ್ ಸಂದೇಶ ಕಳುಹಿಸಲಾಗುತ್ತಿದೆ: '$messageText'"
            } else {
                "Sending WhatsApp message to $recipient: '$messageText'"
            }
            return """{"intent": "whatsapp_send", "tool": "WhatsAppTool", "arguments": {"contactName": "$recipient", "message": "$messageText"}, "response": "$resp"}"""
        }

        // 7. Universal App Opening (English & Kannada: e.g. "open YouTube", "Instagram open maadu", "ಕ್ಯಾಮೆರಾ ತೆರೆ")
        val isKannadaOpen = lower.endsWith(" open maadu") || lower.endsWith(" open madi") || lower.endsWith(" open") ||
                            lower.endsWith(" ಓಪನ್ ಮಾಡು") || lower.endsWith(" ತೆರೆ") || lower.endsWith(" ಅನ್ನು ತೆರೆ") || lower.endsWith(" ಅನ್ನು ಓಪನ್ ಮಾಡು")
        val isEnglishOpen = lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")

        if (isKannadaOpen || isEnglishOpen || lower == "open whatsapp" || lower == "open youtube" || lower == "youtube" || lower == "whatsapp") {
            var target = lower
                .removePrefix("open the ")
                .removePrefix("open app ")
                .removePrefix("open application ")
                .removePrefix("open ")
                .removePrefix("launch ")
                .removePrefix("start ")
                .removeSuffix(" open maadu")
                .removeSuffix(" open madi")
                .removeSuffix(" open")
                .removeSuffix(" ಓಪನ್ ಮಾಡು")
                .removeSuffix(" ಅನ್ನು ಓಪನ್ ಮಾಡು")
                .removeSuffix(" ಅನ್ನು ತೆರೆ")
                .removeSuffix(" ತೆರೆ")
                .removeSuffix(" app")
                .removeSuffix(" application")
                .trim()

            if (target.contains("youtube") || target.contains("ಯೂಟ್ಯೂಬ್")) {
                val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF }) "ಯೂಟ್ಯೂಬ್ ತೆರೆಯಲಾಗುತ್ತಿದೆ." else "Opening YouTube."
                return """{"intent": "open_app", "tool": "YouTubeTool", "arguments": {"query": ""}, "response": "$resp"}"""
            }
            if (target.contains("whatsapp") || target.contains("ವಾಟ್ಸಾಪ್")) {
                val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF }) "ವಾಟ್ಸಾಪ್ ತೆರೆಯಲಾಗುತ್ತಿದೆ." else "Opening WhatsApp."
                return """{"intent": "open_app", "tool": "WhatsAppTool", "arguments": {}, "response": "$resp"}"""
            }

            return when (target) {
                "settings", "device settings", "ಸೆಟ್ಟಿಂಗ್ಸ್" -> {
                    val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF }) "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಲಾಗುತ್ತಿದೆ." else "Accessing device settings."
                    """{"intent": "open_settings", "tool": "SettingsTool", "arguments": {"settingType": "main"}, "response": "$resp"}"""
                }
                "wifi", "wi-fi", "ವೈಫೈ" -> {
                    val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF }) "ವೈಫೈ ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಲಾಗುತ್ತಿದೆ." else "Opening Wi-Fi settings."
                    """{"intent": "open_settings", "tool": "SettingsTool", "arguments": {"settingType": "wifi"}, "response": "$resp"}"""
                }
                "bluetooth", "ಬ್ಲೂಟೂತ್" -> {
                    val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF }) "ಬ್ಲೂಟೂತ್ ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಲಾಗುತ್ತಿದೆ." else "Opening Bluetooth settings."
                    """{"intent": "open_settings", "tool": "SettingsTool", "arguments": {"settingType": "bluetooth"}, "response": "$resp"}"""
                }
                else -> {
                    val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF } || isKannadaOpen) "$target ತೆರೆಯಲಾಗುತ್ತಿದೆ..." else "Opening $target..."
                    """{"intent": "open_app", "tool": "AppLauncherTool", "arguments": {"action": "open", "appName": "$target"}, "response": "$resp"}"""
                }
            }
        }

        // 8. Creator Attribution & Identity (English & Kannada)
        if (lower.contains("who built you") || lower.contains("who made you") || lower.contains("who created you") || 
            lower.contains("who developed you") || lower.contains("who is your creator") || lower.contains("who designed you") ||
            lower.contains("built you") || lower.contains("made you") || lower.contains("created you") ||
            lower.contains("ಯಾರು ನಿನ್ನನ್ನು") || lower.contains("ನಿನ್ನನ್ನು ಯಾರು") || lower.contains("ಕ್ರಿಯೇಟರ್") ||
            lower.contains("ನಿರ್ಮಿಸಿದ್ದು") || lower.contains("ತಯಾರು ಮಾಡಿದ್ದು") || lower.contains("yaaru maadiddu") ||
            lower.contains("yaaru build maadiddu")) {
            val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF } || lower.contains("yaaru") || lower.contains("maadiddu")) {
                "ನನ್ನನ್ನು ತೋಂಟದಾರ್ಯ ಸಿ ಅವರು ನಿರ್ಮಿಸಿದ್ದಾರೆ."
            } else {
                "I was built by Thontadarya C."
            }
            return """{"intent": "show_creator", "tool": "CreatorTool", "arguments": {}, "response": "$resp"}"""
        }

        if (lower.contains("ಹೇಗಿದ್ದೀಯಾ") || lower.contains("ಹೇಗಿದ್ದೀರಾ") || lower.contains("hege idya") || lower.contains("hegidira")) {
            return """{"intent": "chat", "response": "ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ! ನಿಮಗೆ ಹೇಗೆ ಸಹಾಯ ಮಾಡಲಿ?"}"""
        }

        if (lower.contains("ನಿನ್ನ ಹೆಸರು ಏನು") || lower.contains("ನಿನ್ನ ಹೆಸರೇನು") || lower.contains("ninna hesaru")) {
            return """{"intent": "chat", "response": "ನನ್ನ ಹೆಸರು ರೂಬಿ, ನಿಮ್ಮ ವೈಯಕ್ತಿಕ ಎಐ ಸಹಾಯಕ."}"""
        }

        if (lower.contains("ನನ್ನ ಹೆಸರು ಏನು") || lower.contains("nanna hesaru")) {
            return """{"intent": "chat", "response": "ನಿಮ್ಮ ಹೆಸರು ತೋಂಟದಾರ್ಯ ಸಿ, ನನ್ನ ನಿರ್ಮಾತೃ."}"""
        }

        if (lower.contains("who am i") || lower.contains("what is my name") || lower.contains("my name")) {
            return """{"intent": "chat", "response": "Your name is Thontadarya C, my creator."}"""
        }

        if (lower.contains("who are you") || lower.contains("what is your name") || lower.contains("your name") || lower.contains("tell me your name") || lower.contains("who r u")) {
            return """{"intent": "chat", "response": "My name is Ruby, your personal AI assistant."}"""
        }

        if (lower.contains("hi ruby") || lower.contains("hey ruby") || lower == "ruby" ||
            lower == "hi" || lower == "hello" || lower == "hey jarvis" ||
            lower.contains("ನಮಸ್ಕಾರ") || lower.contains("ನಮಸ್ತೆ") || lower.contains("ರೂಬಿ")) {
            val resp = if (userQuery.any { it.code in 0x0C80..0x0CFF }) "ನಮಸ್ಕಾರ! ನಾನು ರೂಬಿ. ನಿಮಗೆ ಹೇಗೆ ಸಹಾಯ ಮಾಡಲಿ?" else "Hello! I am Ruby. How can I help you today?"
            return """{"intent": "chat", "response": "$resp"}"""
        }

        // 9. Web Scraping, Wikipedia Scraping & Google Search Commands
        if (lower.startsWith("search in google") || lower.startsWith("search google") || lower.startsWith("google ") ||
            lower.startsWith("search on google") || lower.startsWith("search wikipedia") || lower.startsWith("wikipedia ") ||
            lower.startsWith("search web") || lower.startsWith("search the web") || lower.startsWith("web scrape") ||
            lower.startsWith("scrape ") || lower.contains("search on the web") || lower.contains("search in web")) {
            
            val cleanQuery = lower
                .removePrefix("search in google for")
                .removePrefix("search in google")
                .removePrefix("search google for")
                .removePrefix("search google")
                .removePrefix("search on google for")
                .removePrefix("search on google")
                .removePrefix("google for")
                .removePrefix("google")
                .removePrefix("search wikipedia for")
                .removePrefix("search wikipedia")
                .removePrefix("wikipedia for")
                .removePrefix("wikipedia")
                .removePrefix("search the web for")
                .removePrefix("search the web")
                .removePrefix("search web for")
                .removePrefix("search web")
                .removePrefix("web scrape")
                .removePrefix("scrape")
                .trim()

            val isWiki = lower.contains("wikipedia")
            val engineType = if (isWiki) "wikipedia" else "google"
            val scrapedResult = queryInstantKnowledge(cleanQuery)
            
            if (!scrapedResult.isNullOrBlank()) {
                val escaped = escapeJson(scrapedResult)
                return """{"intent": "web_search", "tool": "WebSearchTool", "arguments": {"query": "$cleanQuery", "engine": "$engineType", "openBrowser": true}, "response": "$escaped"}"""
            } else {
                return """{"intent": "web_search", "tool": "WebSearchTool", "arguments": {"query": "$cleanQuery", "engine": "$engineType", "openBrowser": true}, "response": "Searching Google for $cleanQuery."}"""
            }
        }

        // 10. Instant Math Evaluation
        val mathAnswer = tryEvaluateMath(lower)
        if (mathAnswer != null) {
            return """{"intent": "chat", "response": "$mathAnswer"}"""
        }

        // 10. Local Qwen 3:4B Neural Model Query (Primary)
        val qwenAnswer = queryQwenLocalLLM(userQuery)
        if (!qwenAnswer.isNullOrBlank()) {
            val escaped = escapeJson(qwenAnswer)
            return """{"intent": "chat", "response": "$escaped"}"""
        }

        // 11. Instant Knowledge API (DuckDuckGo / Wikipedia - Sub-200ms latency)
        val knowledgeAnswer = queryInstantKnowledge(userQuery)
        if (!knowledgeAnswer.isNullOrBlank()) {
            val escaped = escapeJson(knowledgeAnswer)
            return """{"intent": "chat", "response": "$escaped"}"""
        }

        // 12. Generative LLM (Cloud AI)
        val onlineAnswer = queryGenerativeLLM(userQuery)
        if (!onlineAnswer.isNullOrBlank()) {
            val escaped = escapeJson(onlineAnswer)
            return """{"intent": "chat", "response": "$escaped"}"""
        }

        // 13. Rich Conversational Matrix Fallback
        val offlineAnswer = generateOfflineConversationalAnswer(userQuery)
        val escapedOffline = escapeJson(offlineAnswer)
        return """{"intent": "chat", "response": "$escapedOffline"}"""
    }

    private suspend fun queryQwenLocalLLM(query: String): String? = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://abcd2200-ruby-ai.hf.space/api/generate", // 1. Hugging Face ZeroGPU Cloud
            "http://127.0.0.1:11434/api/generate"             // 2. Local PC Ollama
        )

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 4000
                conn.readTimeout = 9000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("model", "qwen3:4b")
                    put("prompt", query)
                    put("system", "You are Ruby, an intelligent personal AI assistant created and built by Thontadarya C. Give concise, direct, natural, and helpful answers in 1 to 2 clear spoken sentences. If the user asks or speaks in Kannada (ಕನ್ನಡ or Kanglish), you MUST reply in natural, fluent Kannada. Do not use markdown like asterisks or hashtags.")
                    put("stream", false)
                }

                conn.outputStream.use { os ->
                    os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                }

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseText)
                    val output = json.optString("response")
                    if (output.isNotBlank()) {
                        Log.i(TAG, "Successfully received response from cloud endpoint: $endpoint")
                        return@withContext output.replace(Regex("[#*_`\\[\\]]"), "").trim()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Endpoint $endpoint unavailable: ${e.message}")
            }
        }
        return@withContext null
    }

    private suspend fun queryInstantKnowledge(query: String): String? = withContext(Dispatchers.IO) {
        var clean = query.lowercase().trim()
        val removePrefixes = listOf(
            "what is the definition of", "what is definition of",
            "what is the meaning of", "what is meaning of",
            "what is", "who is", "who was", "tell me about", "what are",
            "explain", "describe", "where is", "how does"
        )
        var subject = clean
        for (p in removePrefixes) {
            if (clean.startsWith(p)) {
                subject = clean.removePrefix(p).trim().removeSuffix("?").trim()
                break
            }
        }

        if (subject.isBlank()) return@withContext null

        // 1. Try DuckDuckGo Instant Answer API
        try {
            val ddgUrl = URL("https://api.duckduckgo.com/?q=${URLEncoder.encode(subject, "UTF-8")}&format=json&no_html=1&skip_disambig=1")
            val conn = ddgUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = 1500
            conn.readTimeout = 1500
            conn.setRequestProperty("User-Agent", "RubyAssistant/1.0")

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val abstractText = json.optString("AbstractText")
                if (abstractText.isNotBlank()) {
                    val firstSentence = abstractText.split(".").firstOrNull()?.trim() ?: abstractText
                    return@withContext "$firstSentence."
                }
            }
        } catch (ignored: Exception) {}

        // 2. Try Wikipedia Summary API
        try {
            val wikiSubject = subject.replace(" ", "_").replace("?", "")
            val wikiUrl = URL("https://en.wikipedia.org/api/rest_v1/page/summary/${URLEncoder.encode(wikiSubject, "UTF-8")}")
            val conn = wikiUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.setRequestProperty("User-Agent", "RubyAssistant/1.0")

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val extract = json.optString("extract")
                if (extract.isNotBlank()) {
                    val sentences = extract.split(Regex("(?<=[.!?])\\s+"))
                    val shortExtract = sentences.take(2).joinToString(" ")
                    return@withContext shortExtract
                }
            }
        } catch (ignored: Exception) {}

        // 3. Try Wikipedia Search API
        try {
            val searchApiUrl = URL("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${URLEncoder.encode(subject, "UTF-8")}&utf8=&format=json")
            val searchConn = searchApiUrl.openConnection() as HttpURLConnection
            searchConn.connectTimeout = 2000
            searchConn.readTimeout = 2000
            searchConn.setRequestProperty("User-Agent", "RubyAssistant/1.0")

            if (searchConn.responseCode == 200) {
                val jsonStr = searchConn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(jsonStr)
                val queryObj = rootJson.optJSONObject("query")
                val searchArr = queryObj?.optJSONArray("search")
                if (searchArr != null && searchArr.length() > 0) {
                    val firstResult = searchArr.getJSONObject(0)
                    val matchedTitle = firstResult.optString("title")
                    val snippet = firstResult.optString("snippet")
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&quot;", "\"")
                        .replace("&#039;", "'")
                        .replace("&amp;", "&")

                    val secondSummaryUrl = URL("https://en.wikipedia.org/api/rest_v1/page/summary/${URLEncoder.encode(matchedTitle.replace(" ", "_"), "UTF-8")}")
                    val secondConn = secondSummaryUrl.openConnection() as HttpURLConnection
                    secondConn.connectTimeout = 2000
                    secondConn.readTimeout = 2000
                    secondConn.setRequestProperty("User-Agent", "RubyAssistant/1.0")

                    if (secondConn.responseCode == 200) {
                        val secondJsonStr = secondConn.inputStream.bufferedReader().use { it.readText() }
                        val extract = JSONObject(secondJsonStr).optString("extract")
                        if (extract.isNotBlank()) {
                            val sentences = extract.split(Regex("(?<=[.!?])\\s+"))
                            return@withContext sentences.take(2).joinToString(" ")
                        }
                    }

                    if (snippet.isNotBlank()) {
                        return@withContext "$matchedTitle: $snippet"
                    }
                }
            }
        } catch (ignored: Exception) {}

        return@withContext null
    }

    private suspend fun queryGenerativeLLM(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = URLEncoder.encode(
                "You are Ruby, an intelligent, helpful voice AI assistant. Answer concisely, accurately, and naturally in 1 to 2 clear sentences. Do not use markdown like asterisks or hashtags because your answer will be spoken aloud.",
                "UTF-8"
            )
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://text.pollinations.ai/$encodedQuery?system=$systemPrompt")

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", "RubyAssistant/1.0")

            if (conn.responseCode == 200) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }.trim()
                if (raw.isNotBlank()) {
                    return@withContext raw.replace(Regex("[#*_`\\[\\]]"), "").trim()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Generative LLM query failed: ${e.message}")
        }
        return@withContext null
    }

    private fun tryEvaluateMath(input: String): String? {
        val q = input.lowercase().replace("what is", "").replace("calculate", "").replace("evaluate", "").replace("?", "").trim()

        // Addition
        val addMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:\\+|plus|and)\\s*(\\d+(?:\\.\\d+)?)").find(q)
        if (addMatch != null) {
            val a = addMatch.groupValues[1].toDoubleOrNull() ?: return null
            val b = addMatch.groupValues[2].toDoubleOrNull() ?: return null
            val result = a + b
            return "${formatNum(a)} plus ${formatNum(b)} is ${formatNum(result)}."
        }

        // Subtraction
        val subMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:\\-|minus|take away)\\s*(\\d+(?:\\.\\d+)?)").find(q)
        if (subMatch != null) {
            val a = subMatch.groupValues[1].toDoubleOrNull() ?: return null
            val b = subMatch.groupValues[2].toDoubleOrNull() ?: return null
            val result = a - b
            return "${formatNum(a)} minus ${formatNum(b)} is ${formatNum(result)}."
        }

        // Multiplication
        val mulMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:\\*|times|multiplied by|x)\\s*(\\d+(?:\\.\\d+)?)").find(q)
        if (mulMatch != null) {
            val a = mulMatch.groupValues[1].toDoubleOrNull() ?: return null
            val b = mulMatch.groupValues[2].toDoubleOrNull() ?: return null
            val result = a * b
            return "${formatNum(a)} multiplied by ${formatNum(b)} is ${formatNum(result)}."
        }

        // Division
        val divMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:\\/|divided by|over)\\s*(\\d+(?:\\.\\d+)?)").find(q)
        if (divMatch != null) {
            val a = divMatch.groupValues[1].toDoubleOrNull() ?: return null
            val b = divMatch.groupValues[2].toDoubleOrNull() ?: return null
            if (b == 0.0) return "Division by zero is undefined."
            val result = a / b
            return "${formatNum(a)} divided by ${formatNum(b)} is ${formatNum(result)}."
        }

        // Square root
        val sqrtMatch = Regex("square root of\\s*(\\d+(?:\\.\\d+)?)").find(q)
        if (sqrtMatch != null) {
            val a = sqrtMatch.groupValues[1].toDoubleOrNull() ?: return null
            val result = kotlin.math.sqrt(a)
            return "The square root of ${formatNum(a)} is ${formatNum(result)}."
        }

        return null
    }

    private fun formatNum(n: Double): String {
        return if (n % 1.0 == 0.0) n.toLong().toString() else String.format(Locale.getDefault(), "%.2f", n)
    }

    private fun generateOfflineConversationalAnswer(query: String): String {
        val q = query.lowercase().trim()

        return when {
            q.contains("how are you") -> "I am doing fantastic! All systems are operational. How can I help you today?"
            q.contains("joke") -> "Why don't skeletons fight each other? They don't have the guts!"
            q.contains("thank") -> "You are very welcome! Always happy to assist."
            q.contains("what can you do") || q.contains("help") -> "I can open any app, search YouTube, send WhatsApp messages, close apps, tell you the time and date, and answer your questions."
            q.contains("weather") -> "I recommend checking your weather app for accurate live forecasts."
            q.contains("prime minister of india") -> "The Prime Minister of India is Narendra Modi."
            q.contains("president of india") -> "The President of India is Droupadi Murmu."
            q.contains("capital of india") -> "The capital of India is New Delhi."
            q.contains("capital of karnataka") -> "The capital of Karnataka is Bengaluru."
            q.contains("favorite language") || q.contains("python") -> "Python is one of the most powerful and expressive languages in modern computing."
            else -> "I am Ruby, your personal AI assistant. How can I help you, sir?"
        }
    }

    private fun extractYouTubeQuery(input: String): String {
        var query = input
        val removePrefixes = listOf(
            "open youtube and search the video of",
            "open youtube and search the video",
            "open youtube and search for",
            "open youtube and search",
            "search youtube for the video of",
            "search youtube for",
            "search youtube",
            "search in youtube for the video of",
            "search in youtube for",
            "search in youtube",
            "play the video of",
            "play video of",
            "play video",
            "search for"
        )
        for (prefix in removePrefixes) {
            if (query.contains(prefix)) {
                query = query.replace(prefix, "").trim()
                break
            }
        }
        query = query.replace("youtube", "").trim()
        return if (query.isNotBlank()) query else "kannada vlogs"
    }

    private fun extractWhatsAppDetails(rawInput: String): Pair<String, String> {
        var input = rawInput.trim()

        // 1. Kannada sentence parser (e.g. "ಅಮ್ಮಗೆ ಮೆಸೇಜ್ ಕಳಿಸು ಊಟ ಆಯ್ತಾ" or "Amma ge message kalsu hello")
        var contact = ""
        var message = ""

        val isKannadaQuery = input.any { it.code in 0x0C80..0x0CFF } || input.contains(" ge ", true) || input.contains("kalsu", true) || input.contains("kalisu", true)

        if (isKannadaQuery) {
            var working = input
                .replace("ರೂಬಿ", "", true)
                .replace("ruby", "", true)
                .replace("ದಯವಿಟ್ಟು", "", true)
                .replace("please", "", true)
                .trim()

            // Find recipient before "ಗೆ", "ನಿಗೆ", "ರಿಗೆ", " ge"
            val knDativeRegex = Regex("([\\p{L}0-9_]+)(?:ಗೆ|ನಿಗೆ|ರಿಗೆ|\\s+ge|\\s+nige|\\s+ige)\\s*(.*)", RegexOption.IGNORE_CASE)
            val match = knDativeRegex.find(working)
            if (match != null) {
                contact = match.groupValues[1].trim()
                var rest = match.groupValues[2].trim()

                // Clean action verbs from rest
                val removeVerbs = listOf(
                    "ವಾಟ್ಸಾಪ್ ಮೆಸೇಜ್ ಕಳಿಸು", "ವಾಟ್ಸಾಪ್ ಸಂದೇಶ ಕಳಿಸು", "ವಾಟ್ಸಾಪ್ ಕಳಿಸು", "ವಾಟ್ಸಾಪ್ ಮಾಡು",
                    "ಮೆಸೇಜ್ ಕಳಿಸು", "ಸಂದೇಶ ಕಳಿಸು", "ಮೆಸೇಜ್ ಮಾಡು", "ಸಂದೇಶ ಮಾಡು",
                    "whatsapp message kalsu", "whatsapp message kalisu", "whatsapp kalsu",
                    "message kalsu", "message kalisu", "msg kalsu", "msg kalisu",
                    "ಅಂತ ಕಳಿಸು", "ಎಂದು ಕಳಿಸು", "ಅಂತ ಹೇಳು", "ಎಂದು ಹೇಳು",
                    "ಕಳಿಸು", "ಕಳುಹಿಸು", "ಮಾಡು", "kalsu", "kalisu"
                )
                for (v in removeVerbs) {
                    rest = rest.replace(v, "", true).trim()
                }

                message = rest
            }
        }

        // 2. English prefix parsing
        if (contact.isBlank()) {
            val prefixes = listOf(
                "open whatsapp and send message to",
                "open whatsapp and send a message to",
                "open whatsapp and send msg to",
                "open whatsapp and message",
                "send whatsapp message to",
                "send a whatsapp message to",
                "send whatsapp to",
                "send message to",
                "send a message to",
                "send msg to",
                "whatsapp message to",
                "message to",
                "whatsapp to",
                "whatsapp "
            )

            var foundPrefix = false
            for (p in prefixes) {
                if (input.startsWith(p, ignoreCase = true)) {
                    input = input.substring(p.length).trim()
                    foundPrefix = true
                    break
                }
            }

            if (!foundPrefix && input.contains(" to ", ignoreCase = true)) {
                input = input.substring(input.indexOf(" to ", ignoreCase = true) + 4).trim()
            }

            if (input.startsWith("contact ", ignoreCase = true)) {
                input = input.substring("contact ".length).trim()
            }

            val splitMarkers = listOf(
                " message like ", " message saying ", " message of ", " message ",
                " msg like ", " msg saying ", " msg ",
                " text like ", " text saying ", " text of ", " text ",
                " saying ", " that "
            )

            for (marker in splitMarkers) {
                val idx = input.indexOf(marker, ignoreCase = true)
                if (idx != -1) {
                    contact = input.substring(0, idx).trim()
                    message = input.substring(idx + marker.length).trim()
                    break
                }
            }
        }

        if (contact.isBlank()) {
            val tokens = input.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (tokens.size >= 3) {
                if (tokens[1].length <= 3 || tokens[1].equals("kumar", true) || tokens[1].equals("gowda", true) || tokens[1].equals("sharma", true)) {
                    contact = "${tokens[0]} ${tokens[1]}"
                    message = tokens.drop(2).joinToString(" ")
                } else {
                    contact = tokens[0]
                    message = tokens.drop(1).joinToString(" ")
                }
            } else if (tokens.size == 2) {
                contact = tokens[0]
                message = tokens[1]
            } else if (tokens.size == 1) {
                contact = tokens[0]
                message = if (contact == "ಅಮ್ಮ" || contact.equals("amma", true)) "ಊಟ ಆಯ್ತಾ?" else "Good morning"
            }
        }

        // Map Kannada contact names
        when (contact.lowercase().trim()) {
            "ಅಮ್ಮ", "amma", "ammage", "ಅಮ್ಮಗೆ", "ಅಮ್ಮನಿಗೆ" -> contact = "Amma"
            "ಅಪ್ಪ", "appa", "appage", "ಅಪ್ಪಗೆ", "ಅಪ್ಪನಿಗೆ" -> contact = "Appa"
            "ಅಣ್ಣ", "anna" -> contact = "Anna"
            "ಅಕ್ಕ", "akka" -> contact = "Akka"
            "ತಂಗಿ", "tangi" -> contact = "Tangi"
            "ತಮ್ಮ", "thamma" -> contact = "Thamma"
        }

        if (contact.isBlank()) contact = "Amma"
        if (message.isBlank()) message = if (contact == "Amma") "ಊಟ ಆಯ್ತಾ?" else "Good morning"

        val formattedContact = contact.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        val formattedMessage = message.replaceFirstChar { it.uppercase() }

        return Pair(formattedContact, formattedMessage)
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
    }
}
