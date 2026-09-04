package com.akari.retailer.features.supplier.data.repository

import com.akari.retailer.features.supplier.domain.models.Supplier
import kotlinx.coroutines.flow.Flow

interface SupplierRepository {
    suspend fun addSupplier(supplier: Supplier): Result<String>
    suspend fun updateSupplier(supplier: Supplier): Result<Unit>
    suspend fun deleteSupplier(supplierId: String): Result<Unit>
    fun getSuppliers(): Flow<List<Supplier>>
    fun getSupplierById(supplierId: String): Flow<Supplier?>
    suspend fun searchSuppliers(query: String): Result<List<Supplier>>
}
