package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.inventory.domain.models.MovementType
import com.akari.retailer.features.inventory.domain.models.StockMovement

class AdjustStockUseCase(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository
) : UseCase<AdjustStockParams, Result<Unit>> {
    
    override suspend fun invoke(params: AdjustStockParams): Result<Unit> {
        return try {
            // Get current product
            val productResult = inventoryRepository.getProductByIdSync(params.productId)
            if (productResult.isFailure) {
                return Result.failure(productResult.exceptionOrNull() ?: Exception("Product not found"))
            }
            
            val product = productResult.getOrNull() ?: return Result.failure(Exception("Product not found"))
            
            val difference = params.newStock - product.stockQuantity
            if (difference == 0) {
                return Result.success(Unit)
            }
            
            val updatedProduct = product.copy(
                stockQuantity = params.newStock,
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
                type = MovementType.ADJUSTMENT,
                quantity = difference,
                previousStock = product.stockQuantity,
                newStock = params.newStock,
                reason = params.reason,
                userId = params.userId
            )
            
            stockRepository.addMovement(movement)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class AdjustStockParams(
    val productId: String,
    val newStock: Int,
    val reason: String = "",
    val userId: String = ""
)
