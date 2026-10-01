package com.akari.retailer.features.expense.presentation

import com.akari.retailer.features.expense.domain.models.ExpenseType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.data.remote.FirestoreExpenseFinalizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseListViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseFinalizer: FirestoreExpenseFinalizer
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseListState())
    val state: StateFlow<ExpenseListState> = _state.asStateFlow()

    private var loadExpensesJob: Job? = null
    private var loadCategoriesJob: Job? = null

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
            is ExpenseListEvent.TimeFilterChanged -> setTimeFilter(event.filter)
        }
    }

    private fun loadCategories() {
        loadCategoriesJob?.cancel()
        loadCategoriesJob = viewModelScope.launch {
            try {
                categoryRepository.getCategories().collect { categories ->
                    _state.value = _state.value.copy(categories = categories)
                }
            } catch (e: Exception) { }
        }
    }

    private fun loadExpenses() {
        loadExpensesJob?.cancel()
        loadExpensesJob = viewModelScope.launch {
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

    private fun refreshExpenses() = loadExpenses()

    private fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = expenseFinalizer.deleteExpense(expenseId)
                if (result.isSuccess) loadExpenses()
                else _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to delete expense"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete expense"
                )
            }
        }
    }

    private fun toggleCategoryFilter(categoryId: String) {
        val current = _state.value.selectedCategoryIds.toMutableSet()
        if (current.contains(categoryId)) current.remove(categoryId) else current.add(categoryId)
        _state.value = _state.value.copy(selectedCategoryIds = current.toSet())
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

    private fun setTimeFilter(filter: TimeFilter) {
        _state.value = _state.value.copy(timeFilter = filter)
        applyFilters()
    }

    private fun applyFilters() {
        val query = _state.value.searchQuery.lowercase().trim()
        val allExpenses = _state.value.allExpenses
        val categories = _state.value.categories
        val selectedCategoryIds = _state.value.selectedCategoryIds
        val range = _state.value.timeFilter.resolveRange()

        var filtered = allExpenses.filter { it.date in range.first..range.last }
        if (selectedCategoryIds.isNotEmpty()) {
            filtered = filtered.filter { selectedCategoryIds.contains(it.categoryId) }
        }
        if (query.isNotEmpty()) {
            filtered = filtered.filter { expense ->
                expense.title.lowercase().contains(query) ||
                categories.find { it.id == expense.categoryId }?.name?.lowercase()?.contains(query) == true
            }
        }

        // ── Summary card computations ──
        var businessTotal = 0
        var personalTotal = 0
        var mixedTotal = 0
        val categoryTotals = mutableMapOf<String, Int>()

        filtered.forEach { exp ->
            when (exp.type) {
                com.akari.retailer.features.expense.domain.models.ExpenseType.BUSINESS ->
                    businessTotal += exp.amount
                com.akari.retailer.features.expense.domain.models.ExpenseType.PERSONAL ->
                    personalTotal += exp.amount
                com.akari.retailer.features.expense.domain.models.ExpenseType.MIXED ->
                    mixedTotal += exp.amount
            }
            if (exp.categoryId.isNotBlank()) {
                categoryTotals[exp.categoryId] =
                    (categoryTotals[exp.categoryId] ?: 0) + exp.amount
            }
        }

        // Top category = highest total (skip the "uncategorized" fallback if empty)
        val topEntry = categoryTotals.entries.maxByOrNull { it.value }
        val topCategoryName = topEntry?.let { (id, _) ->
            val cat = categories.find { it.id == id }
            if (cat != null) "${cat.icon} ${cat.name}" else ""
        } ?: ""
        val topCategoryAmount = topEntry?.value ?: 0

        _state.value = _state.value.copy(
            expenses = filtered,
            totalExpenses = filtered.sumOf { it.amount },
            businessTotal = businessTotal,
            personalTotal = personalTotal,
            mixedTotal = mixedTotal,
            topCategoryName = topCategoryName,
            topCategoryAmount = topCategoryAmount
        )
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

class ExpenseListViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseFinalizer: FirestoreExpenseFinalizer
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseListViewModel::class.java)) {
            return ExpenseListViewModel(expenseRepository, categoryRepository, expenseFinalizer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
