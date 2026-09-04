package com.akari.retailer.features.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseAddState(
    val title: String = "",
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val description: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class ExpenseAddEvent {
    data class TitleChanged(val value: String) : ExpenseAddEvent()
    data class AmountChanged(val value: String) : ExpenseAddEvent()
    data class CategoryChanged(val value: ExpenseCategory) : ExpenseAddEvent()
    data class DescriptionChanged(val value: String) : ExpenseAddEvent()
    data object SaveExpense : ExpenseAddEvent()
    data object ClearError : ExpenseAddEvent()
    data object ResetSuccess : ExpenseAddEvent()
}

class ExpenseAddViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseAddState())
    val state: StateFlow<ExpenseAddState> = _state.asStateFlow()

    fun handleEvent(event: ExpenseAddEvent) {
        when (event) {
            is ExpenseAddEvent.TitleChanged -> _state.value = _state.value.copy(title = event.value)
            is ExpenseAddEvent.AmountChanged -> _state.value = _state.value.copy(amount = event.value)
            is ExpenseAddEvent.CategoryChanged -> _state.value = _state.value.copy(category = event.value)
            is ExpenseAddEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            ExpenseAddEvent.SaveExpense -> saveExpense()
            ExpenseAddEvent.ClearError -> _state.value = _state.value.copy(error = null)
            ExpenseAddEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun saveExpense() {
        val currentState = _state.value
        
        if (currentState.title.isBlank()) {
            _state.value = _state.value.copy(error = "Title is required")
            return
        }
        
        val amountInt = currentState.amount.toIntOrNull()
        if (amountInt == null || amountInt <= 0) {
            _state.value = _state.value.copy(error = "Enter a valid amount")
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            val expense = Expense(
                title = currentState.title.trim(),
                amount = amountInt,
                category = currentState.category,
                description = currentState.description.trim()
            )
            
            val result = repository.addExpense(expense)
            
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to save expense"
                )
            }
        }
    }
}

class ExpenseAddViewModelFactory(
    private val repository: ExpenseRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseAddViewModel::class.java)) {
            return ExpenseAddViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
