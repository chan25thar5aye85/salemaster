package com.akari.retailer.features.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.domain.usecases.CalculateBusinessProfitUseCase
import com.akari.retailer.features.expense.domain.usecases.ProfitData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProfitLossState(
    val profitData: ProfitData? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ProfitLossViewModel(
    private val calculateProfitUseCase: CalculateBusinessProfitUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfitLossState())
    val state: StateFlow<ProfitLossState> = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val result = calculateProfitUseCase.invoke()
                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        profitData = result.getOrNull(),
                        isLoading = false,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to calculate profit"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to calculate profit"
                )
            }
        }
    }
}

class ProfitLossViewModelFactory(
    private val calculateProfitUseCase: CalculateBusinessProfitUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfitLossViewModel::class.java)) {
            return ProfitLossViewModel(calculateProfitUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
