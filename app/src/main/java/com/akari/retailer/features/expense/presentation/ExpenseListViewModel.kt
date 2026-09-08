package com.akari.retailer.features.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseListViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseListState())
    val state: StateFlow<ExpenseListState> = _state.asStateFlow()

    init {
        loadExpenses()
        loadCategories()
    }

    fun handleEvent(event: ExpenseListEvent) {
        when (event) {
            is ExpenseListEvent.LoadExpenses -> loadExpenses()
            is ExpenseListEvent.RefreshExpenses -> refreshExpenses()
            is ExpenseListEvent.DeleteExpense -> deleteExpense(event.expenseId)
            is ExpenseListEvent.ClearError -> clearError()
            is ExpenseListEvent.SearchQueryChanged -> searchQueryChanged(event.query)
            is ExpenseListEvent.ClearSearch -> clearSearch()
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

    private fun loadExpenses() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                expenseRepository.getExpenses().collect { expenses ->
                    _state.value = _state.value.copy(
                        allExpenses = expenses,
                        isLoading = false,
                        error = null,
                        totalExpenses = expenses.sumOf { it.amount }
                    )
                    applySearch()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load expenses"
                )
            }
        }
    }

    private fun refreshExpenses() {
        loadExpenses()
    }

    private fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = expenseRepository.deleteExpense(expenseId)
                if (result.isSuccess) {
                    loadExpenses()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete expense"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete expense"
                )
            }
        }
    }

    private fun searchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applySearch()
    }

    private fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "")
        applySearch()
    }

    private fun applySearch() {
        val query = _state.value.searchQuery.lowercase().trim()
        val allExpenses = _state.value.allExpenses
        val categories = _state.value.categories
        
        val filtered = if (query.isEmpty()) {
            allExpenses
        } else {
            allExpenses.filter { expense ->
                expense.title.lowercase().contains(query) ||
                categories.find { it.id == expense.categoryId }?.name?.lowercase()?.contains(query) == true
            }
        }
        
        _state.value = _state.value.copy(expenses = filtered)
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

class ExpenseListViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseListViewModel::class.java)) {
            return ExpenseListViewModel(expenseRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
