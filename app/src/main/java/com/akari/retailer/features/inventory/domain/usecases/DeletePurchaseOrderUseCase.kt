package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus

class DeletePurchaseOrderUseCase(
    private val repository: PurchaseOrderRepository
) : UseCase<String, Result<Unit>> {
    
    override suspend fun invoke(params: String): Result<Unit> {
        return try {
            if (params.isEmpty()) {
                return Result.failure(Exception("Order ID cannot be empty"))
            }
            
            // Only allow deletion of DRAFT orders
            val order = repository.getOrderSync(params)
            if (order == null) {
                return Result.failure(Exception("Order not found"))
            }
            
            if (order.status != PurchaseOrderStatus.DRAFT) {
                return Result.failure(Exception("Only draft orders can be deleted"))
            }
            
            repository.deleteOrder(params)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
