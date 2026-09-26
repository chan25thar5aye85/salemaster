package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.inventory.data.repository.InventoryRepository

class DeleteProductUseCase(
    private val repository: InventoryRepository
) : UseCase<String, Result<Unit>> {
    
    override suspend fun invoke(params: String): Result<Unit> {
        return try {
            if (params.isEmpty()) {
                Result.failure(Exception("Product ID cannot be empty"))
            } else {
                repository.deleteProduct(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
