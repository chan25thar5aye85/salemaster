package com.akari.retailer.features.inventory.domain.models

enum class MovementType {
    SALE,
    PURCHASE,
    ADJUSTMENT,
    CANCEL
}

data class StockMovement(
    val id: String = "",
    val productId: String = "",
    val type: MovementType = MovementType.ADJUSTMENT,
    val quantity: Int = 0,
    val previousStock: Int = 0,
    val newStock: Int = 0,
    val reason: String = "",
    val saleId: String = "",
    val purchaseOrderId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = ""
)
