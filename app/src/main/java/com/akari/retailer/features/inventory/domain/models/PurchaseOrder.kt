package com.akari.retailer.features.inventory.domain.models

enum class PurchaseOrderStatus {
    ORDER,
    RECEIVED,
    COMPLETED
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
    
    val orderItems: List<PurchaseOrderItem> = emptyList(),
    val receivedItems: List<PurchaseOrderItem> = emptyList(),
    
    val orderTotal: Int = 0,
    val receivedTotal: Int = 0,
    
    // Dates
    val orderDate: Long = System.currentTimeMillis(),
    val expectedDeliveryDate: Long = 0,  // NEW: User sets this
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
            PurchaseOrderStatus.COMPLETED -> receivedItems
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
