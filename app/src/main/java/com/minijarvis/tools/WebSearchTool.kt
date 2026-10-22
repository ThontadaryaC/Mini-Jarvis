package com.minijarvis.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.minijarvis.security.SecurityLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * High-Speed Web & Wikipedia Scraping Tool for Ruby.
 * Performs live web scraping, Wikipedia extraction, and Google Search.
 */
class WebSearchTool(context: Context) : AndroidTool(context) {

    override val name: String = "WebSearchTool"
    override val description: String = "Scrapes Wikipedia and the web for information, or searches Google."
    override val securityLevel: SecurityLevel = SecurityLevel.LEVEL_1_SAFE
    override val parameterSchema: String = """{"query": "string", "engine": "string (optional: google, wikipedia, web)", "openBrowser": "boolean (optional)"}"""

    companion object {
        private const val TAG = "WebSearchTool"
    }

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val query = (arguments["query"] as? String)?.trim() ?: ""
        val engine = (arguments["engine"] as? String)?.lowercase()?.trim() ?: "web"
        val openBrowser = arguments["openBrowser"] as? Boolean ?: false

        if (query.isBlank()) {
            return ToolResult.Failed("Search query cannot be empty.")
        }

        return withContext(Dispatchers.IO) {
            try {
                var scrapedAnswer: String? = null

                // 1. If Wikipedia explicitly requested or default web search
                if (engine.contains("wiki") || engine == "web") {
                    scrapedAnswer = scrapeWikipedia(query)
                }

                // 2. If no Wikipedia match, perform live web scraping (DuckDuckGo Instant & HTML extract)
                if (scrapedAnswer.isNullOrBlank()) {
                    scrapedAnswer = scrapeWeb(query)
                }

                // 3. If openBrowser is requested, launch Google in Chrome
                if (openBrowser) {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }

                if (!scrapedAnswer.isNullOrBlank()) {
                    ToolResult.Success(scrapedAnswer)
                } else {
                    // Fallback to opening Google search in browser
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolResult.Success("Searching Google for $query.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Web scraping failed", e)
                ToolResult.Failed("Search error: ${e.message}")
            }
        }
    }

    suspend fun scrapeWikipedia(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val clean = query.replace("?", "").trim()
            // Try direct summary first
            val summaryUrl = URL("https://en.wikipedia.org/api/rest_v1/page/summary/${URLEncoder.encode(clean.replace(" ", "_"), "UTF-8")}")
            val conn = summaryUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("User-Agent", "RubyAssistant/1.0")

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val extract = json.optString("extract")
                if (extract.isNotBlank()) {
                    val sentences = extract.split(Regex("(?<=[.!?])\\s+"))
                    return@withContext sentences.take(2).joinToString(" ")
                }
            }

            // If direct page not found, perform Wikipedia Search API to find best matching article title
            val searchApiUrl = URL("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${URLEncoder.encode(clean, "UTF-8")}&utf8=&format=json")
            val searchConn = searchApiUrl.openConnection() as HttpURLConnection
            searchConn.connectTimeout = 3000
            searchConn.readTimeout = 3000
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
                        .replace(Regex("<[^>]*>"), "") // Strip HTML tags
                        .replace("&quot;", "\"")
                        .replace("&#039;", "'")
                        .replace("&amp;", "&")

                    // Now get extract for the matched title
                    val secondSummaryUrl = URL("https://en.wikipedia.org/api/rest_v1/page/summary/${URLEncoder.encode(matchedTitle.replace(" ", "_"), "UTF-8")}")
                    val secondConn = secondSummaryUrl.openConnection() as HttpURLConnection
                    secondConn.connectTimeout = 3000
                    secondConn.readTimeout = 3000
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
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia scraping error: ${e.message}")
        }
        return@withContext null
    }

    suspend fun scrapeWeb(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query.replace("?", "").trim()
            val ddgUrl = URL("https://api.duckduckgo.com/?q=${URLEncoder.encode(cleanQuery, "UTF-8")}&format=json&no_html=1&skip_disambig=1")
            val conn = ddgUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("User-Agent", "RubyAssistant/1.0")

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val abstractText = json.optString("AbstractText")
                if (abstractText.isNotBlank()) {
                    val sentences = abstractText.split(Regex("(?<=[.!?])\\s+"))
                    return@withContext sentences.take(2).joinToString(" ")
                }

                // Check Related Topics
                val relatedTopics = json.optJSONArray("RelatedTopics")
                if (relatedTopics != null && relatedTopics.length() > 0) {
                    val firstTopic = relatedTopics.optJSONObject(0)
                    val text = firstTopic?.optString("Text")
                    if (!text.isNullOrBlank()) {
                        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
                        return@withContext sentences.take(2).joinToString(" ")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Web scrape error: ${e.message}")
        }
        return@withContext null
    }
}
