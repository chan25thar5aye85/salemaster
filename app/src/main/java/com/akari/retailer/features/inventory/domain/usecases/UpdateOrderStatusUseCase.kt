package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus

data class UpdateOrderStatusParams(
    val orderId: String,
    val newStatus: PurchaseOrderStatus
)

class UpdateOrderStatusUseCase(
    private val repository: PurchaseOrderRepository
) : UseCase<UpdateOrderStatusParams, Result<Unit>> {
    
    override suspend fun invoke(params: UpdateOrderStatusParams): Result<Unit> {
        return try {
            val order = repository.getOrderSync(params.orderId)
            if (order == null) {
                return Result.failure(Exception("Order not found"))
            }
            
            // Validate status transition
            if (!order.canTransitionTo(params.newStatus)) {
                return Result.failure(
                    Exception("Cannot transition from ${order.status} to ${params.newStatus}")
                )
            }
            
            // Update with timestamp
            val updatedOrder = when (params.newStatus) {
                PurchaseOrderStatus.SENT -> order.copy(
                    status = params.newStatus,
                    sentDate = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                PurchaseOrderStatus.ACKNOWLEDGED -> order.copy(
                    status = params.newStatus,
                    acknowledgedDate = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                PurchaseOrderStatus.RECEIVED -> order.copy(
                    status = params.newStatus,
                    receivedDate = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                PurchaseOrderStatus.INVOICED -> order.copy(
                    status = params.newStatus,
                    invoicedDate = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                PurchaseOrderStatus.CLOSED -> order.copy(
                    status = params.newStatus,
                    closedDate = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                else -> order.copy(
                    status = params.newStatus,
                    updatedAt = System.currentTimeMillis()
                )
            }
            
            repository.updateOrder(updatedOrder)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
