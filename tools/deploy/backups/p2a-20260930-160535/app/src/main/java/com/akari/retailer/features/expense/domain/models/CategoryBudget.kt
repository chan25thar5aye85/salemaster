package com.akari.retailer.features.expense.domain.models

data class CategoryBudget(
    val id: String = "",
    val categoryId: String = "",
    val month: Int = 0,
    val year: Int = 0,
    val budgetAmount: Int = 0,
    val spentAmount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val remaining: Int get() = budgetAmount - spentAmount
    val isOverBudget: Boolean get() = spentAmount > budgetAmount
    val utilizationPercentage: Double get() = if (budgetAmount > 0) (spentAmount.toDouble() / budgetAmount) * 100 else 0.0
    
    fun getProgress(): Float {
        return if (budgetAmount > 0) {
            (spentAmount.toFloat() / budgetAmount).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
}
