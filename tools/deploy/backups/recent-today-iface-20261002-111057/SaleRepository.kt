package com.akari.retailer.features.sales.data.repository

import com.akari.retailer.features.sales.domain.models.Sale
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    suspend fun saveSale(sale: Sale): Result<String>
    fun getSales(): Flow<List<Sale>>
    /** Only the N most recent sales — for the Recent Sales card. */
    fun getRecentSales(limit: Int = 10): Flow<List<Sale>>
    /** Larger history window for the Sale History screen. */
    fun getSalesHistory(limit: Int = 500): Flow<List<Sale>>

    /** Most recent N sales from today only (resets at midnight). */
    fun getRecentTodaySales(limit: Int = 5): Flow<List<Sale>>
    fun getTodaySales(): Flow<List<Sale>>
    fun getSaleById(saleId: String): Flow<Sale?>
    suspend fun deleteSale(saleId: String): Result<Unit>
}
