package com.akari.retailer.data.repository

import com.akari.retailer.data.remote.FirestoreService
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.sales.domain.models.Sale
import kotlinx.coroutines.flow.Flow

class FirestoreSaleRepository(
    private val firestoreService: FirestoreService
) : SaleRepository {
    
    override suspend fun saveSale(sale: Sale): Result<String> {
        return firestoreService.saveSale(sale)
    }
    
    override fun getSales(): Flow<List<Sale>> {
        return firestoreService.getSales()
    }
    
    override fun getTodaySales(): Flow<List<Sale>> {
        return firestoreService.getTodaySales()
    }
    
    override suspend fun deleteSale(saleId: String): Result<Unit> {
        return firestoreService.deleteSale(saleId)
    }
}
