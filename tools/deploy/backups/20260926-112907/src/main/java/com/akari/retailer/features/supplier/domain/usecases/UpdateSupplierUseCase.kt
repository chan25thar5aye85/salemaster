package com.akari.retailer.features.supplier.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.Supplier

class UpdateSupplierUseCase(
    private val repository: SupplierRepository
) : UseCase<Supplier, Result<Unit>> {
    
    override suspend fun invoke(params: Supplier): Result<Unit> {
        return try {
            if (params.name.isBlank()) {
                Result.failure(Exception("Supplier name is required"))
            } else if (params.id.isEmpty()) {
                Result.failure(Exception("Supplier ID cannot be empty"))
            } else {
                repository.updateSupplier(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
