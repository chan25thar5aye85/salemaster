package com.akari.retailer.features.settings.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.akari.retailer.core.utils.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        SettingsState(
            currentLanguage = LanguageManager.getCurrentLanguage(application)
        )
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LanguageSelected -> changeLanguage(event.languageCode)
            SettingsEvent.ResetSuccess -> resetSuccess()
            SettingsEvent.ClearError -> clearError()
        }
    }

    private fun changeLanguage(languageCode: String) {
        _state.value = _state.value.copy(isLoading = true)
        try {
            // This will trigger activity recreation automatically
            LanguageManager.applyLanguage(application, languageCode)
            _state.value = _state.value.copy(
                currentLanguage = languageCode,
                isLoading = false,
                success = true,
                error = null
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Failed to change language",
                success = false
            )
        }
    }

    private fun resetSuccess() {
        _state.value = _state.value.copy(success = false)
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
