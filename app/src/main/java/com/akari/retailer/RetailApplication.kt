package com.akari.retailer

import android.app.Application
import com.akari.retailer.di.AppContainer
import com.akari.retailer.core.utils.LanguageManager
import com.google.firebase.FirebaseApp

class RetailApplication : Application() {
    
    // Dependency container - accessible from anywhere
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize DI container
        container = AppContainer()
        
        // Apply saved language
        val languageCode = LanguageManager.getCurrentLanguage(this)
        LanguageManager.applyLanguage(this, languageCode)
    }
}
