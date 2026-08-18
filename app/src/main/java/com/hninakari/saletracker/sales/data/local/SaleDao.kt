package com.hninakari.saletracker.sales.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert
    suspend fun insertSale(sale: SaleEntity)
    
    @Update
    suspend fun updateSale(sale: SaleEntity)
    
    @Delete
    suspend fun deleteSale(sale: SaleEntity)
    
    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleById(saleId: String): SaleEntity?
    
    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun getAllSales(): Flow<List<SaleEntity>>
    
    @Query("SELECT * FROM sales WHERE saleDate BETWEEN :startDate AND :endDate ORDER BY saleDate DESC")
    fun getSalesByDateRange(startDate: String, endDate: String): Flow<List<SaleEntity>>
    
    @Query("SELECT SUM(amount) FROM sales")
    suspend fun getTotalSalesAmount(): Double?
    
    @Query("SELECT COUNT(*) FROM sales")
    suspend fun getSalesCount(): Int
    
    @Query("""
        SELECT paymentMethod, COUNT(*) as count 
        FROM sales 
        GROUP BY paymentMethod 
        ORDER BY count DESC
    """)
    suspend fun getPaymentMethodBreakdown(): List<PaymentMethodBreakdown>
    
    @Query("DELETE FROM sales")
    suspend fun deleteAllSales()
}

data class PaymentMethodBreakdown(
    val paymentMethod: String,
    val count: Int
)
