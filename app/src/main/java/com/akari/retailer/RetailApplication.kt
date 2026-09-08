package com.akari.retailer

import android.app.Application
import com.akari.retailer.di.AppContainer
import com.akari.retailer.core.utils.LanguageManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RetailApplication : Application() {
    
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize DI container
        container = AppContainer()
        
        // Seed default categories
        CoroutineScope(Dispatchers.IO).launch {
            container.categoryRepository.seedDefaultCategories()
        }
        
        // Apply saved language
        val languageCode = LanguageManager.getCurrentLanguage(this)
        LanguageManager.applyLanguage(this, languageCode)
    }
}
