package com.hninakari.salemaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hninakari.salemaster.core.ui.theme.AppTheme
import com.hninakari.salemaster.inventory.presentation.InventoryScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                InventoryScreen()
            }
        }
    }
}


@Composable
fun Greeting() {
    Text(
        text = "Hnin Akari"
    )
}
