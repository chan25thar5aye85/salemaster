package com.akari.retailer.features.customer.domain.models

/**
 * A single line in a customer's credit history.
 *
 * Positive [amount] means credit was extended (customer owes more).
 * Negative [amount] means a payment was received (customer owes less).
 */
data class CreditTransaction(
    val id: String = "",
    val customerId: String = "",
    val type: CreditTransactionType = CreditTransactionType.SALE_ON_CREDIT,
    val amount: Int = 0,
    val saleId: String = "",
    val paymentAccountId: String = "",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class CreditTransactionType {
    SALE_ON_CREDIT,
    PAYMENT
}
