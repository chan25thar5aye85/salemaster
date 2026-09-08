package com.akari.retailer.features.sales.presentation.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.sales.data.repository.IncomeEntryRepository
import com.akari.retailer.features.sales.data.repository.IncomeStreamRepository
import com.akari.retailer.features.sales.domain.models.IncomeEntry
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class IncomeEntryViewModel(
    private val incomeEntryRepository: IncomeEntryRepository,
    private val incomeStreamRepository: IncomeStreamRepository
) : ViewModel() {

    private val _state = MutableStateFlow(IncomeEntryState())
    val state: StateFlow<IncomeEntryState> = _state.asStateFlow()

    init {
        loadStreams()
    }

    fun handleEvent(event: IncomeEntryEvent) {
        when (event) {
            is IncomeEntryEvent.AmountChanged -> _state.value = _state.value.copy(amount = event.value)
            is IncomeEntryEvent.StreamSelected -> _state.value = _state.value.copy(selectedStream = event.stream)
            is IncomeEntryEvent.EntryTypeChanged -> _state.value = _state.value.copy(entryType = event.type)
            is IncomeEntryEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            is IncomeEntryEvent.DateChanged -> _state.value = _state.value.copy(date = event.date)
            IncomeEntryEvent.SaveIncome -> saveIncome()
            IncomeEntryEvent.ClearError -> _state.value = _state.value.copy(error = null)
            IncomeEntryEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadStreams() {
        viewModelScope.launch {
            try {
                incomeStreamRepository.getIncomeStreams().collect { streams ->
                    _state.value = _state.value.copy(streams = streams)
                }
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    private fun saveIncome() {
        val currentState = _state.value
        
        val amountInt = currentState.amount.toIntOrNull()
        if (amountInt == null || amountInt <= 0) {
            _state.value = _state.value.copy(error = "Enter a valid amount")
            return
        }
        
        if (currentState.selectedStream == null) {
            _state.value = _state.value.copy(error = "Select an income stream")
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            val entry = IncomeEntry(
                amount = amountInt,
                incomeStreamId = currentState.selectedStream.id,
                description = currentState.description.trim(),
                type = currentState.entryType,
                date = currentState.date
            )
            
            val result = incomeEntryRepository.addIncomeEntry(entry)
            
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to save income"
                )
            }
        }
    }
}

class IncomeEntryViewModelFactory(
    private val incomeEntryRepository: IncomeEntryRepository,
    private val incomeStreamRepository: IncomeStreamRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncomeEntryViewModel::class.java)) {
            return IncomeEntryViewModel(incomeEntryRepository, incomeStreamRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
