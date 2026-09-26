package com.akari.retailer.core.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Language manager.
 *
 * Uses AppCompatDelegate.setApplicationLocales() which:
 *  - Works correctly on API 33+ (uses the platform per-app locales)
 *  - Works on API 21-32 via AppCompat's backport
 *  - Automatically recreates the activity — no manual restart needed
 */
object LanguageManager {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_LANGUAGE = "language_code"
    private const val DEFAULT_LANGUAGE = "en"

    fun getCurrentLanguage(context: Context): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            return appLocales.toLanguageTags().substringBefore(",")
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    /**
     * Apply the language. This triggers activity recreation automatically.
     * No need to call restartApp().
     */
    fun applyLanguage(context: Context, languageCode: String) {
        setLanguage(context, languageCode)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageCode)
        )
    }

    /**
     * Ensures a language is applied on app start.
     * Call from Application.onCreate().
     */
    fun syncOnAppStart(context: Context) {
        val saved = getCurrentLanguage(context)
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.isEmpty || current.toLanguageTags() != saved) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(saved)
            )
        }
    }

    fun getAvailableLanguages(): List<LanguageOption> = LANGUAGES

    private val LANGUAGES = listOf(
        LanguageOption("en", "English", "🇬🇧"),
        LanguageOption("my", "မြန်မာ", "🇲🇲")
    )
}

data class LanguageOption(
    val code: String,
    val name: String,
    val flag: String
)
