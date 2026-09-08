package com.akari.retailer.features.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import com.akari.retailer.features.expense.domain.models.ExpenseType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseAddState(
    val title: String = "",
    val amount: String = "",
    val selectedCategory: ExpenseCategory? = null,
    val categories: List<ExpenseCategory> = emptyList(),
    val expenseType: ExpenseType = ExpenseType.BUSINESS,  // ✅ NEW
    val businessPercentage: String = "100",  // ✅ NEW
    val description: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class ExpenseAddEvent {
    data class TitleChanged(val value: String) : ExpenseAddEvent()
    data class AmountChanged(val value: String) : ExpenseAddEvent()
    data class CategorySelected(val category: ExpenseCategory) : ExpenseAddEvent()
    data class ExpenseTypeChanged(val type: ExpenseType) : ExpenseAddEvent()  // ✅ NEW
    data class BusinessPercentageChanged(val value: String) : ExpenseAddEvent()  // ✅ NEW
    data class DescriptionChanged(val value: String) : ExpenseAddEvent()
    data object SaveExpense : ExpenseAddEvent()
    data object ClearError : ExpenseAddEvent()
    data object ResetSuccess : ExpenseAddEvent()
}

class ExpenseAddViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseAddState())
    val state: StateFlow<ExpenseAddState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    fun handleEvent(event: ExpenseAddEvent) {
        when (event) {
            is ExpenseAddEvent.TitleChanged -> _state.value = _state.value.copy(title = event.value)
            is ExpenseAddEvent.AmountChanged -> _state.value = _state.value.copy(amount = event.value)
            is ExpenseAddEvent.CategorySelected -> _state.value = _state.value.copy(selectedCategory = event.category)
            is ExpenseAddEvent.ExpenseTypeChanged -> _state.value = _state.value.copy(expenseType = event.type)
            is ExpenseAddEvent.BusinessPercentageChanged -> _state.value = _state.value.copy(businessPercentage = event.value)
            is ExpenseAddEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            ExpenseAddEvent.SaveExpense -> saveExpense()
            ExpenseAddEvent.ClearError -> _state.value = _state.value.copy(error = null)
            ExpenseAddEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getCategories().collect { categories ->
                    _state.value = _state.value.copy(categories = categories)
                }
            } catch (e: Exception) {
                // Handle error silently
            }
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
        
        if (currentState.selectedCategory == null) {
            _state.value = _state.value.copy(error = "Select a category")
            return
        }
        
        // Validate business percentage for MIXED type
        if (currentState.expenseType == ExpenseType.MIXED) {
            val percentage = currentState.businessPercentage.toIntOrNull()
            if (percentage == null || percentage < 0 || percentage > 100) {
                _state.value = _state.value.copy(error = "Business percentage must be between 0 and 100")
                return
            }
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            val percentage = currentState.businessPercentage.toIntOrNull() ?: 100
            
            val expense = Expense(
                title = currentState.title.trim(),
                amount = amountInt,
                categoryId = currentState.selectedCategory.id,
                type = currentState.expenseType,
                businessPercentage = if (currentState.expenseType == ExpenseType.MIXED) percentage else 100,
                description = currentState.description.trim()
            )
            
            val result = expenseRepository.addExpense(expense)
            
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
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseAddViewModel::class.java)) {
            return ExpenseAddViewModel(expenseRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
