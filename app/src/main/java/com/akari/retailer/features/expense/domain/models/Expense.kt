package com.akari.retailer.features.expense.domain.models

enum class ExpenseCategory {
    UTILITIES,
    RENT,
    SALARY,
    INVENTORY,
    MARKETING,
    TRAVEL,
    MEALS,
    SUPPLIES,
    MAINTENANCE,
    OTHER
}

data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Int = 0,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
