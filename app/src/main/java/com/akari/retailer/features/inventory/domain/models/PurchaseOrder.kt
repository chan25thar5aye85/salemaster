package com.akari.retailer.features.inventory.domain.models

enum class PurchaseOrderStatus {
    DRAFT,          // Initial state
    SENT,           // Sent to supplier
    ACKNOWLEDGED,   // Supplier acknowledged
    RECEIVED,       // Partially or fully received
    INVOICED,       // Invoice received
    CLOSED          // Fully completed
}

data class PurchaseOrderItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val costPrice: Int = 0,
    val total: Int = 0,
    val receivedQuantity: Int = 0  // Track how much received
) {
    fun isFullyReceived(): Boolean = receivedQuantity >= quantity
    fun getRemainingQuantity(): Int = quantity - receivedQuantity
    fun getReceivedTotal(): Int = receivedQuantity * costPrice
}

data class PurchaseOrder(
    val id: String = "",
    val orderName: String = "",          // User-friendly name (e.g., "Weekly Stock Purchase")
    val orderNumber: String = "",        // Auto-generated (e.g., "PO-2024-001")
    val supplierId: String = "",
    val supplierName: String = "",
    val items: List<PurchaseOrderItem> = emptyList(),
    val status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    val totalCost: Int = 0,
    val receivedCost: Int = 0,
    val orderDate: Long = System.currentTimeMillis(),
    // Status timeline
    val sentDate: Long = 0,
    val acknowledgedDate: Long = 0,
    val receivedDate: Long = 0,
    val invoicedDate: Long = 0,
    val closedDate: Long = 0,
    // Invoice fields
    val invoiceNumber: String = "",
    val invoiceAmount: Int = 0,
    val paymentDueDate: Long = 0,
    val paidDate: Long = 0,
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun canTransitionTo(newStatus: PurchaseOrderStatus): Boolean {
        return when (newStatus) {
            PurchaseOrderStatus.DRAFT -> status == PurchaseOrderStatus.DRAFT
            PurchaseOrderStatus.SENT -> status == PurchaseOrderStatus.DRAFT
            PurchaseOrderStatus.ACKNOWLEDGED -> status == PurchaseOrderStatus.SENT
            PurchaseOrderStatus.RECEIVED -> status in listOf(PurchaseOrderStatus.SENT, PurchaseOrderStatus.ACKNOWLEDGED, PurchaseOrderStatus.RECEIVED)
            PurchaseOrderStatus.INVOICED -> status == PurchaseOrderStatus.RECEIVED
            PurchaseOrderStatus.CLOSED -> status in listOf(PurchaseOrderStatus.RECEIVED, PurchaseOrderStatus.INVOICED)
        }
    }
    
    fun getStatusDate(status: PurchaseOrderStatus): Long {
        return when (status) {
            PurchaseOrderStatus.DRAFT -> orderDate
            PurchaseOrderStatus.SENT -> sentDate
            PurchaseOrderStatus.ACKNOWLEDGED -> acknowledgedDate
            PurchaseOrderStatus.RECEIVED -> receivedDate
            PurchaseOrderStatus.INVOICED -> invoicedDate
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
