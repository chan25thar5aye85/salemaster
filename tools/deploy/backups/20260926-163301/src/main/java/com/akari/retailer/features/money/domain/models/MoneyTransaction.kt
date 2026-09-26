package com.akari.retailer.features.money.domain.models

enum class MoneyTransactionType {
    SALE_IN,
    INCOME_IN,
    EXPENSE_OUT,
    PURCHASE_OUT,
    TRANSFER_OUT,
    TRANSFER_IN,
    EXTERNAL_OUT,
    EXTERNAL_IN,
    FEE_IN,
    FEE_OUT,
    ADJUSTMENT,
    OPENING_BALANCE
}

enum class FeeType {
    NONE,
    FEE_PAID,
    FEE_EARNED
}

data class MoneyTransaction(
    val id: String = "",
    val type: MoneyTransactionType = MoneyTransactionType.ADJUSTMENT,
    val fromAccountId: String = "",
    val toAccountId: String = "",
    val amount: Int = 0,
    val fee: Int = 0,
    val feeType: FeeType = FeeType.NONE,
    val netAmount: Int = 0,
    val description: String = "",
    val referenceId: String = "",
    val referenceType: String = "",
    val externalAccountName: String = "",
    val externalAccountNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getDisplayType(): String {
        return when (type) {
            MoneyTransactionType.SALE_IN -> "💰 Sale"
            MoneyTransactionType.INCOME_IN -> "💰 Income"
            MoneyTransactionType.EXPENSE_OUT -> "💳 Expense"
            MoneyTransactionType.PURCHASE_OUT -> "📦 Purchase"
            MoneyTransactionType.TRANSFER_OUT -> "↗️ Transfer Out"
            MoneyTransactionType.TRANSFER_IN -> "↙️ Transfer In"
            MoneyTransactionType.EXTERNAL_OUT -> "🌐 External Out"
            MoneyTransactionType.EXTERNAL_IN -> "🌐 External In"
            MoneyTransactionType.FEE_IN -> "💵 Fee Earned"
            MoneyTransactionType.FEE_OUT -> "💸 Fee Paid"
            MoneyTransactionType.ADJUSTMENT -> "✏️ Adjustment"
            MoneyTransactionType.OPENING_BALANCE -> "🏁 Opening"
        }
    }
}
