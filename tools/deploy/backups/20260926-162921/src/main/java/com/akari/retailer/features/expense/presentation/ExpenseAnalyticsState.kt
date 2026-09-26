package com.akari.retailer.features.expense.presentation

import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.features.expense.domain.models.ExpenseCategory

enum class ExpenseTypeFilter {
    ALL,
    BUSINESS,
    PERSONAL,
    MIXED
}

data class CategorySpending(
    val category: ExpenseCategory,
    val totalSpent: Int,
    val count: Int,
    val percentage: Double
)

data class ExpenseAnalyticsState(
    val totalExpenses: Int = 0,
    val categorySpending: List<CategorySpending> = emptyList(),
    val topCategory: CategorySpending? = null,
    val monthlyAverage: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val timeFilter: TimeFilter = TimeFilter(),
    val typeFilter: ExpenseTypeFilter = ExpenseTypeFilter.ALL
)
