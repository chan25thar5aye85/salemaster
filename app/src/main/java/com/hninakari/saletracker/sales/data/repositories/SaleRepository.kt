package com.hninakari.saletracker.sales.data.repositories

import com.hninakari.saletracker.sales.data.models.Sales
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
    suspend fun insertSale(sale: Sales)
    suspend fun updateSale(sale: Sales)
    suspend fun deleteSale(saleId: String)
    suspend fun getSaleById(saleId: String): Sales?
    fun getAllSales(): Flow<List<Sales>>
    fun getSalesByDateRange(startDate: String, endDate: String): Flow<List<Sales>>
    suspend fun getTotalSalesAmount(): Double
    suspend fun getSalesCount(): Int
}
