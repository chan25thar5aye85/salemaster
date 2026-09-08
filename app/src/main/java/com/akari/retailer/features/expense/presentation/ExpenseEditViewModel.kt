package com.akari.retailer.features.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseEditState(
    val id: String = "",
    val title: String = "",
    val amount: String = "",
    val selectedCategory: ExpenseCategory? = null,
    val categories: List<ExpenseCategory> = emptyList(),
    val description: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class ExpenseEditEvent {
    data class TitleChanged(val value: String) : ExpenseEditEvent()
    data class AmountChanged(val value: String) : ExpenseEditEvent()
    data class CategorySelected(val category: ExpenseCategory) : ExpenseEditEvent()
    data class DescriptionChanged(val value: String) : ExpenseEditEvent()
    data object LoadExpense : ExpenseEditEvent()
    data object SaveExpense : ExpenseEditEvent()
    data object ClearError : ExpenseEditEvent()
    data object ResetSuccess : ExpenseEditEvent()
}

class ExpenseEditViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseId: String
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseEditState(id = expenseId))
    val state: StateFlow<ExpenseEditState> = _state.asStateFlow()

    init {
        loadCategories()
        loadExpense()
    }

    fun handleEvent(event: ExpenseEditEvent) {
        when (event) {
            is ExpenseEditEvent.TitleChanged -> _state.value = _state.value.copy(title = event.value)
            is ExpenseEditEvent.AmountChanged -> _state.value = _state.value.copy(amount = event.value)
            is ExpenseEditEvent.CategorySelected -> _state.value = _state.value.copy(selectedCategory = event.category)
            is ExpenseEditEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            ExpenseEditEvent.LoadExpense -> loadExpense()
            ExpenseEditEvent.SaveExpense -> saveExpense()
            ExpenseEditEvent.ClearError -> _state.value = _state.value.copy(error = null)
            ExpenseEditEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getCategories().collect { categories ->
                    _state.value = _state.value.copy(categories = categories)
                }
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    private fun loadExpense() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                expenseRepository.getExpenseById(expenseId).collect { expense ->
                    if (expense != null) {
                        val category = _state.value.categories.find { it.id == expense.categoryId }
                        _state.value = _state.value.copy(
                            id = expense.id,
                            title = expense.title,
                            amount = expense.amount.toString(),
                            selectedCategory = category,
                            description = expense.description,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Expense not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load expense"
                )
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
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            val expense = Expense(
                id = currentState.id,
                title = currentState.title.trim(),
                amount = amountInt,
                categoryId = currentState.selectedCategory.id,  // ✅ Fixed
                description = currentState.description.trim()
            )
            
            val result = expenseRepository.updateExpense(expense)
            
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update expense"
                )
            }
        }
    }
}

class ExpenseEditViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseEditViewModel::class.java)) {
            return ExpenseEditViewModel(expenseRepository, categoryRepository, expenseId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
