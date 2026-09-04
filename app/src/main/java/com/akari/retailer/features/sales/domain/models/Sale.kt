package com.akari.retailer.features.sales.domain.models

enum class PaymentMethod {
    CASH,
    KPAY,
    WAVEPAY
}

data class Sale(
    val id: String = "",
    val items: List<Int> = emptyList(),
    val total: Int = 0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierId: String = ""
)
