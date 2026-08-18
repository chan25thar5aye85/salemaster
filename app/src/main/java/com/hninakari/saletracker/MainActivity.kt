package com.hninakari.saletracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hninakari.saletracker.core.navigation.AppNavGraph
import com.hninakari.saletracker.core.ui.theme.AppTheme
import com.hninakari.saletracker.core.utils.LanguageManager
import com.hninakari.saletracker.sales.presentation.viewmodels.SaleViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val viewModel = remember { SaleViewModel(context = this) }
                    AppNavGraph(viewModel = viewModel)
                }
            }
        }
    }
    
    // Apply language when activity is created
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }
}
