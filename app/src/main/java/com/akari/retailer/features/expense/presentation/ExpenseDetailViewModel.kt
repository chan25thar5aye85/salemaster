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

data class ExpenseDetailState(
    val expense: Expense? = null,
    val categories: List<ExpenseCategory> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class ExpenseDetailEvent {
    data object LoadExpense : ExpenseDetailEvent()
    data object ClearError : ExpenseDetailEvent()
}

class ExpenseDetailViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseId: String
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseDetailState())
    val state: StateFlow<ExpenseDetailState> = _state.asStateFlow()

    init {
        loadCategories()
        loadExpense()
    }

    fun handleEvent(event: ExpenseDetailEvent) {
        when (event) {
            is ExpenseDetailEvent.LoadExpense -> loadExpense()
            is ExpenseDetailEvent.ClearError -> clearError()
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
                    _state.value = _state.value.copy(
                        expense = expense,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load expense"
                )
            }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

class ExpenseDetailViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseDetailViewModel::class.java)) {
            return ExpenseDetailViewModel(expenseRepository, categoryRepository, expenseId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
