package com.akari.retailer.data.repository

import com.akari.retailer.features.sales.domain.models.Sale
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    suspend fun saveSale(sale: Sale): Result<String>
    fun getSales(): Flow<List<Sale>>
    fun getTodaySales(): Flow<List<Sale>>
    suspend fun deleteSale(saleId: String): Result<Unit>
}
