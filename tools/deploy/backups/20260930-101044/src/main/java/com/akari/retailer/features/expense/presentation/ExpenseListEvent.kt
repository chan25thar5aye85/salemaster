package com.akari.retailer.features.expense.presentation

import com.akari.retailer.core.ui.components.TimeFilter

sealed class ExpenseListEvent {
    data object LoadExpenses : ExpenseListEvent()
    data object RefreshExpenses : ExpenseListEvent()
    data class DeleteExpense(val expenseId: String) : ExpenseListEvent()
    data object ClearError : ExpenseListEvent()
    data class SearchQueryChanged(val query: String) : ExpenseListEvent()
    data object ClearSearch : ExpenseListEvent()
    data class ToggleCategoryFilter(val categoryId: String) : ExpenseListEvent()
    data object ClearCategoryFilters : ExpenseListEvent()
    data class TimeFilterChanged(val filter: TimeFilter) : ExpenseListEvent()
}
