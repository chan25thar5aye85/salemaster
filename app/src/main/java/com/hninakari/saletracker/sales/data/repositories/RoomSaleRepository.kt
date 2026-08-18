package com.hninakari.saletracker.sales.data.repositories

import android.content.Context
import com.hninakari.saletracker.sales.data.local.SaleEntity
import com.hninakari.saletracker.sales.data.local.SalesDatabase
import com.hninakari.saletracker.sales.data.models.Sales
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSaleRepository(context: Context) : SaleRepository {
    private val dao = SalesDatabase.getDatabase(context).saleDao()
    
    override suspend fun insertSale(sale: Sales) {
        dao.insertSale(SaleEntity.fromDomain(sale))
    }
    
    override suspend fun updateSale(sale: Sales) {
        dao.updateSale(SaleEntity.fromDomain(sale))
    }
    
    override suspend fun deleteSale(saleId: String) {
        val entity = dao.getSaleById(saleId)
        entity?.let { dao.deleteSale(it) }
    }
    
    override suspend fun getSaleById(saleId: String): Sales? {
        return dao.getSaleById(saleId)?.toDomain()
    }
    
    override fun getAllSales(): Flow<List<Sales>> {
        return dao.getAllSales().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getSalesByDateRange(startDate: String, endDate: String): Flow<List<Sales>> {
        return dao.getSalesByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getTotalSalesAmount(): Double {
        return dao.getTotalSalesAmount() ?: 0.0
    }
    
    override suspend fun getSalesCount(): Int {
        return dao.getSalesCount()
    }
}
