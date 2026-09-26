package com.akari.retailer.features.supplier.domain.models

/**
 * A single line in a supplier's payable history.
 *
 * Sign convention:
 *   - [PURCHASE_ON_CREDIT]: amount is positive (you owe supplier more)
 *   - [PAYMENT]: amount is negative (you paid them, owe less)
 */
data class SupplierTransaction(
    val id: String = "",
    val supplierId: String = "",
    val type: SupplierTransactionType = SupplierTransactionType.PURCHASE_ON_CREDIT,
    val amount: Int = 0,
    val purchaseId: String = "",
    val paymentAccountId: String = "",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class SupplierTransactionType {
    /** You bought goods on credit — you owe more */
    PURCHASE_ON_CREDIT,

    /** You paid the supplier — you owe less */
    PAYMENT,

    /** Supplier refunded you — they owe you (amount is positive) */
    REFUND_RECEIVED
}
