package com.akari.retailer.features.sales.domain.models

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
    val accountId: String = "default_cash",
    val timestamp: Long = System.currentTimeMillis(),
    val cashierId: String = ""
)
