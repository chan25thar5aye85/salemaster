package com.akari.retailer.features.inventory.domain.models

enum class PurchaseOrderStatus {
    DRAFT,
    SENT,
    ACKNOWLEDGED,
    RECEIVED,
    INVOICED,
    CLOSED
}

data class PurchaseOrderItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val costPrice: Int = 0,
    val total: Int = 0
)

data class PurchaseOrder(
    val id: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val items: List<PurchaseOrderItem> = emptyList(),
    val receivedItems: List<PurchaseOrderItem> = emptyList(),
    val totalCost: Int = 0,
    val status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    val orderedAt: Long = System.currentTimeMillis(),
    val sentAt: Long = 0,
    val acknowledgedAt: Long = 0,
    val receivedAt: Long = 0,
    val invoicedAt: Long = 0,
    val closedAt: Long = 0,
    val invoiceNumber: String = "",
    val invoiceAmount: Int = 0,
    val paidAt: Long = 0,
    val paymentDueDate: Long = 0,
    val notes: String = ""
)
