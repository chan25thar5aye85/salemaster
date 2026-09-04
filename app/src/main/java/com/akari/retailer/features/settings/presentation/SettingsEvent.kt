package com.akari.retailer.features.settings.presentation

sealed class SettingsEvent {
    data class LanguageSelected(val languageCode: String) : SettingsEvent()
    data object ResetSuccess : SettingsEvent()
    data object ClearError : SettingsEvent()
}
