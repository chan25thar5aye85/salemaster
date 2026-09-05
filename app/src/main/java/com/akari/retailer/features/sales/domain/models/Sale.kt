package com.akari.retailer.features.sales.domain.models

enum class PaymentMethod {
    CASH,
    KPAY,
    WAVEPAY
}

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
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierId: String = ""
)
