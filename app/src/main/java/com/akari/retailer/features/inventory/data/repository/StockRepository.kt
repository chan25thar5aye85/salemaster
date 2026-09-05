package com.akari.retailer.features.inventory.data.repository

import com.akari.retailer.features.inventory.domain.models.StockMovement
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    suspend fun addMovement(movement: StockMovement): Result<String>
    fun getMovementsForProduct(productId: String): Flow<List<StockMovement>>
    fun getAllMovements(): Flow<List<StockMovement>>
    suspend fun getMovementsForProductSync(productId: String): Result<List<StockMovement>>
}
