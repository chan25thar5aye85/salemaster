package com.akari.retailer.features.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
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
            is ExpenseListEvent.ToggleCategoryFilter -> toggleCategoryFilter(event.categoryId)
            is ExpenseListEvent.ClearCategoryFilters -> clearCategoryFilters()
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
                    applyFilters()
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

    private fun toggleCategoryFilter(categoryId: String) {
        val currentSelected = _state.value.selectedCategoryIds.toMutableSet()
        if (currentSelected.contains(categoryId)) {
            currentSelected.remove(categoryId)
        } else {
            currentSelected.add(categoryId)
        }
        _state.value = _state.value.copy(selectedCategoryIds = currentSelected.toSet())
        applyFilters()
    }

    private fun clearCategoryFilters() {
        _state.value = _state.value.copy(selectedCategoryIds = emptySet())
        applyFilters()
    }

    private fun searchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
    }

    private fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "")
        applyFilters()
    }

    private fun applyFilters() {
        val query = _state.value.searchQuery.lowercase().trim()
        val allExpenses = _state.value.allExpenses
        val categories = _state.value.categories
        val selectedCategoryIds = _state.value.selectedCategoryIds
        
        var filtered = allExpenses
        
        // Apply category filter
        if (selectedCategoryIds.isNotEmpty()) {
            filtered = filtered.filter { expense ->
                selectedCategoryIds.contains(expense.categoryId)
            }
        }
        
        // Apply search filter
        if (query.isNotEmpty()) {
            filtered = filtered.filter { expense ->
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
