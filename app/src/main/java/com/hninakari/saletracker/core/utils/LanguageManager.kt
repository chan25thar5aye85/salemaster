package com.hninakari.saletracker.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {
    private const val PREF_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    
    // Default language is English
    private const val DEFAULT_LANGUAGE = "en"
    
    fun getLanguagePrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun getCurrentLanguage(context: Context): String {
        val prefs = getLanguagePrefs(context)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
    
    fun setLanguage(context: Context, languageCode: String) {
        val prefs = getLanguagePrefs(context)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    fun applyLanguage(context: Context): Context {
        val languageCode = getCurrentLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}
