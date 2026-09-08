package com.akari.retailer.features.expense.presentation

import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import com.akari.retailer.features.expense.domain.models.ExpenseType

enum class AnalyticsTimeRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM
}

enum class ExpenseTypeFilter {
    ALL,        // Show all expenses
    BUSINESS,   // Only business expenses
    PERSONAL,   // Only personal expenses
    MIXED       // Only mixed expenses
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
    val selectedMonth: Int = 0,
    val selectedYear: Int = 0,
    val timeRange: AnalyticsTimeRange = AnalyticsTimeRange.TODAY,
    val typeFilter: ExpenseTypeFilter = ExpenseTypeFilter.ALL,  // ✅ NEW
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val rangeLabel: String = "Today"
)
