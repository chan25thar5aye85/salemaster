package com.akari.retailer.features.expense.presentation

import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.expense.domain.models.ExpenseCategory

data class ExpenseListState(
    val expenses: List<Expense> = emptyList(),
    val allExpenses: List<Expense> = emptyList(),
    val categories: List<ExpenseCategory> = emptyList(),
    val selectedCategoryIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val totalExpenses: Int = 0
)
