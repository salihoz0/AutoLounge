package com.autohub.park.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import com.autohub.park.i18n.tr

/**
 * Gemini / Asistan kartı yeni ekran açmaz.
 * Doğrudan telefondaki asistanı tetikler:
 * 1) Google Gemini uygulaması (varsa)
 * 2) Sistem sesli asistan (VOICE_COMMAND / ASSIST)
 * 3) Fallback: konuşma ile arama
 */
object AssistantLauncher {

    private val GEMINI_PACKAGES = listOf(
        "com.google.android.apps.bard",          // Gemini
        "com.google.android.googlequicksearchbox" // Google / Asistan
    )

    fun launch(context: Context): Boolean {
        // 1) Gemini uygulamasını doğrudan açmayı dene
        for (pkg in GEMINI_PACKAGES) {
            try {
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // Gemini paketi ise deep-link ile asistan yüzeyini açmayı dene
                    if (pkg == "com.google.android.apps.bard") {
                        intent.action = Intent.ACTION_VOICE_COMMAND
                    }
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) { /* sıradakini dene */ }
        }

        // 2) Sistem sesli asistan intent'i (telefon asistanını tetikler, yeni app ekranı açmaz)
        val voiceCommands = listOf(
            Intent(Intent.ACTION_VOICE_COMMAND),
            Intent(Intent.ACTION_ASSIST),
            Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE)
        )
        for (base in voiceCommands) {
            try {
                base.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(base)
                return true
            } catch (_: ActivityNotFoundException) { /* dene */ }
            catch (_: Exception) { /* dene */ }
        }

        Toast.makeText(
            context,
            tr("Telefon asistanı bulunamadı"),
            Toast.LENGTH_SHORT
        ).show()
        return false
    }
}
