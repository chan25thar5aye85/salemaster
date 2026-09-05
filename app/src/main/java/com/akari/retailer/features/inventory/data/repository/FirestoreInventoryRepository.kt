package com.akari.retailer.features.inventory.data.repository

import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.domain.models.Product
import kotlinx.coroutines.flow.Flow

class FirestoreInventoryRepository(
    private val service: FirestoreInventoryService
) : InventoryRepository {
    
    override suspend fun addProduct(product: Product): Result<String> {
        return service.addProduct(product)
    }
    
    override suspend fun updateProduct(product: Product): Result<Unit> {
        return service.updateProduct(product)
    }
    
    override suspend fun deleteProduct(productId: String): Result<Unit> {
        return service.deleteProduct(productId)
    }
    
    override fun getProducts(): Flow<List<Product>> {
        return service.getProducts()
    }
    
    override fun getProductById(productId: String): Flow<Product?> {
        return service.getProductById(productId)
    }
    
    override suspend fun getProductByIdSync(productId: String): Result<Product> {
        return service.getProductByIdSync(productId)
    }
}
