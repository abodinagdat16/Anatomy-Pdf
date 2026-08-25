package com.example.data.translate

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

object GoogleTranslateHelper {
    private const val TAG = "GoogleTranslateHelper"
    const val GOOGLE_TRANSLATE_PACKAGE = "com.google.android.apps.translate"

    /**
     * Checks whether Google Translate (or a system text translation handler) is installed on the device.
     */
    fun isGoogleTranslateAvailable(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            
            // 1. Check direct Google Translate package
            try {
                pm.getPackageInfo(GOOGLE_TRANSLATE_PACKAGE, 0)
                return true
            } catch (_: PackageManager.NameNotFoundException) {
                // Not found by direct package name
            }

            // 2. Check for Process Text activities with "translate" in package
            val processTextIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                type = "text/plain"
            }
            val processActivities = pm.queryIntentActivities(processTextIntent, 0)
            if (processActivities.any { it.activityInfo.packageName.contains("translate", ignoreCase = true) }) {
                return true
            }

            // 3. Check for ACTION_TRANSLATE activities
            val translateIntent = Intent("android.intent.action.TRANSLATE")
            val translateActivities = pm.queryIntentActivities(translateIntent, 0)
            if (translateActivities.isNotEmpty()) {
                return true
            }

            // 4. Check for ACTION_SEND with translate package
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                `package` = GOOGLE_TRANSLATE_PACKAGE
            }
            val sendActivities = pm.queryIntentActivities(sendIntent, 0)
            sendActivities.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Translate availability", e)
            false
        }
    }

    /**
     * Sends the medical text directly to Google Translate dialog/app without requiring an API key.
     */
    fun translateText(context: Context, text: String): Boolean {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return false

        try {
            // Priority 1: Launch via ACTION_PROCESS_TEXT targeting Google Translate (opens Tap-to-Translate / floating overlay)
            val processIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, cleanText)
                putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                `package` = GOOGLE_TRANSLATE_PACKAGE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (isIntentResolvable(context, processIntent)) {
                context.startActivity(processIntent)
                return true
            }

            // Priority 2: Launch via ACTION_SEND targeting Google Translate
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, cleanText)
                `package` = GOOGLE_TRANSLATE_PACKAGE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (isIntentResolvable(context, sendIntent)) {
                context.startActivity(sendIntent)
                return true
            }

            // Priority 3: System ACTION_TRANSLATE
            val systemTranslateIntent = Intent("android.intent.action.TRANSLATE").apply {
                putExtra("android.intent.extra.TEXT", cleanText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (isIntentResolvable(context, systemTranslateIntent)) {
                context.startActivity(systemTranslateIntent)
                return true
            }

            // Priority 4: Web Browser Fallback to Google Translate if app not installed
            val encodedText = Uri.encode(cleanText)
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://translate.google.com/?sl=auto&tl=auto&text=$encodedText&op=translate")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error launching Google Translate", e)
            return false
        }
    }

    private fun isIntentResolvable(context: Context, intent: Intent): Boolean {
        return try {
            val activities = context.packageManager.queryIntentActivities(intent, 0)
            activities.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
