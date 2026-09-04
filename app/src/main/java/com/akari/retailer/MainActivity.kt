package com.akari.retailer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.akari.retailer.core.ui.theme.AppTheme
import com.akari.retailer.core.ui.theme.Background
import com.akari.retailer.core.utils.LanguageManager
import com.akari.retailer.navigation.AppNavHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val languageCode = LanguageManager.getCurrentLanguage(this)
        LanguageManager.applyLanguage(this, languageCode)

        // Enable edge-to-edge for Android 15 / targetSdk 35
        enableEdgeToEdge()

        // Transparent system bars
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Prevent Android from adding a navigation-bar contrast scrim
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            AppTheme {

                val view = LocalView.current

                SideEffect {
                    val window =
                        (view.context as ComponentActivity).window

                    val controller =
                        WindowCompat.getInsetsController(window, view)

                    // DARK status-bar icons/text
                    // Good for your light/transparent background
                    controller.isAppearanceLightStatusBars = true

                    // DARK navigation-bar icons/gesture handle
                    controller.isAppearanceLightNavigationBars = true
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    AppNavHost()
                }
            }
        }
    }
}
