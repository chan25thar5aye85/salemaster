package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.domain.models.Product

class UpdateProductUseCase(
    private val repository: InventoryRepository
) : UseCase<Product, Result<Unit>> {
    
    override suspend fun invoke(params: Product): Result<Unit> {
        return try {
            if (params.name.isBlank()) {
                Result.failure(Exception("Product name is required"))
            } else if (params.sellPrice <= 0) {
                Result.failure(Exception("Sell price must be greater than 0"))
            } else if (params.stockQuantity < 0) {
                Result.failure(Exception("Stock quantity cannot be negative"))
            } else if (params.id.isEmpty()) {
                Result.failure(Exception("Product ID cannot be empty"))
            } else {
                repository.updateProduct(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
