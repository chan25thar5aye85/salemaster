package com.akari.retailer.features.supplier.data.repository

import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierService
import com.akari.retailer.features.supplier.domain.models.Supplier
import kotlinx.coroutines.flow.Flow

class FirestoreSupplierRepository(
    private val service: FirestoreSupplierService
) : SupplierRepository {
    
    override suspend fun addSupplier(supplier: Supplier): Result<String> {
        return service.addSupplier(supplier)
    }
    
    override suspend fun updateSupplier(supplier: Supplier): Result<Unit> {
        return service.updateSupplier(supplier)
    }
    
    override suspend fun deleteSupplier(supplierId: String): Result<Unit> {
        return service.deleteSupplier(supplierId)
    }
    
    override fun getSuppliers(): Flow<List<Supplier>> {
        return service.getSuppliers()
    }
    
    override fun getSupplierById(supplierId: String): Flow<Supplier?> {
        return service.getSupplierById(supplierId)
    }
    
    override suspend fun searchSuppliers(query: String): Result<List<Supplier>> {
        return service.searchSuppliers(query)
    }
}
