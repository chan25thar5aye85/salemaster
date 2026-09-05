package com.akari.retailer.features.inventory.domain.models

data class PurchaseOrder(
    val id: String = "",
    val supplierId: String = "",
    val productId: String = "",
    val quantity: Int = 0,
    val costPrice: Int = 0,
    val totalCost: Int = 0,
    val status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    val orderedAt: Long = System.currentTimeMillis(),
    val receivedAt: Long = 0,
    val notes: String = ""
)

enum class PurchaseOrderStatus {
    DRAFT,
    COMPLETED,
    CANCELLED
}
