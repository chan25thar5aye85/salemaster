package com.akari.retailer.features.expense.presentation

import com.akari.retailer.features.expense.domain.models.ExpenseCategory

sealed class CategoryManagementEvent {
    data object LoadCategories : CategoryManagementEvent()
    data object RefreshCategories : CategoryManagementEvent()
    data class DeleteCategory(val categoryId: String) : CategoryManagementEvent()
    data object ClearError : CategoryManagementEvent()
    data class SelectTab(val tabIndex: Int) : CategoryManagementEvent()
    
    // Dialog events
    data object ShowAddDialog : CategoryManagementEvent()
    data class ShowEditDialog(val category: ExpenseCategory) : CategoryManagementEvent()
    data object DismissDialog : CategoryManagementEvent()
    data class DialogNameChanged(val name: String) : CategoryManagementEvent()
    data object SaveCategory : CategoryManagementEvent()
}
