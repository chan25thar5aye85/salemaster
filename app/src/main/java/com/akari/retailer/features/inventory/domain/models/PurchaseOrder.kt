package com.akari.retailer.features.inventory.domain.models

enum class PurchaseOrderStatus {
    ORDER,      // Creating/editing the order
    RECEIVED,   // Items received, ready for purchase
    COMPLETED   // Purchase created, order is done
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
    val orderName: String = "",
    val orderNumber: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val status: PurchaseOrderStatus = PurchaseOrderStatus.ORDER,
    
    // Each status has its own items
    val orderItems: List<PurchaseOrderItem> = emptyList(),
    val receivedItems: List<PurchaseOrderItem> = emptyList(),
    
    // Each status has its own total
    val orderTotal: Int = 0,
    val receivedTotal: Int = 0,
    
    // Timeline
    val orderDate: Long = System.currentTimeMillis(),
    val receivedDate: Long = 0,
    val completedDate: Long = 0,
    
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getItemsForStatus(status: PurchaseOrderStatus): List<PurchaseOrderItem> {
        return when (status) {
            PurchaseOrderStatus.ORDER -> orderItems
            PurchaseOrderStatus.RECEIVED -> receivedItems
            PurchaseOrderStatus.COMPLETED -> receivedItems // Show received items as final
        }
    }
    
    fun getTotalForStatus(status: PurchaseOrderStatus): Int {
        return when (status) {
            PurchaseOrderStatus.ORDER -> orderTotal
            PurchaseOrderStatus.RECEIVED -> receivedTotal
            PurchaseOrderStatus.COMPLETED -> receivedTotal
        }
    }
    
    fun canTransitionTo(newStatus: PurchaseOrderStatus): Boolean {
        return when (newStatus) {
            PurchaseOrderStatus.ORDER -> status == PurchaseOrderStatus.ORDER
            PurchaseOrderStatus.RECEIVED -> status == PurchaseOrderStatus.ORDER
            PurchaseOrderStatus.COMPLETED -> status == PurchaseOrderStatus.RECEIVED
        }
    }
}
