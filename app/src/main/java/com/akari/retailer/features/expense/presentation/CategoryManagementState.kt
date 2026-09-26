package com.akari.retailer.features.expense.presentation

import com.akari.retailer.features.expense.domain.models.ExpenseCategory

data class CategoryManagementState(
    val categories: List<ExpenseCategory> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDialog: Boolean = false,
    val editingCategory: ExpenseCategory? = null,
    val dialogName: String = "",
    val isSaving: Boolean = false
)
