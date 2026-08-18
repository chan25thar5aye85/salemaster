package com.hninakari.saletracker.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light theme - vibrant colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),        // Vibrant Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3D),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF202020),
    surface = Color.White,
    onSurface = Color(0xFF202020),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF5F6368),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

// Dark theme - vibrant colors that work on dark background
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),        // Lighter blue for dark theme
    onPrimary = Color(0xFF001B3D),
    primaryContainer = Color(0xFF1A3A5C),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFF9AA0A6),
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as androidx.activity.ComponentActivity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val barColor = if (darkTheme) Color.Black else Color.White
                window.statusBarColor = barColor.toArgb()
                window.navigationBarColor = barColor.toArgb()
            }
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
