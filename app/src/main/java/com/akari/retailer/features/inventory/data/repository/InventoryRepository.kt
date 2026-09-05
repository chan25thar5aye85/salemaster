package com.akari.retailer.features.inventory.data.repository

import com.akari.retailer.features.inventory.domain.models.Product
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    suspend fun addProduct(product: Product): Result<String>
    suspend fun updateProduct(product: Product): Result<Unit>
    suspend fun deleteProduct(productId: String): Result<Unit>
    fun getProducts(): Flow<List<Product>>
    fun getProductById(productId: String): Flow<Product?>
    suspend fun getProductByIdSync(productId: String): Result<Product>
}
