package com.akari.retailer.features.expense.domain.models

data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Int = 0,
    val categoryId: String = "default_other",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Helper to get category name (for display)
    fun getCategoryName(categories: List<ExpenseCategory>): String {
        return categories.find { it.id == categoryId }?.name ?: "Other"
    }
    
    // Helper to get category icon
    fun getCategoryIcon(categories: List<ExpenseCategory>): String {
        return categories.find { it.id == categoryId }?.icon ?: "📌"
    }
}
