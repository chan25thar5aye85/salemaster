package com.akari.retailer.features.expense.domain.models

enum class ExpenseType {
    BUSINESS,   // Business expense - affects profit
    PERSONAL,   // Personal expense - doesn't affect profit
    MIXED       // Partially business - user can decide percentage
}

data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Int = 0,
    val categoryId: String = "default_other",
    val type: ExpenseType = ExpenseType.BUSINESS,  // ✅ NEW
    val businessPercentage: Int = 100,  // ✅ NEW: For MIXED expenses (0-100)
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Calculate business portion of the expense
    fun getBusinessAmount(): Int {
        return when (type) {
            ExpenseType.BUSINESS -> amount
            ExpenseType.PERSONAL -> 0
            ExpenseType.MIXED -> (amount * businessPercentage / 100)
        }
    }
    
    // Check if this expense affects profit
    fun affectsProfit(): Boolean {
        return type != ExpenseType.PERSONAL
    }
}
