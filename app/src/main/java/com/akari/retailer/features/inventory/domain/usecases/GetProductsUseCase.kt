package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.NoParamUseCase
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.domain.models.Product
import kotlinx.coroutines.flow.Flow

class GetProductsUseCase(
    private val repository: InventoryRepository
) : NoParamUseCase<Flow<List<Product>>> {
    
    override suspend fun invoke(): Flow<List<Product>> {
        return repository.getProducts()
    }
}
