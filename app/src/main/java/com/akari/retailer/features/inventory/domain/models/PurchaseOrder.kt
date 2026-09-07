package com.akari.retailer.features.inventory.domain.models

enum class PurchaseOrderStatus {
    DRAFT,
    SENT
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
    val status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    
    // Each status has its own items
    val draftItems: List<PurchaseOrderItem> = emptyList(),
    val sentItems: List<PurchaseOrderItem> = emptyList(),
    
    // Each status has its own total
    val draftTotal: Int = 0,
    val sentTotal: Int = 0,
    
    // Timeline
    val orderDate: Long = System.currentTimeMillis(),
    val sentDate: Long = 0,
    
    val notes: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getItemsForStatus(status: PurchaseOrderStatus): List<PurchaseOrderItem> {
        return when (status) {
            PurchaseOrderStatus.DRAFT -> draftItems
            PurchaseOrderStatus.SENT -> sentItems
        }
    }
    
    fun getTotalForStatus(status: PurchaseOrderStatus): Int {
        return when (status) {
            PurchaseOrderStatus.DRAFT -> draftTotal
            PurchaseOrderStatus.SENT -> sentTotal
        }
    }
    
    fun canTransitionTo(newStatus: PurchaseOrderStatus): Boolean {
        return when (newStatus) {
            PurchaseOrderStatus.DRAFT -> status == PurchaseOrderStatus.DRAFT
            PurchaseOrderStatus.SENT -> status == PurchaseOrderStatus.DRAFT
        }
    }
}
