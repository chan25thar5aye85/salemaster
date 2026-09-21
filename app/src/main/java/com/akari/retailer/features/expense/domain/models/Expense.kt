package com.akari.retailer.features.expense.domain.models

enum class ExpenseType {
    BUSINESS,
    PERSONAL,
    MIXED
}

data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Int = 0,
    val categoryId: String = "default_other",
    val type: ExpenseType = ExpenseType.BUSINESS,
    val businessPercentage: Int = 100,
    val accountId: String = "default_cash",  // ✅ NEW - Money account
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getBusinessAmount(): Int {
        return when (type) {
            ExpenseType.BUSINESS -> amount
            ExpenseType.PERSONAL -> 0
            ExpenseType.MIXED -> (amount * businessPercentage / 100)
        }
    }
    
    fun affectsProfit(): Boolean {
        return type != ExpenseType.PERSONAL
    }
}
