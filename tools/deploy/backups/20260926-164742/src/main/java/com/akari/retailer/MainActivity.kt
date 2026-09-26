package com.akari.retailer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.akari.retailer.core.ui.theme.AppTheme
import com.akari.retailer.core.ui.theme.Background
import com.akari.retailer.navigation.AppNavHost

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge for Android 15 / targetSdk 35
        enableEdgeToEdge()

        // Transparent system bars
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Prevent Android from adding a navigation-bar contrast scrim
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val activityWindow = window

        setContent {
            AppTheme {

                val view = LocalView.current

                SideEffect {
                    val controller = WindowCompat.getInsetsController(activityWindow, view)

                    // DARK status-bar icons/text (good for light/transparent background)
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
