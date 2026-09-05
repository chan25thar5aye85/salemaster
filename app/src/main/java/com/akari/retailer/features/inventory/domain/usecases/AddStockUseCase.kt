package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.inventory.domain.models.MovementType
import com.akari.retailer.features.inventory.domain.models.StockMovement

class AddStockUseCase(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository
) : UseCase<AddStockParams, Result<Unit>> {
    
    override suspend fun invoke(params: AddStockParams): Result<Unit> {
        return try {
            // Get current product
            val productResult = inventoryRepository.getProductByIdSync(params.productId)
            if (productResult.isFailure) {
                return Result.failure(productResult.exceptionOrNull() ?: Exception("Product not found"))
            }
            
            val product = productResult.getOrNull() ?: return Result.failure(Exception("Product not found"))
            
            // Calculate new stock
            val newStock = product.stockQuantity + params.quantity
            val updatedProduct = product.copy(
                stockQuantity = newStock,
                updatedAt = System.currentTimeMillis()
            )
            
            // Update product
            val updateResult = inventoryRepository.updateProduct(updatedProduct)
            if (updateResult.isFailure) {
                return Result.failure(updateResult.exceptionOrNull() ?: Exception("Failed to update product"))
            }
            
            // Record movement
            val movement = StockMovement(
                productId = params.productId,
                type = MovementType.PURCHASE,
                quantity = params.quantity,
                previousStock = product.stockQuantity,
                newStock = newStock,
                reason = params.reason,
                purchaseOrderId = params.purchaseOrderId,
                userId = params.userId
            )
            
            stockRepository.addMovement(movement)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class AddStockParams(
    val productId: String,
    val quantity: Int,
    val reason: String = "",
    val purchaseOrderId: String = "",
    val userId: String = ""
)
