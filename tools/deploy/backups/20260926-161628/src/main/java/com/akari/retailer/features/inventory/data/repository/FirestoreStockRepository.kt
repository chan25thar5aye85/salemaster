package com.akari.retailer.features.inventory.data.repository

import com.akari.retailer.features.inventory.data.remote.FirestoreStockService
import com.akari.retailer.features.inventory.domain.models.StockMovement
import kotlinx.coroutines.flow.Flow

class FirestoreStockRepository(
    private val service: FirestoreStockService
) : StockRepository {
    
    override suspend fun addMovement(movement: StockMovement): Result<String> {
        return service.addMovement(movement)
    }
    
    override fun getMovementsForProduct(productId: String): Flow<List<StockMovement>> {
        return service.getMovementsForProduct(productId)
    }
    
    override fun getAllMovements(): Flow<List<StockMovement>> {
        return service.getAllMovements()
    }
    
    override suspend fun getMovementsForProductSync(productId: String): Result<List<StockMovement>> {
        return service.getMovementsForProductSync(productId)
    }
    
    override suspend fun deleteMovement(movementId: String): Result<Unit> {
        return service.deleteMovement(movementId)
    }
}
