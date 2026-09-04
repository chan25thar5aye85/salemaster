package com.akari.retailer.features.supplier.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.Supplier

class AddSupplierUseCase(
    private val repository: SupplierRepository
) : UseCase<Supplier, Result<String>> {
    
    override suspend fun invoke(params: Supplier): Result<String> {
        return try {
            if (params.name.isBlank()) {
                Result.failure(Exception("Supplier name is required"))
            } else {
                repository.addSupplier(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
