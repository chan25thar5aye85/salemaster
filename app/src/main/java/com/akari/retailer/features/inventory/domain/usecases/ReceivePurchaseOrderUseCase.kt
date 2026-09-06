package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.inventory.domain.models.MovementType
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.inventory.domain.models.StockMovement

data class ReceivePurchaseOrderParams(
    val orderId: String,
    val receivedItems: List<PurchaseOrderItem>  // Items with actual received quantities
)

class ReceivePurchaseOrderUseCase(
    private val poRepository: PurchaseOrderRepository,
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository
) : UseCase<ReceivePurchaseOrderParams, Result<Unit>> {
    
    override suspend fun invoke(params: ReceivePurchaseOrderParams): Result<Unit> {
        return try {
            // Get current order
            val order = poRepository.getOrderSync(params.orderId)
            if (order == null) {
                return Result.failure(Exception("Order not found"))
            }
            
            // Validate status
            if (order.status !in listOf(PurchaseOrderStatus.SENT, PurchaseOrderStatus.ACKNOWLEDGED, PurchaseOrderStatus.RECEIVED)) {
                return Result.failure(Exception("Order must be Sent or Acknowledged to receive"))
            }
            
            // Validate received items
            params.receivedItems.forEach { received ->
                val orderedItem = order.items.find { it.productId == received.productId }
                if (orderedItem == null) {
                    return Result.failure(Exception("Product ${received.productName} not in order"))
                }
                if (received.quantity <= 0) {
                    return Result.failure(Exception("Received quantity must be > 0"))
                }
                val remaining = orderedItem.quantity - orderedItem.receivedQuantity
                if (received.quantity > remaining) {
                    return Result.failure(Exception("Cannot receive more than remaining: $remaining"))
                }
            }
            
            // Update items with received quantities
            val updatedItems = order.items.map { orderedItem ->
                val received = params.receivedItems.find { it.productId == orderedItem.productId }
                if (received != null) {
                    orderedItem.copy(
                        receivedQuantity = orderedItem.receivedQuantity + received.quantity
                    )
                } else {
                    orderedItem
                }
            }
            
            // Update stock for each received item
            params.receivedItems.forEach { received ->
                val productResult = inventoryRepository.getProductByIdSync(received.productId)
                if (productResult.isFailure) {
                    return Result.failure(Exception("Product not found: ${received.productId}"))
                }
                
                val product = productResult.getOrNull() ?: return@forEach
                val newStock = product.stockQuantity + received.quantity
                
                val updatedProduct = product.copy(
                    stockQuantity = newStock,
                    updatedAt = System.currentTimeMillis()
                )
                inventoryRepository.updateProduct(updatedProduct)
                
                // Record stock movement
                val movement = StockMovement(
                    productId = received.productId,
                    type = MovementType.PURCHASE,
                    quantity = received.quantity,
                    previousStock = product.stockQuantity,
                    newStock = newStock,
                    reason = "Purchase Order: ${order.orderNumber}",
                    purchaseOrderId = order.id,
                    createdAt = System.currentTimeMillis()
                )
                stockRepository.addMovement(movement)
            }
            
            // Calculate totals
            val newReceivedCost = updatedItems.sumOf { it.getReceivedTotal() }
            val isFullyReceived = updatedItems.all { it.isFullyReceived() }
            
            // Determine new status
            val newStatus = if (isFullyReceived) {
                PurchaseOrderStatus.RECEIVED
            } else {
                PurchaseOrderStatus.RECEIVED  // Partial receive, keep as RECEIVED
            }
            
            // Update order
            val updatedOrder = order.copy(
                items = updatedItems,
                receivedCost = newReceivedCost,
                status = newStatus,
                receivedDate = if (isFullyReceived) System.currentTimeMillis() else order.receivedDate,
                updatedAt = System.currentTimeMillis()
            )
            
            poRepository.updateOrder(updatedOrder)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
