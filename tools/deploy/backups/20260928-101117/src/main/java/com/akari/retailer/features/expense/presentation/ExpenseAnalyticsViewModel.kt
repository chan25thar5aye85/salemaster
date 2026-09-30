package com.akari.retailer.features.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import com.akari.retailer.features.expense.domain.models.ExpenseType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAnalyticsViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseAnalyticsState())
    val state: StateFlow<ExpenseAnalyticsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                combine(
                    expenseRepository.getExpenses(),
                    categoryRepository.getCategories()
                ) { expenses, categories ->
                    calculateAnalytics(expenses, categories)
                }.collect { analytics ->
                    _state.value = _state.value.copy(
                        totalExpenses = analytics.totalExpenses,
                        categorySpending = analytics.categorySpending,
                        topCategory = analytics.topCategory,
                        monthlyAverage = analytics.monthlyAverage,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load analytics"
                )
            }
        }
    }

    private fun calculateAnalytics(
        expenses: List<Expense>,
        categories: List<ExpenseCategory>
    ): ExpenseAnalyticsState {
        val typeFilter = _state.value.typeFilter
        val range = _state.value.timeFilter.resolveRange()
        val startDate = range.first
        val endDate = range.last

        val filteredExpenses = expenses.filter { expense ->
            val dateMatch = expense.date in startDate..endDate
            val typeMatch = when (typeFilter) {
                ExpenseTypeFilter.ALL -> true
                ExpenseTypeFilter.BUSINESS -> expense.type == ExpenseType.BUSINESS
                ExpenseTypeFilter.PERSONAL -> expense.type == ExpenseType.PERSONAL
                ExpenseTypeFilter.MIXED -> expense.type == ExpenseType.MIXED
            }
            dateMatch && typeMatch
        }

        val total = filteredExpenses.sumOf { it.amount }
        val categoryMap = mutableMapOf<ExpenseCategory, Pair<Int, Int>>()
        filteredExpenses.forEach { expense ->
            val category = categories.find { it.id == expense.categoryId }
                ?: return@forEach
            val current = categoryMap[category] ?: (0 to 0)
            categoryMap[category] = (current.first + expense.amount) to (current.second + 1)
        }
        val categorySpending = categoryMap.map { (category, data) ->
            CategorySpending(
                category = category,
                totalSpent = data.first,
                count = data.second,
                percentage = if (total > 0) (data.first.toDouble() / total) * 100 else 0.0
            )
        }.sortedByDescending { it.totalSpent }

        return ExpenseAnalyticsState(
            totalExpenses = total,
            categorySpending = categorySpending,
            topCategory = categorySpending.firstOrNull(),
            monthlyAverage = if (filteredExpenses.isNotEmpty()) total / filteredExpenses.size else 0,
            timeFilter = _state.value.timeFilter,
            typeFilter = typeFilter
        )
    }








    fun setTimeFilter(filter: TimeFilter) {
        _state.value = _state.value.copy(timeFilter = filter)
        loadAnalytics()
    }

    fun setTypeFilter(filter: ExpenseTypeFilter) {
        _state.value = _state.value.copy(typeFilter = filter)
        loadAnalytics()
    }
}

class ExpenseAnalyticsViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseAnalyticsViewModel::class.java)) {
            return ExpenseAnalyticsViewModel(expenseRepository, categoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
