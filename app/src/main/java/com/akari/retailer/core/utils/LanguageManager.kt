package com.akari.retailer.core.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import com.akari.retailer.MainActivity
import java.util.Locale

object LanguageManager {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_LANGUAGE = "language_code"

    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun applyLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun restartApp(activity: Activity) {
        val intent = android.content.Intent(activity, MainActivity::class.java)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
        activity.finish()
        // Don't kill the process - let Android handle it
    }

    fun getAvailableLanguages(): List<LanguageOption> {
        return listOf(
            LanguageOption("en", "English", "🇬🇧"),
            LanguageOption("my", "မြန်မာ", "🇲🇲")
        )
    }
}

data class LanguageOption(
    val code: String,
    val name: String,
    val flag: String
)
