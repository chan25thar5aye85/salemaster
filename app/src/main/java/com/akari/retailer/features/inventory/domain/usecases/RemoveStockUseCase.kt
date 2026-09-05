package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.inventory.domain.models.MovementType
import com.akari.retailer.features.inventory.domain.models.StockMovement

class RemoveStockUseCase(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository
) : UseCase<RemoveStockParams, Result<Unit>> {
    
    override suspend fun invoke(params: RemoveStockParams): Result<Unit> {
        return try {
            // Get current product
            val productResult = inventoryRepository.getProductByIdSync(params.productId)
            if (productResult.isFailure) {
                return Result.failure(productResult.exceptionOrNull() ?: Exception("Product not found"))
            }
            
            val product = productResult.getOrNull() ?: return Result.failure(Exception("Product not found"))
            
            // Check stock (warn but allow if insufficient)
            val newStock = product.stockQuantity - params.quantity
            if (newStock < 0 && !params.allowNegative) {
                return Result.failure(Exception("Insufficient stock: available ${product.stockQuantity}, required ${params.quantity}"))
            }
            
            val finalStock = if (newStock < 0) 0 else newStock
            val updatedProduct = product.copy(
                stockQuantity = finalStock,
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
                type = if (params.isCancellation) MovementType.CANCEL else MovementType.SALE,
                quantity = -params.quantity,
                previousStock = product.stockQuantity,
                newStock = finalStock,
                reason = if (params.isCancellation) "Sale cancelled" else params.reason,
                saleId = params.saleId,
                userId = params.userId
            )
            
            stockRepository.addMovement(movement)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class RemoveStockParams(
    val productId: String,
    val quantity: Int,
    val reason: String = "",
    val saleId: String = "",
    val userId: String = "",
    val allowNegative: Boolean = true,
    val isCancellation: Boolean = false
)
