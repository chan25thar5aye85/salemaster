package com.akari.retailer.features.sales.domain.models

import com.akari.retailer.features.money.domain.models.PaymentEntry

data class SaleItem(
    val productId: String = "",
    val quantity: Int = 0,
    val price: Int = 0,
    val total: Int = 0
)

data class Sale(
    val id: String = "",
    val items: List<SaleItem> = emptyList(),
    val total: Int = 0,
    val payments: List<PaymentEntry> = emptyList(),  // ✅ NEW - Multiple payments
    val timestamp: Long = System.currentTimeMillis(),
    val cashierId: String = ""
) {
    val accountId: String
        get() = payments.firstOrNull()?.accountId ?: "default_cash"
}
