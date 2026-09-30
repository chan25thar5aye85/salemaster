package com.akari.retailer.features.expense.domain.models

import com.akari.retailer.features.money.domain.models.PaymentEntry

enum class ExpenseType {
    BUSINESS,
    PERSONAL,
    MIXED
}

data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Int = 0,
    val categoryId: String = "",
    val type: ExpenseType = ExpenseType.BUSINESS,
    val businessPercentage: Int = 100,
    val payments: List<PaymentEntry> = emptyList(),  // ✅ NEW - Multiple payments
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // ✅ Backward compatibility - get first account for old code
    val accountId: String
        get() = payments.firstOrNull()?.accountId ?: "default_cash"
    
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
    
    // ✅ Get account names for display
    fun getAccountSummary(accounts: List<com.akari.retailer.features.money.domain.models.MoneyAccount>): String {
        return payments.joinToString(", ") { payment ->
            val account = accounts.find { it.id == payment.accountId }
            "${account?.name ?: "?"}: ${payment.amount}"
        }
    }
}
