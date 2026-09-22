package com.akari.retailer.features.money.domain.models

data class PaymentEntry(
    val accountId: String = "",
    val amount: Int = 0
)
