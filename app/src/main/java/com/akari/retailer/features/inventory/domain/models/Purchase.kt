package com.akari.retailer.features.inventory.domain.models

data class PurchaseItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val costPrice: Int = 0,
    val total: Int = 0
)

data class Purchase(
    val id: String = "",
    val orderId: String = "",
    val orderName: String = "",
    val orderNumber: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val items: List<PurchaseItem> = emptyList(),
    val totalCost: Int = 0,
    val purchaseDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val receiptNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
