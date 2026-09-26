package com.akari.retailer

import android.app.Application
import android.util.Log
import com.akari.retailer.di.AppContainer
import com.akari.retailer.core.utils.LanguageManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RetailApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Init Firebase FIRST
        FirebaseApp.initializeApp(this)

        // 2. Configure Firestore ONCE, before anything else touches it
        try {
            FirebaseFirestore.getInstance().firestoreSettings =
                FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
        } catch (e: Exception) {
            Log.w("RetailApplication", "Firestore settings already set: ${e.message}")
        }

        // 3. DI container
        container = AppContainer(this)

        // 4. Seed one starter entry per user-definable list, if empty.
        //    Names are localized at seed time using the device language.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val uncategorized = getString(R.string.uncategorized)

                container.categoryRepository.seedIfEmpty(
                    name = uncategorized,
                    icon = "📌",
                    color = "#636E72"
                )
                container.incomeStreamRepository.seedIfEmpty(
                    name = uncategorized,
                    icon = "💰",
                    color = "#636E72"
                )
                container.moneyAccountRepository.seedDefaultAccounts()
            }.onFailure { Log.e("RetailApplication", "Seeding failed", it) }
        }

        // 5. Apply saved language (AppCompat handles activity recreation automatically)
        LanguageManager.syncOnAppStart(this)
    }
}
