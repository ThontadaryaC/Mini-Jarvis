package com.minijarvis.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.minijarvis.overlay.RubyAccessibilityService
import com.minijarvis.security.SecurityLevel
import java.net.URLEncoder

/**
 * Tool to compose and send WhatsApp messages directly to ANY contact or phone number.
 * No confirmation blocking. Integrates Contact lookup and RubyAccessibilityService for auto-send dispatch.
 */
class WhatsAppTool(context: Context) : AndroidTool(context) {

    override val name: String = "WhatsAppTool"
    override val description: String = "Opens WhatsApp and prepares or sends a message to any contact or phone number."
    override val securityLevel: SecurityLevel = SecurityLevel.LEVEL_1_SAFE
    override val parameterSchema: String = """{"contactName": "string (optional)", "phoneNumber": "string (optional)", "message": "string"}"""

    companion object {
        private const val TAG = "WhatsAppTool"
    }

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val contactName = (arguments["contactName"] as? String)?.trim()
        var phoneNumber = (arguments["phoneNumber"] as? String)?.trim()
        val message = (arguments["message"] as? String)?.trim() ?: ""

        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            message
        }

        // 1. If phone number is missing but contactName is given, lookup across all phone contacts
        var matchedDisplayName: String? = null
        if (phoneNumber.isNullOrBlank() && !contactName.isNullOrBlank()) {
            val contactMatch = findContactInPhonebook(contactName)
            if (contactMatch != null) {
                matchedDisplayName = contactMatch.first
                phoneNumber = formatPhoneNumberForWhatsApp(contactMatch.second)
                Log.d(TAG, "Contact lookup: '$contactName' -> found '${matchedDisplayName}' ($phoneNumber)")
            } else {
                Log.d(TAG, "Contact lookup: '$contactName' not found in device address book.")
            }
        } else if (!phoneNumber.isNullOrBlank()) {
            phoneNumber = formatPhoneNumberForWhatsApp(phoneNumber)
        }

        return try {
            if (!phoneNumber.isNullOrBlank()) {
                // Direct WhatsApp chat by phone number
                val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage("com.whatsapp")
                }
                context.startActivity(intent)

                // Trigger accessibility auto-send if active
                if (RubyAccessibilityService.isEnabled) {
                    RubyAccessibilityService.instance?.autoClickSendWhatsApp(delayMs = 1200)
                } else {
                    // Prompt user to enable Accessibility for 1-click auto-dispatch
                    val accIntent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(accIntent)
                }

                val targetLabel = matchedDisplayName ?: contactName ?: phoneNumber
                val suffix = if (RubyAccessibilityService.isEnabled) "" else " (Please turn ON Ruby in Accessibility Settings for automatic 1-tap sending)"
                ToolResult.Success("Sending WhatsApp message to $targetLabel: '$message'$suffix")
            } else if (!contactName.isNullOrBlank()) {
                // If not in phonebook or permission not granted, open WhatsApp share sheet prefilled with message
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(sendIntent)
                ToolResult.Success("Opening WhatsApp for $contactName with message: '$message'")
            } else {
                // Just open WhatsApp
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"))
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ToolResult.Success("Opening WhatsApp...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing WhatsAppTool", e)
            ToolResult.Failed("Could not dispatch WhatsApp message: ${e.message}")
        }
    }

    private fun formatPhoneNumberForWhatsApp(phone: String): String {
        var clean = phone.replace(Regex("[^0-9+]"), "")
        if (clean.startsWith("+")) {
            clean = clean.substring(1)
        }
        // If standard 10-digit Indian mobile number, prepend 91
        if (clean.length == 10 && (clean.startsWith("6") || clean.startsWith("7") || clean.startsWith("8") || clean.startsWith("9"))) {
            clean = "91$clean"
        } else if (clean.length == 11 && clean.startsWith("0")) {
            clean = "91" + clean.substring(1)
        }
        return clean
    }

    private fun findContactInPhonebook(query: String): Pair<String, String>? {
        var cleanQuery = query.lowercase().trim()

        // Strip Kannada grammatical suffixes like "ಗೆ", "ನಿಗೆ", "ರಿಗೆ", " ge", " nige"
        val suffixRemovals = listOf("ನಿಗೆ", "ರಿಗೆ", "ಗೆ", " nige", " rige", " ge", " ige")
        for (s in suffixRemovals) {
            if (cleanQuery.endsWith(s)) {
                cleanQuery = cleanQuery.removeSuffix(s).trim()
                break
            }
        }

        // Map common Kannada terms to English phonetics & synonyms
        val searchAliases = mutableListOf(cleanQuery)
        when (cleanQuery) {
            "ಅಮ್ಮ", "amma", "mom", "mother" -> searchAliases.addAll(listOf("amma", "mom", "mother", "ಅಮ್ಮ", "ma", "mummy", "ammi"))
            "ಅಪ್ಪ", "appa", "dad", "father" -> searchAliases.addAll(listOf("appa", "dad", "father", "ಅಪ್ಪ", "pa", "pappa"))
            "ಅಣ್ಣ", "anna", "brother" -> searchAliases.addAll(listOf("anna", "brother", "ಅಣ್ಣ", "bro"))
            "ಅಕ್ಕ", "akka", "sister" -> searchAliases.addAll(listOf("akka", "sister", "ಅಕ್ಕ", "sis"))
            "ತಂಗಿ", "tangi" -> searchAliases.addAll(listOf("tangi", "sister", "ತಂಗಿ"))
            "ತಮ್ಮ", "thamma" -> searchAliases.addAll(listOf("thamma", "brother", "ತಮ್ಮ"))
        }

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                var bestMatchName: String? = null
                var bestMatchPhone: String? = null
                var bestScore = 0

                while (it.moveToNext()) {
                    val rawName = it.getString(nameIndex) ?: continue
                    val phone = it.getString(numberIndex) ?: continue

                    val cleanContactName = rawName.lowercase().trim()

                    // Check exact or alias match
                    for (alias in searchAliases) {
                        if (cleanContactName == alias) {
                            return Pair(rawName, phone)
                        }
                    }

                    // Score matches
                    var currentScore = 0
                    for (alias in searchAliases) {
                        if (cleanContactName.startsWith(alias) || alias.startsWith(cleanContactName)) {
                            currentScore += 10
                        } else if (cleanContactName.contains(alias) || alias.contains(cleanContactName)) {
                            currentScore += 6
                        }
                    }

                    if (currentScore > bestScore) {
                        bestScore = currentScore
                        bestMatchName = rawName
                        bestMatchPhone = phone
                    }
                }

                if (bestScore >= 5 && bestMatchName != null && bestMatchPhone != null) {
                    return Pair(bestMatchName, bestMatchPhone)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Contacts query exception: ${e.message}")
        }
        return null
    }
}
