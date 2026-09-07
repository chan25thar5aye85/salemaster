package com.akari.retailer.features.inventory.domain.models

enum class PurchaseOrderStatus {
    DRAFT,          // Initial state - editing
    SENT,           // Sent to supplier
    RECEIVED,       // Items received into stock
    CLOSED          // Fully completed
}

data class PurchaseOrderItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val costPrice: Int = 0,
    val total: Int = 0,
    val receivedQuantity: Int = 0
) {
    fun isFullyReceived(): Boolean = receivedQuantity >= quantity
    fun getRemainingQuantity(): Int = quantity - receivedQuantity
    fun getReceivedTotal(): Int = receivedQuantity * costPrice
}

data class PurchaseOrder(
    val id: String = "",
    val orderName: String = "",
    val orderNumber: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val items: List<PurchaseOrderItem> = emptyList(),
    val status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    val totalCost: Int = 0,
    val receivedCost: Int = 0,
    val orderDate: Long = System.currentTimeMillis(),
    val sentDate: Long = 0,
    val receivedDate: Long = 0,
    val closedDate: Long = 0,
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun canTransitionTo(newStatus: PurchaseOrderStatus): Boolean {
        return when (newStatus) {
            PurchaseOrderStatus.DRAFT -> status == PurchaseOrderStatus.DRAFT
            PurchaseOrderStatus.SENT -> status == PurchaseOrderStatus.DRAFT
            PurchaseOrderStatus.RECEIVED -> status in listOf(PurchaseOrderStatus.SENT, PurchaseOrderStatus.RECEIVED)
            PurchaseOrderStatus.CLOSED -> status == PurchaseOrderStatus.RECEIVED
        }
    }
    
    fun getStatusDate(status: PurchaseOrderStatus): Long {
        return when (status) {
            PurchaseOrderStatus.DRAFT -> orderDate
            PurchaseOrderStatus.SENT -> sentDate
            PurchaseOrderStatus.RECEIVED -> receivedDate
            PurchaseOrderStatus.CLOSED -> closedDate
        }
    }
    
    fun getReceivedProgress(): Float {
        return if (totalCost > 0) receivedCost.toFloat() / totalCost.toFloat() else 0f
    }
    
    fun isFullyReceived(): Boolean = items.all { it.isFullyReceived() }
    fun getOrderedQuantity(): Int = items.sumOf { it.quantity }
    fun getReceivedQuantity(): Int = items.sumOf { it.receivedQuantity }
    fun getRemainingQuantity(): Int = getOrderedQuantity() - getReceivedQuantity()
}
