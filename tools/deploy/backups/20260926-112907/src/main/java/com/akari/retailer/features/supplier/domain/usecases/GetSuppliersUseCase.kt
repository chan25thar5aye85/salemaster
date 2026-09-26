package com.akari.retailer.features.supplier.domain.usecases

import com.akari.retailer.core.usecases.NoParamUseCase
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.Supplier
import kotlinx.coroutines.flow.Flow

class GetSuppliersUseCase(
    private val repository: SupplierRepository
) : NoParamUseCase<Flow<List<Supplier>>> {
    
    override suspend fun invoke(): Flow<List<Supplier>> {
        return repository.getSuppliers()
    }
}
