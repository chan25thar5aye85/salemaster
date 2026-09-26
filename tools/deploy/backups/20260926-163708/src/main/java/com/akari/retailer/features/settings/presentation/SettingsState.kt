package com.akari.retailer.features.settings.presentation

data class SettingsState(
    val currentLanguage: String = "en",
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)
