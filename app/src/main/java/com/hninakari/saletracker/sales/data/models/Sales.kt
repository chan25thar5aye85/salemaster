package com.hninakari.saletracker.sales.data.models

import java.time.LocalDateTime
import java.util.UUID

data class Sales(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val saleDate: LocalDateTime = LocalDateTime.now()
)

enum class PaymentMethod {
    CASH,
    KPAY,
    WAVE_PAY
}
